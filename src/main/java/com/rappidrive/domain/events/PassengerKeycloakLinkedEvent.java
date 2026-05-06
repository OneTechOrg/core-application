package com.rappidrive.domain.events;

import com.rappidrive.domain.valueobjects.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a Passenger profile is linked to a Keycloak identity.
 */
public record PassengerKeycloakLinkedEvent(
    String eventId,
    LocalDateTime occurredOn,
    UUID passengerId,
    String keycloakId,
    TenantId tenantId
) implements DomainEvent {
    
    public PassengerKeycloakLinkedEvent(UUID passengerId, String keycloakId, TenantId tenantId) {
        this(
            UUID.randomUUID().toString(),
            LocalDateTime.now(),
            passengerId,
            keycloakId,
            tenantId
        );
    }
}
