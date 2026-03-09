package com.rappidrive.application.usecases.trip;

import com.rappidrive.application.exceptions.TripAlreadyAcceptedException;
import com.rappidrive.application.ports.input.trip.AssignDriverToTripInputPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.application.ports.output.TripRepositoryPort;
import com.rappidrive.application.ports.output.TelemetryPort;
import com.rappidrive.application.ports.output.DriverAssignmentMetricsPort;
import com.rappidrive.application.metrics.DriverAssignmentStage;
import com.rappidrive.application.metrics.DriverAssignmentAttemptStatus;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.entities.Trip;
import com.rappidrive.domain.events.DomainEventPublisher;
import com.rappidrive.domain.events.TripDriverAssignedEvent;
import com.rappidrive.domain.valueobjects.*;
import com.rappidrive.domain.valueobjects.DriverLicense;
import com.rappidrive.domain.exceptions.TripConcurrencyException;
import com.rappidrive.domain.exceptions.DriverNotFoundException;
import com.rappidrive.domain.exceptions.TripNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssignDriverToTripUseCaseTest {

    private TripRepositoryPort tripRepository;
    private DriverRepositoryPort driverRepository;
    private DomainEventPublisher eventPublisher;
    private TelemetryPort telemetryPort;
    private DriverAssignmentMetricsPort metricsPort;
    private AssignDriverToTripUseCase useCase;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepositoryPort.class);
        driverRepository = mock(DriverRepositoryPort.class);
        eventPublisher = mock(DomainEventPublisher.class);
        
        // No-op telemetry port for unit tests - just pass through the execution
        telemetryPort = new TelemetryPort() {
            @Override
            public <T> T traceUseCase(String useCaseName, Map<String, String> attributes, Supplier<T> supplier) {
                return supplier.get();
            }
        };
        
        // No-op metrics port for unit tests - ignore all metric calls
        metricsPort = new DriverAssignmentMetricsPort() {
            @Override
            public void recordStageDuration(DriverAssignmentStage stage, long durationMillis) {}
            
            @Override
            public void incrementAttempts(DriverAssignmentStage stage, DriverAssignmentAttemptStatus status) {}
            
            @Override
            public void incrementQueue(String tenantId) {}
            
            @Override
            public void decrementQueue(String tenantId) {}
        };
        
        useCase = new AssignDriverToTripUseCase(tripRepository, driverRepository, eventPublisher, telemetryPort, metricsPort);
    }

    private Driver buildAvailableDriver(UUID id) {
        DriverLicense license = new DriverLicense("12345678901", "B", LocalDate.now().minusYears(5), LocalDate.now().plusYears(5), true);
        Driver driver = new Driver(id, TenantId.generate(), "Test Driver", new Email("driver@example.com"), new CPF("12345678909"), new Phone("+5511999999999"), license);
        // Make the driver active and set a current location so isAvailableForRide() returns true
        driver.activate();
        driver.updateLocation(new com.rappidrive.domain.valueobjects.Location(-23.55, -46.63));
        return driver;
    }

    private Trip buildPendingTrip(TripId id, PassengerId passengerId, Location origin, Location destination) {
        return new Trip(id, TenantId.generate(), passengerId, origin, destination);
    }

    @Test
    void execute_success_publishesEventAndSaves() {
        TripId tripId = TripId.generate();
        PassengerId passengerId = PassengerId.generate();
        DriverId driverId = new DriverId(UUID.randomUUID());
        Location origin = new Location(-23.55, -46.63);
        Location destination = new Location(-23.56, -46.65);

        Trip trip = buildPendingTrip(tripId, passengerId, origin, destination);
        Driver driver = buildAvailableDriver(driverId.getValue());

        when(tripRepository.findById(tripId.getValue())).thenReturn(Optional.of(trip));
        when(driverRepository.findById(driverId.getValue())).thenReturn(Optional.of(driver));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignDriverToTripInputPort.AssignDriverCommand cmd = new AssignDriverToTripInputPort.AssignDriverCommand(tripId.getValue(), driverId.getValue());

        Trip result = useCase.execute(cmd);

        assertNotNull(result);
        verify(driverRepository).save(driver);

        ArgumentCaptor<TripDriverAssignedEvent> captor = ArgumentCaptor.forClass(TripDriverAssignedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        TripDriverAssignedEvent ev = captor.getValue();
        assertEquals(tripId, ev.tripId());
        assertEquals(driverId, ev.driverId());
    }

    @Test
    void execute_conflict_throwsFriendlyException() {
        TripId tripId = TripId.generate();
        PassengerId passengerId = PassengerId.generate();
        DriverId driverId = new DriverId(UUID.randomUUID());
        Location origin = new Location(-23.55, -46.63);
        Location destination = new Location(-23.56, -46.65);

        Trip trip = buildPendingTrip(tripId, passengerId, origin, destination);
        Driver driver = buildAvailableDriver(driverId.getValue());

        when(tripRepository.findById(tripId.getValue())).thenReturn(Optional.of(trip));
        when(driverRepository.findById(driverId.getValue())).thenReturn(Optional.of(driver));
        when(tripRepository.save(any(Trip.class))).thenThrow(new TripConcurrencyException("conflict"));

        AssignDriverToTripInputPort.AssignDriverCommand cmd = new AssignDriverToTripInputPort.AssignDriverCommand(tripId.getValue(), driverId.getValue());

        TripAlreadyAcceptedException ex = assertThrows(TripAlreadyAcceptedException.class, () -> useCase.execute(cmd));
        assertTrue(ex.getMessage().contains("Esta corrida já foi aceita"));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void execute_validation_driverNotAvailable() {
        TripId tripId = TripId.generate();
        PassengerId passengerId = PassengerId.generate();
        DriverId driverId = new DriverId(UUID.randomUUID());
        Location origin = new Location(-23.55, -46.63);
        Location destination = new Location(-23.56, -46.65);

        Trip trip = buildPendingTrip(tripId, passengerId, origin, destination);
        // Build driver with expired license -> not available
        DriverLicense expired = new DriverLicense("12345678901", "B", LocalDate.now().minusYears(10), LocalDate.now().minusYears(1), true);
        Driver driver = new Driver(driverId.getValue(), TenantId.generate(), "Driver", new Email("drv@example.com"), new CPF("12345678909"), new Phone("+5511999999999"), expired);

        when(tripRepository.findById(tripId.getValue())).thenReturn(Optional.of(trip));
        when(driverRepository.findById(driverId.getValue())).thenReturn(Optional.of(driver));

        AssignDriverToTripInputPort.AssignDriverCommand cmd = new AssignDriverToTripInputPort.AssignDriverCommand(tripId.getValue(), driverId.getValue());

        assertThrows(IllegalStateException.class, () -> useCase.execute(cmd));
    }

    @Test
    void execute_validation_tripNotPending() {
        TripId tripId = TripId.generate();
        PassengerId passengerId = PassengerId.generate();
        DriverId driverId = new DriverId(UUID.randomUUID());
        Location origin = new Location(-23.55, -46.63);
        Location destination = new Location(-23.56, -46.65);

        Trip trip = buildPendingTrip(tripId, passengerId, origin, destination);
        trip.assignDriver(driverId); // now not pending

        when(tripRepository.findById(tripId.getValue())).thenReturn(Optional.of(trip));

        AssignDriverToTripInputPort.AssignDriverCommand cmd = new AssignDriverToTripInputPort.AssignDriverCommand(tripId.getValue(), driverId.getValue());

        assertThrows(IllegalStateException.class, () -> useCase.execute(cmd));
    }
}
