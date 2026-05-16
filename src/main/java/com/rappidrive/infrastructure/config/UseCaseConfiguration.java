package com.rappidrive.infrastructure.config;

import com.rappidrive.application.ports.input.auth.*;
import com.rappidrive.application.ports.input.driver.*;
import com.rappidrive.application.ports.input.notification.*;
import com.rappidrive.application.ports.input.passenger.*;
import com.rappidrive.application.ports.input.payment.*;
import com.rappidrive.application.ports.input.rating.*;
import com.rappidrive.application.ports.input.servicearea.*;
import com.rappidrive.application.ports.input.tenant.*;
import com.rappidrive.application.ports.input.trip.*;
import com.rappidrive.application.ports.input.vehicle.*;
import com.rappidrive.application.ports.input.ApproveDriverInputPort;
import com.rappidrive.application.ports.input.RejectDriverInputPort;
import com.rappidrive.application.ports.input.ListPendingApprovalsInputPort;
import com.rappidrive.application.ports.input.SubmitDriverApprovalInputPort;
import com.rappidrive.application.ports.input.CompleteTripWithPaymentInputPort;
import com.rappidrive.application.ports.input.GetTripWithPaymentDetailsInputPort;
import com.rappidrive.application.ports.output.*;
import com.rappidrive.application.usecases.auth.*;
import com.rappidrive.application.usecases.driver.*;
import com.rappidrive.application.usecases.notification.*;
import com.rappidrive.application.usecases.passenger.*;
import com.rappidrive.application.usecases.payment.*;
import com.rappidrive.application.usecases.rating.*;
import com.rappidrive.application.usecases.servicearea.*;
import com.rappidrive.application.usecases.tenant.*;
import com.rappidrive.application.usecases.trip.*;
import com.rappidrive.application.usecases.vehicle.*;
import com.rappidrive.application.usecases.approval.*;
import com.rappidrive.domain.services.FareCalculator;
import com.rappidrive.domain.services.StandardFareCalculator;
import com.rappidrive.domain.services.CancellationPolicyService;
import com.rappidrive.domain.events.DomainEventPublisher;
import com.rappidrive.domain.services.RatingValidationService;
import com.rappidrive.domain.services.TripCompletionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

/**
 * Configuration class for wiring use cases (application layer).
 * Use cases are plain Java classes, not Spring components.
 * Dependencies are injected via constructor.
 */
@Configuration
public class UseCaseConfiguration {
    
    // Auth Use Cases

    @Bean
    public SendOtpInputPort sendOtpUseCase(SmsOtpPort smsOtpPort, OtpRateLimiterPort rateLimiterPort) {
        return new SendOtpUseCase(rateLimiterPort, smsOtpPort);
    }

    @Bean
    public VerifyOtpInputPort verifyOtpUseCase(SmsOtpPort smsOtpPort, PhoneTokenPort phoneTokenPort) {
        return new VerifyOtpUseCase(smsOtpPort, phoneTokenPort);
    }

    @Bean
    public RegisterUserInputPort registerUserUseCase(
            PhoneTokenPort phoneTokenPort,
            OtpRateLimiterPort rateLimiterPort,
            IdentityProvisioningPort identityProvisioning,
            PassengerRepositoryPort passengerRepository) {
        return new RegisterUserUseCase(phoneTokenPort, rateLimiterPort, identityProvisioning, passengerRepository);
    }

    // Driver Use Cases
    
    @Bean
    public CreateDriverInputPort createDriverUseCase(DriverRepositoryPort driverRepository,
                                                     DriverApprovalRepositoryPort approvalRepository) {
        return new CreateDriverUseCase(driverRepository, approvalRepository);
    }
    
    @Bean
    public GetDriverInputPort getDriverUseCase(DriverRepositoryPort driverRepository) {
        return new GetDriverUseCase(driverRepository);
    }
    
    @Bean
    public GetCurrentDriverInputPort getCurrentDriverUseCase(DriverRepositoryPort driverRepository,
                                                             CurrentUserPort currentUserPort) {
        return new GetCurrentDriverUseCase(driverRepository, currentUserPort);
    }
    
