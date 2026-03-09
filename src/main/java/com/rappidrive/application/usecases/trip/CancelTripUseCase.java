package com.rappidrive.application.usecases.trip;

import com.rappidrive.application.exceptions.TripUnauthorizedException;
import com.rappidrive.application.ports.input.trip.CancelTripInputPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.application.ports.output.PaymentGatewayPort;
import com.rappidrive.application.ports.output.TripRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.entities.Trip;
import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.exceptions.TripNotFoundException;
import com.rappidrive.domain.exceptions.DriverNotFoundException;
import com.rappidrive.domain.services.CancellationPolicyService;
import com.rappidrive.domain.valueobjects.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class CancelTripUseCase implements CancelTripInputPort {

    private final TripRepositoryPort tripRepository;
    private final DriverRepositoryPort driverRepository;
    private final CancellationPolicyService policyService;
    private final PaymentGatewayPort paymentGateway;

    public CancelTripUseCase(TripRepositoryPort tripRepository,
                            DriverRepositoryPort driverRepository,
                            CancellationPolicyService policyService,
                            PaymentGatewayPort paymentGateway) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.policyService = policyService;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional
    public CancellationResult execute(CancelCommand command) {
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new TripNotFoundException("Trip not found: " + command.tripId()));

        validateUserAuthorization(trip, command.userId(), command.actorType());

        LocalDateTime cancelledAt = LocalDateTime.now();
        CancellationFee fee = policyService.calculateFee(trip, command.actorType(), cancelledAt);

        trip.cancel(command.actorType(), command.reason(), cancelledAt);

        Money feeCharged = Money.zero(Currency.BRL);
        if (!fee.isFree() && command.actorType() == ActorType.PASSENGER) {
            try {
                PaymentGatewayPort.PaymentGatewayRequest paymentRequest = new PaymentGatewayPort.PaymentGatewayRequest(
                    fee.amount(),
                    PaymentMethod.cash(),
                    "Cancellation fee: " + fee.reason()
                );
                PaymentGatewayPort.PaymentGatewayResponse response = paymentGateway.processPayment(paymentRequest);
                if (response.success()) {
                    feeCharged = fee.amount();
                }
            } catch (Exception e) {
                feeCharged = Money.zero(Currency.BRL);
            }
        }

        reactivateDriverIfAssigned(trip);
        tripRepository.save(trip);

        return new CancellationResult(
            trip.getId(),
            true,
            command.actorType(),
            command.reason(),
            feeCharged,
            false,
            cancelledAt,
            "Trip cancelled successfully. " + (feeCharged.isZero() ? "No fee charged." : "Fee charged: " + feeCharged)
        );
    }

    private void validateUserAuthorization(Trip trip, UUID userId, ActorType actorType) {
        if (actorType == ActorType.PASSENGER) {
            if (!trip.getPassengerId().getValue().equals(userId)) {
                throw new TripUnauthorizedException("Passenger can only cancel their own trips");
            }
        } else if (actorType == ActorType.DRIVER) {
            UUID driverId = trip.getDriverId().orElse(null);
            if (driverId == null || !driverId.equals(userId)) {
                throw new TripUnauthorizedException("Driver can only cancel trips assigned to them");
            }
        }
    }

    private void reactivateDriverIfAssigned(Trip trip) {
        UUID driverId = trip.getDriverId().orElse(null);
        if (driverId == null) {
            return;
        }

        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new DriverNotFoundException(driverId));
        if (driver.getStatus() == DriverStatus.BUSY) {
            driver.activate();
            driverRepository.save(driver);
        }
    }
}
