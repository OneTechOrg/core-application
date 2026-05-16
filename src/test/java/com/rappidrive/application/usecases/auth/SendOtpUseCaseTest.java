package com.rappidrive.application.usecases.auth;

import com.rappidrive.application.ports.input.auth.SendOtpInputPort;
import com.rappidrive.application.ports.output.OtpRateLimiterPort;
import com.rappidrive.application.ports.output.SmsOtpPort;
import com.rappidrive.application.exceptions.OtpRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendOtpUseCaseTest {

    @Mock
    private OtpRateLimiterPort rateLimiter;

    @Mock
    private SmsOtpPort smsOtpPort;

    private SendOtpUseCase sendOtpUseCase;

    @BeforeEach
    void setUp() {
        sendOtpUseCase = new SendOtpUseCase(rateLimiter, smsOtpPort);
    }

    @Test
    void execute_ShouldSendOtp_WhenValidRequest() {
        // Given
        String phone = "+5511999999999";
        SendOtpInputPort.Command command = new SendOtpInputPort.Command(phone, "127.0.0.1");

        // When
        SendOtpInputPort.SendOtpResult result = sendOtpUseCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals(600, result.expiresIn());
        verify(rateLimiter).checkAndRecordOtpSend(phone);
        verify(smsOtpPort).sendOtp(phone);
    }

    @Test
    void execute_ShouldThrowException_WhenRateLimitExceeded() {
        // Given
        String phone = "+5511999999999";
        SendOtpInputPort.Command command = new SendOtpInputPort.Command(phone, "127.0.0.1");
        doThrow(new OtpRateLimitExceededException("Rate limit exceeded", 60))
                .when(rateLimiter).checkAndRecordOtpSend(phone);

        // When & Then
        assertThrows(OtpRateLimitExceededException.class, () -> sendOtpUseCase.execute(command));
        verify(smsOtpPort, never()).sendOtp(any());
    }

    @Test
    void execute_ShouldThrowException_WhenInvalidPhoneFormat() {
        // Given
        String invalidPhone = "12345";
        SendOtpInputPort.Command command = new SendOtpInputPort.Command(invalidPhone, "127.0.0.1");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> sendOtpUseCase.execute(command));
        verify(rateLimiter, never()).checkAndRecordOtpSend(any());
        verify(smsOtpPort, never()).sendOtp(any());
    }
}
