package com.rappidrive.application.ports.input.auth;

public interface SendOtpInputPort {
    record Command(String phone, String clientIp) {}
    SendOtpResult execute(Command command);
    record SendOtpResult(int expiresIn) {}
}
