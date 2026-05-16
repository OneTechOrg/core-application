package com.rappidrive.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new user")
public record RegisterUserRequest(
    @Schema(description = "User's full name", example = "John Doe", required = true)
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    String fullName,

    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @Schema(description = "User's password", example = "Password123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @Schema(description = "User's phone number in E.164 format", example = "+1234567890", required = true)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
    String phone,

    @Schema(description = "Phone verification token obtained after OTP verification", required = true)
    @NotBlank(message = "Phone token is required")
    String phoneToken,

    @Schema(description = "User role", allowableValues = {"PASSENGER", "DRIVER"}, required = true)
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(PASSENGER|DRIVER)$", message = "Role must be either PASSENGER or DRIVER")
    String role
) {}
