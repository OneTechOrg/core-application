# SMS OTP Onboarding Design Specification

**Status:** Approved
**Date:** 2026-05-16
**Author:** Gemini CLI
**Topic:** SMS OTP Onboarding for Passengers

---

## 1. Overview
The goal is to implement a secure, SMS-based onboarding flow for mobile users (primarily Passengers). This replaces traditional password-only registration with a verified phone number flow.

## 2. Architecture (Hexagonal)

### 2.1. Packages & Components
- **Domain (`domain/`)**:
    - `Phone` (Value Object): E.164 validation.
- **Application (`application/`)**:
    - **Input Ports**: `SendOtpInputPort`, `VerifyOtpInputPort`, `RegisterUserInputPort`.
    - **Use Cases**: `SendOtpUseCase`, `VerifyOtpUseCase`, `RegisterUserUseCase`.
    - **Output Ports**: `SmsOtpPort` (Twilio), `PhoneTokenPort` (JWT), `OtpRateLimiterPort` (Caffeine), `IdentityProvisioningPort` (Keycloak), `PassengerRepositoryPort` (JPA).
- **Infrastructure (`infrastructure/`)**:
    - `TwilioVerifyAdapter`: SMS delivery and verification.
    - `NimbusPhoneTokenAdapter`: Internal JWT handling (HS256).
    - `CaffeineRateLimiterAdapter`: In-memory rate limiting.
    - `KeycloakProvisioningAdapter`: User creation in Identity Provider.
- **Presentation (`presentation/`)**:
    - `AuthController`: REST endpoints.
    - `GlobalExceptionHandler`: Mapping application exceptions to 4xx/5xx.

## 3. Data Flow

### 3.1. Send OTP
1. User provides phone number.
2. System checks Rate Limit (3 attempts / 10 min).
3. System triggers Twilio Verify SMS.

### 3.2. Verify OTP
1. User provides phone + 6-digit code.
2. System verifies via Twilio.
3. If approved, system generates a **phoneToken** (Stateless JWT).

### 3.3. Register
1. User provides `fullName`, `email`, `password`, `phone`, `phoneToken`, and `role`.
2. System validates `phoneToken` signature and expiration (5 min).
3. System ensures `phone` in body matches `sub` in `phoneToken`.
4. System creates user in Keycloak and profile in DB.
5. System enforces `role=PASSENGER` (Driver registration is out of scope).

## 4. Security & Resilience

### 4.1. phoneToken (Stateless JWT)
- **Algorithm**: HS256 (internal secret).
- **Claims**: `sub` (phone), `iat`, `exp` (300s).
- **Purpose**: Prevents registration with an unverified phone number.

### 4.2. Rate Limiting
- **OTP Send**: 3 sends per 10 minutes per phone.
- **Registration**: 10 registrations per hour per IP.

### 4.3. Privacy
- No phone numbers or PII in logs.
- `phoneToken` is short-lived and internal.

## 5. Testing Strategy

### 5.1. Unit Tests
- Focused on Use Case logic, ensuring ports are called correctly and exceptions are thrown for rate limits or invalid tokens.

### 5.2. Integration Tests (E2E)
- Use **AuthRegistrationIT** with Testcontainers.
- Mock `SmsOtpPort` to simulate successful OTP verification.
- Verify presence of user in Keycloak and database.

### 5.3. Architecture Tests
- Ensure `HexagonalArchitectureTest` passes with the new packages.

---

## 6. Out of Scope
- **Driver Registration**: CPF/CNH validation and document upload.
- **Password Recovery**: Handled by a separate flow.
- **Social Login**: To be integrated later.
