package com.rappidrive.application.ports.input.auth;

public interface VerifyOtpInputPort {
    record Command(String phone, String code) {}
    VerifyOtpResult execute(Command command);
    record VerifyOtpResult(String phoneToken, int expiresIn) {}
}