    @Bean
    public ActivateDriverInputPort activateDriverUseCase(DriverRepositoryPort driverRepository) {
        return new ActivateDriverUseCase(driverRepository);
    }
    
    @Bean
    public UpdateDriverLocationInputPort updateDriverLocationUseCase(DriverRepositoryPort driverRepository) {
        return new UpdateDriverLocationUseCase(driverRepository);
    }
    
    @Bean
    public FindAvailableDriversInputPort findAvailableDriversUseCase(
            DriverGeoQueryPort driverGeoQueryPort,
            @Qualifier("virtualThreadExecutor") ExecutorService executor,
            TelemetryPort telemetryPort,
            DriverAssignmentMetricsPort driverAssignmentMetricsPort) {
        return new FindAvailableDriversUseCase(driverGeoQueryPort, executor, telemetryPort, driverAssignmentMetricsPort);
    }
    
    // Passenger Use Cases
    
    @Bean
    public CreatePassengerInputPort createPassengerUseCase(PassengerRepositoryPort passengerRepository) {
        return new CreatePassengerUseCase(passengerRepository);
    }
    
    @Bean
    public GetPassengerInputPort getPassengerUseCase(PassengerRepositoryPort passengerRepository) {
        return new GetPassengerUseCase(passengerRepository);
    }
    
    @Bean
    public GetCurrentPassengerInputPort getCurrentPassengerUseCase(PassengerRepositoryPort passengerRepository,
                                                                   CurrentUserPort currentUserPort) {
        return new GetCurrentPassengerUseCase(passengerRepository, currentUserPort);
    }
    
    // Trip Use Cases
    
    @Bean
    public CreateTripInputPort createTripUseCase(TripRepositoryPort tripRepository,
                                                  PassengerRepositoryPort passengerRepository,
                                                  TelemetryPort telemetryPort) {
        return new CreateTripUseCase(tripRepository, passengerRepository, telemetryPort);
    }
    
    @Bean
    public GetTripInputPort getTripUseCase(TripRepositoryPort tripRepository) {
        return new GetTripUseCase(tripRepository);
    }
    
    @Bean
    public AssignDriverToTripInputPort assignDriverToTripUseCase(TripRepositoryPort tripRepository,
                                                                  DriverRepositoryPort driverRepository,
                                                                  DomainEventPublisher eventPublisher,
                                                                  TelemetryPort telemetryPort,
                                                                  DriverAssignmentMetricsPort driverAssignmentMetricsPort) {
        return new AssignDriverToTripUseCase(tripRepository, driverRepository, eventPublisher, telemetryPort, driverAssignmentMetricsPort);
    }
    
    @Bean
    public StartTripInputPort startTripUseCase(TripRepositoryPort tripRepository) {
        return new StartTripUseCase(tripRepository);
    }
    
    @Bean
    public CompleteTripInputPort completeTripUseCase(TripRepositoryPort tripRepository,
                                                      DriverRepositoryPort driverRepository) {
        return new CompleteTripUseCase(tripRepository, driverRepository);
    }
    
    @Bean
    public TripCompletionService tripCompletionService() {
        return new TripCompletionService();
    }
    
    @Bean
    public CompleteTripWithPaymentInputPort completeTripWithPaymentUseCase(
            TripRepositoryPort tripRepository,
            FareRepositoryPort fareRepository,
            PaymentRepositoryPort paymentRepository,
            DistanceCalculationPort distanceCalculation,
            CalculateFareInputPort calculateFare,
            ProcessPaymentInputPort processPayment,
            TripCompletionService completionService) {
        return new CompleteTripWithPaymentUseCase(
            tripRepository,
            fareRepository,
            paymentRepository,
            distanceCalculation,
            calculateFare,
            processPayment,
            completionService
        );
    }
    
    @Bean
    public GetTripWithPaymentDetailsInputPort getTripWithPaymentDetailsUseCase(
            TripRepositoryPort tripRepository,
            FareRepositoryPort fareRepository,
            PaymentRepositoryPort paymentRepository) {
        return new GetTripWithPaymentDetailsUseCase(
            tripRepository,
            fareRepository,
            paymentRepository
        );
    }

