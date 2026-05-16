package com.rappidrive.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after registering a new user")
public record RegisterUserResponse(
    @Schema(description = "The unique identifier of the registered user", example = "550e8400-e29b-41d4-a716-446655440000")
    String userId,
    
    @Schema(description = "The email address of the registered user", example = "john.doe@example.com")
    String email
) {}
