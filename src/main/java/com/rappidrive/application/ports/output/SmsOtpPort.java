package com.rappidrive.application.ports.output;

public interface SmsOtpPort {
    void sendOtp(String phone);
    OtpVerificationResult verifyOtp(String phone, String code);
    enum OtpVerificationResult { APPROVED, INVALID, MAX_ATTEMPTS_REACHED }
}
