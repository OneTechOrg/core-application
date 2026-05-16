package com.rappidrive.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to verify an OTP")
public record OtpVerifyRequest(
    @Schema(description = "Phone number in E.164 format", example = "+1234567890", required = true)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
    String phone,

    @Schema(description = "6-digit verification code", example = "123456", required = true)
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    String code
) {}
