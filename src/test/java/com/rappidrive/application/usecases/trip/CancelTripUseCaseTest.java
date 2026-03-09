package com.rappidrive.application.usecases.trip;

import com.rappidrive.application.ports.input.trip.CancelTripInputPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.application.ports.output.PaymentGatewayPort;
import com.rappidrive.application.ports.output.TripRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.entities.Trip;
import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.services.CancellationPolicyService;
import com.rappidrive.domain.valueobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CancelTripUseCaseTest {

    private TripRepositoryPort tripRepository;
    private DriverRepositoryPort driverRepository;
    private PaymentGatewayPort paymentGateway;
    private CancelTripUseCase useCase;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepositoryPort.class);
        driverRepository = mock(DriverRepositoryPort.class);
        paymentGateway = mock(PaymentGatewayPort.class);
        useCase = new CancelTripUseCase(tripRepository, driverRepository, new CancellationPolicyService(), paymentGateway);
    }

    @Test
    void execute_whenTripHasBusyDriver_reactivatesDriver() {
        UUID passengerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Trip trip = buildAssignedTrip(passengerId, driverId);
        Driver driver = buildBusyDriver(driverId, trip.getTenantId());

        when(tripRepository.findById(trip.getId().getValue())).thenReturn(Optional.of(trip));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelTripInputPort.CancelCommand command = new CancelTripInputPort.CancelCommand(
            trip.getId().getValue(),
            passengerId,
            ActorType.PASSENGER,
            CancellationReason.PASSENGER_WAIT_TOO_LONG,
            "cancel"
        );

        CancelTripInputPort.CancellationResult result = useCase.execute(command);

        assertTrue(result.cancelled());
        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
        verify(driverRepository).save(driver);
        verify(tripRepository).save(trip);
    }

    @Test
    void execute_whenTripHasNoDriver_doesNotLookupDriver() {
        UUID passengerId = UUID.randomUUID();
        Trip trip = new Trip(
            TripId.generate(),
            TenantId.generate(),
            new PassengerId(passengerId),
            new Location(-23.55, -46.63),
            new Location(-23.56, -46.65)
        );

        when(tripRepository.findById(trip.getId().getValue())).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelTripInputPort.CancelCommand command = new CancelTripInputPort.CancelCommand(
            trip.getId().getValue(),
            passengerId,
            ActorType.PASSENGER,
            CancellationReason.PASSENGER_CHANGE_OF_PLANS,
            "cancel"
        );

        useCase.execute(command);

        verify(driverRepository, never()).findById(any(UUID.class));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    private Trip buildAssignedTrip(UUID passengerId, UUID driverId) {
        Trip trip = new Trip(
            TripId.generate(),
            TenantId.generate(),
            new PassengerId(passengerId),
            new Location(-23.55, -46.63),
            new Location(-23.56, -46.65)
        );
        trip.assignDriver(new DriverId(driverId));
        return trip;
    }

    private Driver buildBusyDriver(UUID driverId, TenantId tenantId) {
        DriverLicense license = new DriverLicense(
            "12345678901",
            "B",
            LocalDate.now().minusYears(5),
            LocalDate.now().plusYears(5),
            true
        );
        Driver driver = new Driver(
            driverId,
            tenantId,
            "Driver",
            new Email("driver.cancel@test.com"),
            new CPF("12345678909"),
            new Phone("+5511999999999"),
            license
        );
        driver.activate();
        driver.updateLocation(new Location(-23.55, -46.63));
        driver.markAsBusy();
        return driver;
    }
}

