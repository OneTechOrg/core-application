package com.rappidrive.application.ports.input.auth;

import com.rappidrive.domain.valueobjects.TenantId;

public interface RegisterUserInputPort {
    record Command(
        String fullName, String email, String password,
        String phone, String phoneToken, String role,
        String clientIp, TenantId tenantId
    ) {}
    RegisterUserResult execute(Command command);
    record RegisterUserResult(String userId, String email) {}
}
