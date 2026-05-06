package com.rappidrive.infrastructure.persistence.entities;

import com.rappidrive.domain.enums.PassengerStatus;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Phone;
import com.rappidrive.domain.valueobjects.TenantId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for Passenger.
 * This is part of the infrastructure layer and should NOT be exposed to domain or application layers.
 */
@Entity
@Table(name = "passengers")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Builder
public class PassengerJpaEntity {

    public PassengerJpaEntity() {}

    public PassengerJpaEntity(UUID id,
                              String keycloakId,
                              TenantId tenantId,
                              String fullName,
                              Email email,
                              Phone phone,
                              PassengerStatus status,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;
    
    @Column(name = "tenant_id", nullable = false)
    private TenantId tenantId;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(nullable = false, unique = true)
    private Email email;
    
    @Column(nullable = false, length = 20)
    private Phone phone;
    
    @Column(nullable = false, length = 20)
    private PassengerStatus status;
    
    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Explicit getters/setters for mappers
    public UUID getId() { return this.id; }
    public void setId(UUID id) { this.id = id; }

    public String getKeycloakId() { return this.keycloakId; }
    public void setKeycloakId(String k) { this.keycloakId = k; }

    public TenantId getTenantId() { return this.tenantId; }
    public void setTenantId(TenantId t) { this.tenantId = t; }

    public String getFullName() { return this.fullName; }
    public void setFullName(String n) { this.fullName = n; }

    public Email getEmail() { return this.email; }
    public void setEmail(Email e) { this.email = e; }

    public Phone getPhone() { return this.phone; }
    public void setPhone(Phone p) { this.phone = p; }

    public PassengerStatus getStatus() { return this.status; }
    public void setStatus(PassengerStatus s) { this.status = s; }
}
