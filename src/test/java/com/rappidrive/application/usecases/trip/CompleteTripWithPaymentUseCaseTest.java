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
import com.rappidrive.domain.enums.PaymentStatus;
import com.rappidrive.domain.services.TripCompletionService;
import com.rappidrive.domain.valueobjects.Location;
import com.rappidrive.domain.valueobjects.PaymentMethod;
import com.rappidrive.domain.valueobjects.TripId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CompleteTripWithPaymentUseCaseTest {

    private TripRepositoryPort tripRepository;
    private FareRepositoryPort fareRepository;
    private PaymentRepositoryPort paymentRepository;
    private DistanceCalculationPort distanceCalculation;
    private CalculateFareInputPort calculateFare;
    private ProcessPaymentInputPort processPayment;
    private TripCompletionService completionService;
    private CompleteTripWithPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepositoryPort.class);
        fareRepository = mock(FareRepositoryPort.class);
        paymentRepository = mock(PaymentRepositoryPort.class);
        distanceCalculation = mock(DistanceCalculationPort.class);
        calculateFare = mock(CalculateFareInputPort.class);
        processPayment = mock(ProcessPaymentInputPort.class);
        completionService = mock(TripCompletionService.class);
        useCase = new CompleteTripWithPaymentUseCase(
            tripRepository,
            fareRepository,
            paymentRepository,
            distanceCalculation,
            calculateFare,
            processPayment,
            completionService
        );
    }

    @Test
    void execute_whenTripAlreadyCompleted_returnsExistingCompletionResult() {
        TripId tripId = TripId.generate();
        Trip trip = mock(Trip.class);
        Fare fare = mock(Fare.class);
        Payment payment = mock(Payment.class);

        when(trip.getId()).thenReturn(tripId);
        when(tripRepository.findById(tripId.getValue())).thenReturn(Optional.of(trip));
        when(fareRepository.existsByTripId(tripId.getValue())).thenReturn(true);
        when(fareRepository.findByTripId(tripId.getValue())).thenReturn(Optional.of(fare));
        when(paymentRepository.findByTripId(tripId.getValue())).thenReturn(Optional.of(payment));
        when(payment.getStatus()).thenReturn(PaymentStatus.COMPLETED);

        CompleteTripWithPaymentInputPort.TripCompletionResult result = useCase.execute(
            new CompleteTripWithPaymentInputPort.CompleteTripWithPaymentCommand(
                tripId.getValue(),
                new Location(-23.56, -46.65),
                PaymentMethod.cash()
            )
        );

        assertEquals(trip, result.trip());
        assertEquals(fare, result.fare());
        assertEquals(payment, result.payment());
        assertTrue(result.paymentSuccessful());
        assertNull(result.failureReason());
        verifyNoInteractions(calculateFare, processPayment);
    }

    @Test
    void execute_whenExistingPaymentFailed_returnsFailureReason() {
        TripId tripId = TripId.generate();
        Trip trip = mock(Trip.class);
        Fare fare = mock(Fare.class);
        Payment payment = mock(Payment.class);

        when(trip.getId()).thenReturn(tripId);
        when(tripRepository.findById(tripId.getValue())).thenReturn(Optional.of(trip));
        when(fareRepository.existsByTripId(tripId.getValue())).thenReturn(true);
        when(fareRepository.findByTripId(tripId.getValue())).thenReturn(Optional.of(fare));
        when(paymentRepository.findByTripId(tripId.getValue())).thenReturn(Optional.of(payment));
        when(payment.getStatus()).thenReturn(PaymentStatus.FAILED);
        when(payment.getFailureReason()).thenReturn("gateway timeout");

        CompleteTripWithPaymentInputPort.TripCompletionResult result = useCase.execute(
            new CompleteTripWithPaymentInputPort.CompleteTripWithPaymentCommand(
                tripId.getValue(),
                new Location(-23.56, -46.65),
                PaymentMethod.cash()
            )
        );

        assertEquals("gateway timeout", result.failureReason());
        verifyNoInteractions(calculateFare, processPayment);
    }
}

