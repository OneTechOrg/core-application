package com.rappidrive.application.ports.output;

public interface PhoneTokenPort {
    String issue(String phone);
    String validateAndExtractPhone(String token);
}
