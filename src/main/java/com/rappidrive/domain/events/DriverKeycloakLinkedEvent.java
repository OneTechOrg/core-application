package com.rappidrive.domain.events;

import com.rappidrive.domain.valueobjects.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a Driver profile is linked to a Keycloak identity.
 */
public record DriverKeycloakLinkedEvent(
    String eventId,
    LocalDateTime occurredOn,
    UUID driverId,
    String keycloakId,
    TenantId tenantId
) implements DomainEvent {
    
    public DriverKeycloakLinkedEvent(UUID driverId, String keycloakId, TenantId tenantId) {
        this(
            UUID.randomUUID().toString(),
            LocalDateTime.now(),
            driverId,
            keycloakId,
            tenantId
        );
    }
}
