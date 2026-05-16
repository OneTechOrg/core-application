package com.rappidrive.application.usecases.auth;

import com.rappidrive.application.exceptions.InvalidPhoneTokenException;
import com.rappidrive.application.ports.input.auth.RegisterUserInputPort;
import com.rappidrive.application.ports.output.IdentityProvisioningPort;
import com.rappidrive.application.ports.output.OtpRateLimiterPort;
import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.application.ports.output.PhoneTokenPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Phone;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class RegisterUserUseCase implements RegisterUserInputPort {

    private final PhoneTokenPort phoneTokenPort;
    private final OtpRateLimiterPort rateLimiter;
    private final IdentityProvisioningPort identityProvisioningPort;
    private final PassengerRepositoryPort passengerRepositoryPort;

    public RegisterUserUseCase(
            PhoneTokenPort phoneTokenPort,
            OtpRateLimiterPort rateLimiter,
            IdentityProvisioningPort identityProvisioningPort,
            PassengerRepositoryPort passengerRepositoryPort) {
        this.phoneTokenPort = phoneTokenPort;
        this.rateLimiter = rateLimiter;
        this.identityProvisioningPort = identityProvisioningPort;
        this.passengerRepositoryPort = passengerRepositoryPort;
    }

    @Override
    @Transactional
    public RegisterUserResult execute(Command command) {
        String validatedPhone = phoneTokenPort.validateAndExtractPhone(command.phoneToken());
        if (!validatedPhone.equals(command.phone())) {
            throw new InvalidPhoneTokenException("Phone token does not match provided phone number");
        }

        rateLimiter.checkRegistrationLimit(command.clientIp());

        if ("DRIVER".equalsIgnoreCase(command.role())) {
            throw new IllegalArgumentException("Driver registration is not allowed via this use case");
        }

        // For now we only support PASSENGER
        String role = "ROLE_PASSENGER";

        // Create user in Identity Provider (Keycloak)
        String userId = identityProvisioningPort.createMobileUser(
                command.email(),
                command.phone(),
                command.fullName(),
                command.password(),
                role,
                command.tenantId()
        );

        // Save passenger profile in local database
        Passenger passenger = new Passenger(
                UUID.randomUUID(),
                userId,
                command.tenantId(),
                command.fullName(),
                new Email(command.email()),
                new Phone(command.phone())
        );
        passengerRepositoryPort.save(passenger);

        return new RegisterUserResult(userId, command.email());
    }
}
