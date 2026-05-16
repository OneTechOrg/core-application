package com.rappidrive.application.usecases.auth;

import com.rappidrive.application.exceptions.OtpVerificationFailedException;
import com.rappidrive.application.ports.input.auth.VerifyOtpInputPort;
import com.rappidrive.application.ports.output.PhoneTokenPort;
import com.rappidrive.application.ports.output.SmsOtpPort;

public class VerifyOtpUseCase implements VerifyOtpInputPort {

    private final SmsOtpPort smsOtpPort;
    private final PhoneTokenPort phoneTokenPort;

    public VerifyOtpUseCase(SmsOtpPort smsOtpPort, PhoneTokenPort phoneTokenPort) {
        this.smsOtpPort = smsOtpPort;
        this.phoneTokenPort = phoneTokenPort;
    }

    @Override
    public VerifyOtpResult execute(Command command) {
        SmsOtpPort.OtpVerificationResult result = smsOtpPort.verifyOtp(command.phone(), command.code());
        
        return switch (result) {
            case APPROVED -> {
                String token = phoneTokenPort.issue(command.phone());
                yield new VerifyOtpResult(token, 300);
            }
            case INVALID -> throw new OtpVerificationFailedException("Invalid OTP code", false);
            case MAX_ATTEMPTS_REACHED -> throw new OtpVerificationFailedException("Max attempts reached for this phone", true);
        };
    }
}
