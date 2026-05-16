package com.rappidrive.application.usecases.auth;

import com.rappidrive.application.exceptions.InvalidPhoneTokenException;
import com.rappidrive.application.exceptions.OtpRateLimitExceededException;
import com.rappidrive.application.ports.input.auth.RegisterUserInputPort;
import com.rappidrive.application.ports.output.IdentityProvisioningPort;
import com.rappidrive.application.ports.output.OtpRateLimiterPort;
import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.application.ports.output.PhoneTokenPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private PhoneTokenPort phoneTokenPort;

    @Mock
    private OtpRateLimiterPort rateLimiter;

    @Mock
    private IdentityProvisioningPort identityProvisioningPort;

    @Mock
    private PassengerRepositoryPort passengerRepositoryPort;

    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        registerUserUseCase = new RegisterUserUseCase(
                phoneTokenPort,
                rateLimiter,
                identityProvisioningPort,
                passengerRepositoryPort
        );
    }

    @Test
    void execute_ShouldRegisterUser_WhenValidRequest() {
        // Given
        String phone = "+5511999999999";
        String token = "valid-token";
        String email = "john@example.com";
        String userId = UUID.randomUUID().toString();
        TenantId tenantId = TenantId.generate();
        RegisterUserInputPort.Command command = new RegisterUserInputPort.Command(
                "John Doe",
                email,
                "password123",
                phone,
                token,
                "PASSENGER",
                "127.0.0.1",
                tenantId
        );

        when(phoneTokenPort.validateAndExtractPhone(token)).thenReturn(phone);
        when(identityProvisioningPort.createMobileUser(
                eq(email), eq(phone), anyString(), anyString(), eq("ROLE_PASSENGER"), eq(tenantId)
        )).thenReturn(userId);

        // When
        RegisterUserInputPort.RegisterUserResult result = registerUserUseCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(email, result.email());
        verify(rateLimiter).checkRegistrationLimit("127.0.0.1");
        verify(passengerRepositoryPort).save(any(Passenger.class));
    }

    @Test
    void execute_ShouldThrowException_WhenPhoneMismatch() {
        // Given
        String phone = "+5511999999999";
        String token = "valid-token";
        RegisterUserInputPort.Command command = new RegisterUserInputPort.Command(
                "John Doe", "john@example.com", "password123",
                phone, token, "PASSENGER", "127.0.0.1", TenantId.generate()
        );

        when(phoneTokenPort.validateAndExtractPhone(token)).thenReturn("+5511888888888");

        // When & Then
        assertThrows(InvalidPhoneTokenException.class, () -> registerUserUseCase.execute(command));
        verify(identityProvisioningPort, never()).createMobileUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_ShouldThrowException_WhenInvalidRole() {
        // Given
        String phone = "+5511999999999";
        String token = "valid-token";
        RegisterUserInputPort.Command command = new RegisterUserInputPort.Command(
                "John Doe", "john@example.com", "password123",
                phone, token, "DRIVER", "127.0.0.1", TenantId.generate()
        );

        when(phoneTokenPort.validateAndExtractPhone(token)).thenReturn(phone);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registerUserUseCase.execute(command)
        );
        assertEquals("Driver registration is not allowed via this use case", exception.getMessage());
        verify(identityProvisioningPort, never()).createMobileUser(any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_ShouldThrowException_WhenRateLimitExceeded() {
        // Given
        String phone = "+5511999999999";
        String token = "valid-token";
        String ip = "127.0.0.1";
        RegisterUserInputPort.Command command = new RegisterUserInputPort.Command(
                "John Doe", "john@example.com", "password123",
                phone, token, "PASSENGER", ip, TenantId.generate()
        );

        when(phoneTokenPort.validateAndExtractPhone(token)).thenReturn(phone);
        doThrow(new OtpRateLimitExceededException("Rate limit exceeded", 60))
                .when(rateLimiter).checkRegistrationLimit(ip);

        // When & Then
        assertThrows(OtpRateLimitExceededException.class, () -> registerUserUseCase.execute(command));
        verify(identityProvisioningPort, never()).createMobileUser(any(), any(), any(), any(), any(), any());
    }
}