    @Bean
    public CancelTripInputPort cancelTripUseCase(
            TripRepositoryPort tripRepository,
            DriverRepositoryPort driverRepository,
            PaymentGatewayPort paymentGateway) {
        CancellationPolicyService policyService = new CancellationPolicyService();
        return new CancelTripUseCase(tripRepository, driverRepository, policyService, paymentGateway);
    }
    
    // Vehicle Use Cases
    
    @Bean
    public CreateVehicleInputPort createVehicleUseCase(VehicleRepositoryPort vehicleRepository) {
        return new CreateVehicleUseCase(vehicleRepository);
    }
    
    @Bean
    public GetVehicleInputPort getVehicleUseCase(VehicleRepositoryPort vehicleRepository) {
        return new GetVehicleUseCase(vehicleRepository);
    }
    
    @Bean
    public UpdateVehicleInputPort updateVehicleUseCase(VehicleRepositoryPort vehicleRepository) {
        return new UpdateVehicleUseCase(vehicleRepository);
    }
    
    @Bean
    public AssignVehicleToDriverInputPort assignVehicleToDriverUseCase(VehicleRepositoryPort vehicleRepository) {
        return new AssignVehicleToDriverUseCase(vehicleRepository);
    }
    
    @Bean
    public ActivateVehicleInputPort activateVehicleUseCase(VehicleRepositoryPort vehicleRepository) {
        return new ActivateVehicleUseCase(vehicleRepository);
    }
    
    // Payment Use Cases
    
    @Bean
    public CalculateFareInputPort calculateFareUseCase(FareConfigurationRepositoryPort fareConfigRepository) {
        return new CalculateFareUseCase(fareConfigRepository);
    }
    
    @Bean
    public ProcessPaymentInputPort processPaymentUseCase(PaymentRepositoryPort paymentRepository,
                                                          PaymentGatewayPort paymentGateway,
                                                          FareConfigurationRepositoryPort fareConfigRepository,
                                                          TelemetryPort telemetryPort) {
        return new ProcessPaymentUseCase(paymentRepository, paymentGateway, fareConfigRepository, telemetryPort);
    }
    
    @Bean
    public GetPaymentInputPort getPaymentUseCase(PaymentRepositoryPort paymentRepository) {
        return new GetPaymentUseCase(paymentRepository);
    }
    
    @Bean
    public RefundPaymentInputPort refundPaymentUseCase(PaymentRepositoryPort paymentRepository,
                                                        PaymentGatewayPort paymentGateway) {
        return new RefundPaymentUseCase(paymentRepository, paymentGateway);
    }
    
    @Bean
    public GetFareConfigurationInputPort getFareConfigurationUseCase(FareConfigurationRepositoryPort fareConfigRepository) {
        return new GetFareConfigurationUseCase(fareConfigRepository);
    }
    
    @Bean
    public UpdateFareConfigurationInputPort updateFareConfigurationUseCase(FareConfigurationRepositoryPort fareConfigRepository) {
        return new UpdateFareConfigurationUseCase(fareConfigRepository);
    }
    
    // Rating Use Cases
    
    @Bean
    public RatingValidationService ratingValidationService() {
        return new RatingValidationService();
    }
    
    @Bean
    public CreateRatingInputPort createRatingUseCase(
            RatingRepositoryPort ratingRepository,
            TripRepositoryPort tripRepository,
            RatingValidationService validationService) {
        return new CreateRatingUseCase(ratingRepository, tripRepository, validationService);
    }
    
    @Bean
    public GetDriverRatingSummaryInputPort getDriverRatingSummaryUseCase(
            RatingRepositoryPort ratingRepository) {
        return new GetDriverRatingSummaryUseCase(ratingRepository);
    }
    
    @Bean
    public GetPassengerRatingInputPort getPassengerRatingUseCase(
            RatingRepositoryPort ratingRepository) {
        return new GetPassengerRatingUseCase(ratingRepository);
    }
    
    @Bean
    public GetTripRatingsInputPort getTripRatingsUseCase(
            RatingRepositoryPort ratingRepository) {
        return new GetTripRatingsUseCase(ratingRepository);
    }
    
