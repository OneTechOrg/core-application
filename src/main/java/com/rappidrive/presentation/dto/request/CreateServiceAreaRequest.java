package com.rappidrive.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateServiceAreaRequest(
    @NotNull UUID tenantId,
    @NotBlank String name,
    @NotBlank String geoJsonPolygon
) {}
