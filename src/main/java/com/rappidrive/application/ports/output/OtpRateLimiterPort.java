package com.rappidrive.application.ports.output;

public interface OtpRateLimiterPort {
    void checkAndRecordOtpSend(String phone);
    void checkRegistrationLimit(String clientIp);
}
