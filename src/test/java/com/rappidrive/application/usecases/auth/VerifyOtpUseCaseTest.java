package com.rappidrive.application.usecases.auth;

import com.rappidrive.application.exceptions.OtpVerificationFailedException;
import com.rappidrive.application.ports.input.auth.VerifyOtpInputPort;
import com.rappidrive.application.ports.output.PhoneTokenPort;
import com.rappidrive.application.ports.output.SmsOtpPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyOtpUseCaseTest {

    @Mock
    private SmsOtpPort smsOtpPort;

    @Mock
    private PhoneTokenPort phoneTokenPort;

    private VerifyOtpUseCase verifyOtpUseCase;

    @BeforeEach
    void setUp() {
        verifyOtpUseCase = new VerifyOtpUseCase(smsOtpPort, phoneTokenPort);
    }

    @Test
    void execute_ShouldReturnToken_WhenOtpApproved() {
        // Given
        String phone = "+5511999999999";
        String code = "123456";
        String expectedToken = "valid-token";
        VerifyOtpInputPort.Command command = new VerifyOtpInputPort.Command(phone, code);

        when(smsOtpPort.verifyOtp(phone, code)).thenReturn(SmsOtpPort.OtpVerificationResult.APPROVED);
        when(phoneTokenPort.issue(phone)).thenReturn(expectedToken);

        // When
        VerifyOtpInputPort.VerifyOtpResult result = verifyOtpUseCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals(expectedToken, result.phoneToken());
        assertEquals(300, result.expiresIn());
        verify(phoneTokenPort).issue(phone);
    }

    @Test
    void execute_ShouldThrowException_WhenOtpInvalid() {
        // Given
        String phone = "+5511999999999";
        String code = "wrong-code";
        VerifyOtpInputPort.Command command = new VerifyOtpInputPort.Command(phone, code);

        when(smsOtpPort.verifyOtp(phone, code)).thenReturn(SmsOtpPort.OtpVerificationResult.INVALID);

        // When & Then
        OtpVerificationFailedException exception = assertThrows(
                OtpVerificationFailedException.class,
                () -> verifyOtpUseCase.execute(command)
        );
        assertFalse(exception.isMaxAttemptsReached());
        verify(phoneTokenPort, never()).issue(any());
    }

    @Test
    void execute_ShouldThrowException_WhenMaxAttemptsReached() {
        // Given
        String phone = "+5511999999999";
        String code = "any-code";
        VerifyOtpInputPort.Command command = new VerifyOtpInputPort.Command(phone, code);

        when(smsOtpPort.verifyOtp(phone, code)).thenReturn(SmsOtpPort.OtpVerificationResult.MAX_ATTEMPTS_REACHED);

        // When & Then
        OtpVerificationFailedException exception = assertThrows(
                OtpVerificationFailedException.class,
                () -> verifyOtpUseCase.execute(command)
        );
        assertTrue(exception.isMaxAttemptsReached());
        verify(phoneTokenPort, never()).issue(any());
    }
}
