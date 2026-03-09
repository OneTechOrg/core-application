package com.rappidrive.application.usecases.trip;

import com.rappidrive.application.ports.input.CompleteTripWithPaymentInputPort;
import com.rappidrive.application.ports.input.payment.CalculateFareInputPort;
import com.rappidrive.application.ports.input.payment.ProcessPaymentInputPort;
import com.rappidrive.application.ports.output.DistanceCalculationPort;
import com.rappidrive.application.ports.output.FareRepositoryPort;
import com.rappidrive.application.ports.output.PaymentRepositoryPort;
import com.rappidrive.application.ports.output.TripRepositoryPort;
import com.rappidrive.domain.entities.Fare;
import com.rappidrive.domain.entities.Payment;
import com.rappidrive.domain.entities.Trip;
import com.rappidrive.domain.enums.VehicleType;
import com.rappidrive.domain.exceptions.TripNotFoundException;
import com.rappidrive.domain.services.TripCompletionService;

import java.time.LocalDateTime;

/**
 * Use case for completing a trip with automatic fare calculation and payment processing.
 * Orchestrates the entire completion workflow:
 * 1. Validates trip state
 * 2. Calculates actual distance and duration
 * 3. Calculates fare
 * 4. Processes payment
 * 5. Updates trip
 */
public class CompleteTripWithPaymentUseCase implements CompleteTripWithPaymentInputPort {
    
    private final TripRepositoryPort tripRepository;
    private final FareRepositoryPort fareRepository;
    private final PaymentRepositoryPort paymentRepository;
    private final DistanceCalculationPort distanceCalculation;
    private final CalculateFareInputPort calculateFare;
    private final ProcessPaymentInputPort processPayment;
    private final TripCompletionService completionService;
    
    public CompleteTripWithPaymentUseCase(
            TripRepositoryPort tripRepository,
            FareRepositoryPort fareRepository,
            PaymentRepositoryPort paymentRepository,
            DistanceCalculationPort distanceCalculation,
            CalculateFareInputPort calculateFare,
            ProcessPaymentInputPort processPayment,
            TripCompletionService completionService) {
        this.tripRepository = tripRepository;
        this.fareRepository = fareRepository;
        this.paymentRepository = paymentRepository;
        this.distanceCalculation = distanceCalculation;
        this.calculateFare = calculateFare;
        this.processPayment = processPayment;
        this.completionService = completionService;
    }
    
    @Override
    public TripCompletionResult execute(CompleteTripWithPaymentCommand command) {
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new TripNotFoundException(command.tripId()));
        
        completionService.validateCanComplete(trip, command.dropoffLocation());
        
        // Check if trip was already completed and return previous result (idempotency)
        if (fareRepository.existsByTripId(trip.getId().getValue())) {
            return handleExistingCompletion(trip);
        }
        
        double actualDistanceKm = distanceCalculation.calculateDistance(
            trip.getOrigin(),
            command.dropoffLocation()
        );
        
        actualDistanceKm = completionService.calculateActualDistance(
            trip.getOrigin(),
            command.dropoffLocation()
        );
        
        int actualDurationMinutes = completionService.calculateActualDuration(
            trip.getStartedAt().orElseThrow()
        );
        
        Fare fare = calculateFare.execute(
            new CalculateFareInputPort.CalculateFareCommand(
                trip.getId().getValue(),
                trip.getTenantId(),
                actualDistanceKm,
                actualDurationMinutes,
                VehicleType.SEDAN, // TODO: Get from driver's vehicle
                LocalDateTime.now()
            )
        );
        
        fare = fareRepository.save(fare);
        
        Payment payment;
        boolean paymentSuccessful;
        String failureReason = null;
        
        try {
            payment = processPayment.execute(
                new ProcessPaymentInputPort.ProcessPaymentCommand(
                    trip.getId().getValue(),
                    command.paymentMethod()
                )
            );
            
            paymentSuccessful = payment.getStatus().name().equals("COMPLETED");
            if (!paymentSuccessful && payment.getFailureReason() != null && !payment.getFailureReason().isBlank()) {
                failureReason = payment.getFailureReason();
            }
        } catch (Exception e) {
            // Payment failed - create failed payment record
            throw e; // For now, propagate exception
        }
        
        completionService.validateFareAndPayment(fare, payment);
        
        trip.completeWithPayment(fare, payment);
        
        trip = tripRepository.save(trip);
        
        return new TripCompletionResult(
            trip,
            fare,
            payment,
            paymentSuccessful,
            failureReason
        );
    }
    
    /**
     * Handles case where trip was already completed (idempotency).
     */
    private TripCompletionResult handleExistingCompletion(Trip trip) {
        Fare fare = fareRepository.findByTripId(trip.getId().getValue())
            .orElseThrow(() -> new IllegalStateException("Fare not found for completed trip"));
        Payment payment = paymentRepository.findByTripId(trip.getId().getValue())
            .orElseThrow(() -> new IllegalStateException("Payment not found for completed trip"));

        boolean paymentSuccessful = payment.getStatus().name().equals("COMPLETED");
        String failureReason = paymentSuccessful ? null : payment.getFailureReason();
        return new TripCompletionResult(trip, fare, payment, paymentSuccessful, failureReason);
    }
}
