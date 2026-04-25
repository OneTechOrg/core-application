package com.rappidrive.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ServiceAreaResponse(
    UUID id,
    UUID tenantId,
    String name,
    String geoJsonPolygon,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
