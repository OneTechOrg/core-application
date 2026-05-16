# SMS OTP Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a secure SMS-based onboarding flow (send, verify, register) for Passengers using Twilio, JWT phoneTokens, and Keycloak.

**Architecture:** Hexagonal Architecture (Ports & Adapters). Pure domain logic, orchestration via Use Cases, and infrastructure isolation via Adapters.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Twilio Verify SDK, Nimbus JOSE (JWT), Caffeine Cache.

---

## Task 1: Domain - Phone Value Object

**Files:**
- Create: `src/main/java/com/rappidrive/domain/valueobjects/Phone.java`
- Test: `src/test/java/com/rappidrive/domain/valueobjects/PhoneTest.java`

- [ ] **Step 1: Write the failing test for Phone validation.**
```java
package com.rappidrive.domain.valueobjects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhoneTest {
    @Test
    void shouldAcceptValidE164() {
        assertDoesNotThrow(() -> new Phone("+5511999999999"));
    }
    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Phone("11999999999"));
    }
}
```

- [ ] **Step 2: Implement Phone record with regex validation.**
```java
package com.rappidrive.domain.valueobjects;
import java.util.regex.Pattern;

public record Phone(String value) {
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    public Phone {
        if (value == null || !E164_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone format. Must be E.164 (e.g., +5511999999999)");
        }
    }
}
```

- [ ] **Step 3: Run tests and verify PASS.**

---

## Task 2: Application - Output Ports

**Files:**
- Create: `src/main/java/com/rappidrive/application/ports/output/SmsOtpPort.java`
- Create: `src/main/java/com/rappidrive/application/ports/output/PhoneTokenPort.java`
- Create: `src/main/java/com/rappidrive/application/ports/output/OtpRateLimiterPort.java`
- Modify: `src/main/java/com/rappidrive/application/ports/output/IdentityProvisioningPort.java`

- [ ] **Step 1: Define SmsOtpPort.**
```java
package com.rappidrive.application.ports.output;

public interface SmsOtpPort {
    void sendOtp(String phone);
    OtpVerificationResult verifyOtp(String phone, String code);
    enum OtpVerificationResult { APPROVED, INVALID, MAX_ATTEMPTS_REACHED }
}
```

- [ ] **Step 2: Define PhoneTokenPort.**
```java
package com.rappidrive.application.ports.output;

public interface PhoneTokenPort {
    String issue(String phone);
    String validateAndExtractPhone(String token);
}
```

- [ ] **Step 3: Define OtpRateLimiterPort.**
```java
package com.rappidrive.application.ports.output;

public interface OtpRateLimiterPort {
    void checkAndRecordOtpSend(String phone);
    void checkRegistrationLimit(String clientIp);
}
```

- [ ] **Step 4: Add createMobileUser to IdentityProvisioningPort.**
```java
// Add to IdentityProvisioningPort.java
String createMobileUser(String email, String phone, String fullName, 
                        String password, String role, com.rappidrive.domain.valueobjects.TenantId tenantId);
```

---

## Task 3: Application - Input Ports & Exceptions

**Files:**
- Create: `src/main/java/com/rappidrive/application/ports/input/auth/SendOtpInputPort.java`
- Create: `src/main/java/com/rappidrive/application/ports/input/auth/VerifyOtpInputPort.java`
- Create: `src/main/java/com/rappidrive/application/ports/input/auth/RegisterUserInputPort.java`
- Create: `src/main/java/com/rappidrive/application/exceptions/OtpRateLimitExceededException.java`
- Create: `src/main/java/com/rappidrive/application/exceptions/InvalidPhoneTokenException.java`
- Create: `src/main/java/com/rappidrive/application/exceptions/OtpVerificationFailedException.java`

- [ ] **Step 1: Define Input Ports.**
- [ ] **Step 2: Define Application Exceptions with appropriate fields (e.g., retryAfterSeconds).**

---

## Task 4: Application - Use Cases Implementation

**Files:**
- Create: `src/main/java/com/rappidrive/application/usecases/auth/SendOtpUseCase.java`
- Create: `src/main/java/com/rappidrive/application/usecases/auth/VerifyOtpUseCase.java`
- Create: `src/main/java/com/rappidrive/application/usecases/auth/RegisterUserUseCase.java`

- [ ] **Step 1: Implement SendOtpUseCase (Validate Phone -> Check Rate Limit -> Send).**
- [ ] **Step 2: Implement VerifyOtpUseCase (Verify via Port -> Issue Token).**
- [ ] **Step 3: Implement RegisterUserUseCase (Validate Token -> Identity Provisioning -> Create Profile).**
- [ ] **Step 4: Write unit tests for all Use Cases (mocking ports).**

---

## Task 5: Infrastructure - Twilio & JWT Adapters

**Files:**
- Create: `src/main/java/com/rappidrive/infrastructure/adapters/twilio/TwilioVerifyAdapter.java`
- Create: `src/main/java/com/rappidrive/infrastructure/adapters/auth/NimbusPhoneTokenAdapter.java`
- Create: `src/main/java/com/rappidrive/infrastructure/config/TwilioConfig.java`

- [ ] **Step 1: Implement TwilioVerifyAdapter using Twilio SDK.**
- [ ] **Step 2: Implement NimbusPhoneTokenAdapter using Nimbus JOSE library.**
- [ ] **Step 3: Configure Twilio beans in TwilioConfig.**

---

## Task 6: Infrastructure - Rate Limiter & Keycloak

**Files:**
- Create: `src/main/java/com/rappidrive/infrastructure/adapters/auth/CaffeineOtpRateLimiterAdapter.java`
- Modify: `src/main/java/com/rappidrive/infrastructure/adapters/keycloak/KeycloakProvisioningAdapter.java`

- [ ] **Step 1: Implement Caffeine-based rate limiting.**
- [ ] **Step 2: Implement `createMobileUser` in Keycloak adapter (handling roles and attributes).**

---

## Task 7: Presentation - Controller & DTOs

**Files:**
- Create: `src/main/java/com/rappidrive/presentation/controllers/AuthController.java`
- Create: `src/main/java/com/rappidrive/presentation/dto/request/OtpSendRequest.java` (and others)
- Modify: `src/main/java/com/rappidrive/presentation/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Implement AuthController with the 3 endpoints.**
- [ ] **Step 2: Add mappings for the new application exceptions in the Global Handler.**

---

## Task 8: Security & Wiring

**Files:**
- Modify: `src/main/java/com/rappidrive/infrastructure/config/SecurityConfiguration.java`
- Modify: `src/main/java/com/rappidrive/infrastructure/config/UseCaseConfiguration.java`

- [ ] **Step 1: Permit public access to `/api/v1/auth/**` in SecurityConfiguration.**
- [ ] **Step 2: Declare Use Case beans in UseCaseConfiguration.**

---

## Task 9: Verification & Finalization

- [ ] **Step 1: Run all unit tests.**
- [ ] **Step 2: Implement and run E2E Integration Test (`AuthRegistrationIT`).**
- [ ] **Step 3: Final check of code style and architectural rules (ArchUnit).**
- [ ] **Step 4: COMMIT ALL CHANGES.**
```bash
git add .
git commit -m "feat(auth): implement SMS OTP onboarding flow and passenger registration"
```
