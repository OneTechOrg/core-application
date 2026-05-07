package com.rappidrive.presentation.dto.request;

import com.rappidrive.presentation.dto.common.DriverLicenseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request to create a new driver.
 * Uses Java Record for immutability and clean code.
 */
@Schema(description = "Request to create a new driver")
public record CreateDriverRequest(
    
    @Schema(description = "Tenant ID for multi-tenancy", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    @NotNull(message = "Tenant ID is required")
    UUID tenantId,
    
    @Schema(description = "Driver's full name", example = "João da Silva", required = true)
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    String fullName,
    
    @Schema(description = "Driver's email address", example = "joao.silva@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @Schema(description = "Driver's CPF (11 digits)", example = "12345678909", required = true)
    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "\\d{11}", message = "CPF must be 11 digits")
    String cpf,
    
    @Schema(description = "Driver's phone number", example = "+5511987654321", required = true)
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format (E.164)")
    String phone,
    
    @Schema(description = "Driver's license information", required = true)
    @NotNull(message = "Driver license is required")
    @Valid
    DriverLicenseDto driverLicense,

    @Schema(description = "URLs for driver documents (CNH, CRLV, etc.)", example = "[\"https://docs.com/cnh.jpg\"]", required = true)
    @NotEmpty(message = "At least one document URL is required")
    List<@NotBlank String> documentUrls
) {}