    @Bean
    public ReportOffensiveRatingInputPort reportOffensiveRatingUseCase(
            RatingRepositoryPort ratingRepository) {
        return new ReportOffensiveRatingUseCase(ratingRepository);
    }
    
    // Notification Use Cases
    
    @Bean
    public SendNotificationInputPort sendNotificationUseCase(
            NotificationRepositoryPort notificationRepository,
            NotificationServicePort notificationService,
            TelemetryPort telemetryPort,
            DriverAssignmentMetricsPort driverAssignmentMetricsPort) {
        return new SendNotificationUseCase(notificationRepository, notificationService, telemetryPort, driverAssignmentMetricsPort);
    }
    
    @Bean
    public GetUserNotificationsInputPort getUserNotificationsUseCase(
            NotificationRepositoryPort notificationRepository) {
        return new GetUserNotificationsUseCase(notificationRepository);
    }
    
    @Bean
    public MarkNotificationAsReadInputPort markNotificationAsReadUseCase(
            NotificationRepositoryPort notificationRepository) {
        return new MarkNotificationAsReadUseCase(notificationRepository);
    }
    
    @Bean
    public GetUnreadCountInputPort getUnreadCountUseCase(
            NotificationRepositoryPort notificationRepository) {
        return new GetUnreadCountUseCase(notificationRepository);
    }
    
    // FareCalculator Bean
    @Bean
    public FareCalculator fareCalculator() {
        return new StandardFareCalculator();
    }

    // DomainEventPublisher Bean
    @Bean
    public DomainEventPublisher domainEventPublisher() {
        return DomainEventPublisher.instance();
    }

    // Approval Use Cases
    @Bean
    public SubmitDriverApprovalInputPort submitDriverApprovalUseCase(
            DriverApprovalRepositoryPort approvalRepository,
            DriverRepositoryPort driverRepository) {
        return new SubmitDriverApprovalUseCase(approvalRepository, driverRepository);
    }

    @Bean
    public ListPendingApprovalsInputPort listPendingApprovalsUseCase(
            DriverApprovalRepositoryPort approvalRepository,
            AdminUserRepositoryPort adminRepository,
            DriverRepositoryPort driverRepository) {
        return new ListPendingApprovalsUseCase(approvalRepository, adminRepository, driverRepository);
    }

    @Bean
    public ApproveDriverInputPort approveDriverUseCase(
            DriverApprovalRepositoryPort approvalRepository,
            AdminUserRepositoryPort adminRepository,
            DriverRepositoryPort driverRepository) {
        return new ApproveDriverUseCase(approvalRepository, adminRepository, driverRepository);
    }

    @Bean
    public RejectDriverInputPort rejectDriverUseCase(
            DriverApprovalRepositoryPort approvalRepository,
            AdminUserRepositoryPort adminRepository,
            DriverRepositoryPort driverRepository) {
        return new RejectDriverUseCase(approvalRepository, adminRepository, driverRepository);
    }

    // Service Area Use Cases

    @Bean
    public GetServiceAreaInputPort getServiceAreaUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        return new GetServiceAreaUseCase(serviceAreaRepository);
    }

    @Bean
    public ListServiceAreasInputPort listServiceAreasUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        return new ListServiceAreasUseCase(serviceAreaRepository);
    }

    @Bean
    public CreateServiceAreaInputPort createServiceAreaUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        return new CreateServiceAreaUseCase(serviceAreaRepository);
    }

    @Bean
    public ToggleServiceAreaStatusInputPort toggleServiceAreaStatusUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        return new ToggleServiceAreaStatusUseCase(serviceAreaRepository);
    }

    // Tenant Management Use Cases

    @Bean
    public OnboardNewTenantInputPort onboardNewTenantUseCase(
            ServiceAreaRepositoryPort serviceAreaRepository,
            FareConfigurationRepositoryPort fareConfigRepository,
            IdentityProvisioningPort identityProvisioning,
            com.rappidrive.application.ports.output.TenantRepositoryPort tenantRepository) {
        return new OnboardNewTenantUseCase(serviceAreaRepository, fareConfigRepository,
                identityProvisioning, tenantRepository);
    }
}
