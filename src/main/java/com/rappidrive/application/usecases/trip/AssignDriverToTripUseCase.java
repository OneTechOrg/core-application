package com.rappidrive.application.usecases.trip;

import com.rappidrive.application.metrics.DriverAssignmentAttemptStatus;
import com.rappidrive.application.metrics.DriverAssignmentStage;
import com.rappidrive.application.ports.input.trip.AssignDriverToTripInputPort;
import com.rappidrive.application.ports.output.DriverAssignmentMetricsPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.application.ports.output.TelemetryPort;
import com.rappidrive.application.ports.output.TripRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.entities.Trip;
import com.rappidrive.domain.exceptions.DriverNotFoundException;
import com.rappidrive.domain.exceptions.TripNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Use case for assigning a driver to a trip.
 */
public class AssignDriverToTripUseCase implements AssignDriverToTripInputPort {
    
    private final TripRepositoryPort tripRepository;
    private final DriverRepositoryPort driverRepository;
    private final com.rappidrive.domain.events.DomainEventPublisher eventPublisher;
    private final TelemetryPort telemetryPort;
    private final DriverAssignmentMetricsPort metricsPort;

    public AssignDriverToTripUseCase(TripRepositoryPort tripRepository,
                                     DriverRepositoryPort driverRepository,
                                     com.rappidrive.domain.events.DomainEventPublisher eventPublisher,
                                     TelemetryPort telemetryPort,
                                     DriverAssignmentMetricsPort metricsPort) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.eventPublisher = eventPublisher;
        this.telemetryPort = telemetryPort;
        this.metricsPort = metricsPort;
    }
    
    @Override
    @Transactional
    public Trip execute(AssignDriverCommand command) {
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new TripNotFoundException(command.tripId()));

        String tenantTag = tenantAsString(trip);
        Map<String, String> attributes = telemetryAttributes(command.tripId(), tenantTag);

        return telemetryPort.traceUseCase("driver.assignment", attributes,
            () -> executeWithMetrics(command, trip, tenantTag));
    }

    private Trip executeWithMetrics(AssignDriverCommand command, Trip trip, String tenantTag) {
        long start = System.nanoTime();
        metricsPort.incrementQueue(tenantTag);
        try {
            Trip result = performAssignment(command, trip);
            metricsPort.incrementAttempts(DriverAssignmentStage.ASSIGNMENT, DriverAssignmentAttemptStatus.SUCCESS);
            return result;
        } catch (RuntimeException ex) {
            metricsPort.incrementAttempts(DriverAssignmentStage.ASSIGNMENT, DriverAssignmentAttemptStatus.ERROR);
            throw ex;
        } finally {
            metricsPort.decrementQueue(tenantTag);
            metricsPort.recordStageDuration(DriverAssignmentStage.ASSIGNMENT, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }

    private Trip performAssignment(AssignDriverCommand command, Trip trip) {
        if (!trip.isPending()) {
            throw new IllegalStateException("Trip is not pending");
        }

        Driver driver = driverRepository.findById(command.driverId())
            .orElseThrow(() -> new DriverNotFoundException(command.driverId()));

        if (!driver.isAvailableForRide()) {
            throw new IllegalStateException("Driver is not available for rides");
        }

        try {
            trip.assignDriver(new com.rappidrive.domain.valueobjects.DriverId(command.driverId()));
            Trip saved = tripRepository.save(trip);
            driver.markAsBusy();
            driverRepository.save(driver);
            eventPublisher.publish(new com.rappidrive.domain.events.TripDriverAssignedEvent(saved.getId(), new com.rappidrive.domain.valueobjects.DriverId(command.driverId())));
            return saved;
        } catch (com.rappidrive.domain.exceptions.TripConcurrencyException e) {
            throw new com.rappidrive.application.exceptions.TripAlreadyAcceptedException("Esta corrida já foi aceita por outro motorista", e);
        }
    }

    private Map<String, String> telemetryAttributes(java.util.UUID tripId, String tenantTag) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("stage", "driver.assignment");
        attributes.put("tripId", tripId != null ? tripId.toString() : "unknown");
        attributes.put("tenantId", tenantTag);
        return attributes;
    }

    private String tenantAsString(Trip trip) {
        return trip.getTenantId() != null ? trip.getTenantId().asString() : "unknown";
    }
}
