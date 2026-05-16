package com.rappidrive.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after sending an OTP")
public record OtpSendResponse(
    @Schema(description = "Seconds until the OTP expires", example = "300")
    int expiresIn
) {}
