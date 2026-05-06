package com.rappidrive.domain.entities;

import com.rappidrive.domain.enums.PassengerStatus;
import com.rappidrive.domain.exceptions.InvalidPassengerStateException;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Phone;
import com.rappidrive.domain.valueobjects.TenantId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Passenger entity representing a rider in the RappiDrive platform.
 * Aggregate root for passenger-related operations.
 */
public class Passenger {
    
    private final UUID id;
    private final String keycloakId;
    private final TenantId tenantId;
    private final String fullName;
    private final Email email;
    private final Phone phone;
    private PassengerStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Creates a new Passenger instance.
     * Passenger starts in ACTIVE status.
     * 
     * @param id unique identifier
     * @param keycloakId external identity identifier
     * @param tenantId tenant identifier for multi-tenancy
     * @param fullName passenger's full name
     * @param email passenger's email
     * @param phone passenger's phone
     * @throws IllegalArgumentException if any required field is null
     */
    public Passenger(UUID id, String keycloakId, TenantId tenantId, String fullName, 
                     Email email, Phone phone) {
        if (id == null) {
            throw new IllegalArgumentException("Passenger ID cannot be null");
        }
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new IllegalArgumentException("Keycloak ID cannot be null or empty");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        if (phone == null) {
            throw new IllegalArgumentException("Phone cannot be null");
        }
        
        this.id = id;
        this.keycloakId = keycloakId;
        this.tenantId = tenantId;
        this.fullName = fullName.trim();
        this.email = email;
        this.phone = phone;
        this.status = PassengerStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reconstruction constructor for infrastructure layer (persistence).
     * Used to rebuild passenger objects from the database with all fields.
     * 
     * @param id unique identifier
     * @param keycloakId external identity identifier
     * @param tenantId tenant identifier
     * @param fullName passenger's full name
     * @param email passenger's email
     * @param phone passenger's phone
     * @param status current passenger status
     */
    public Passenger(UUID id, String keycloakId, TenantId tenantId, String fullName, 
                     Email email, Phone phone, PassengerStatus status) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = LocalDateTime.now(); // Will be overwritten by persistence layer
        this.updatedAt = LocalDateTime.now(); // Will be overwritten by persistence layer
    }
    
    /**
     * Activates the passenger.
     * 
     * @throws InvalidPassengerStateException if passenger is already active or blocked
     */
    public void activate() {
        if (status == PassengerStatus.ACTIVE) {
            throw new InvalidPassengerStateException("Passenger is already active");
        }
        if (status == PassengerStatus.BLOCKED) {
            throw new InvalidPassengerStateException("Cannot activate blocked passenger");
        }
        
        this.status = PassengerStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Deactivates the passenger.
     * 
     * @throws InvalidPassengerStateException if passenger is blocked
     */
    public void deactivate() {
        if (status == PassengerStatus.BLOCKED) {
            throw new InvalidPassengerStateException("Cannot deactivate blocked passenger");
        }
        if (status == PassengerStatus.INACTIVE) {
            throw new InvalidPassengerStateException("Passenger is already inactive");
        }
        
        this.status = PassengerStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Blocks the passenger due to policy violations.
     */
    public void block() {
        if (status == PassengerStatus.BLOCKED) {
            throw new InvalidPassengerStateException("Passenger is already blocked");
        }
        
        this.status = PassengerStatus.BLOCKED;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Checks if passenger can request rides.
     * 
     * @return true if passenger is active
     */
    public boolean canRequestRide() {
        return status == PassengerStatus.ACTIVE;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public String getKeycloakId() {
        return keycloakId;
    }
    
    public TenantId getTenantId() {
        return tenantId;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public Email getEmail() {
        return email;
    }
    
    public Phone getPhone() {
        return phone;
    }
    
    public PassengerStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Passenger)) return false;
        Passenger passenger = (Passenger) o;
        return id.equals(passenger.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Passenger(id=%s, name=%s, status=%s, tenant=%s)",
            id, fullName, status, tenantId);
    }
}
