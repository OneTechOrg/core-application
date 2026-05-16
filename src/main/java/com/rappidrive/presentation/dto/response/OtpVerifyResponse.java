package com.rappidrive.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after verifying an OTP")
public record OtpVerifyResponse(
    @Schema(description = "Token to be used for registration", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String phoneToken,
    
    @Schema(description = "Seconds until the token expires", example = "3600")
    int expiresIn
) {}
