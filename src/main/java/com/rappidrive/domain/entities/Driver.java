package com.rappidrive.domain.entities;

import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.exceptions.InvalidDriverStateException;
import com.rappidrive.domain.valueobjects.CPF;
import com.rappidrive.domain.valueobjects.DriverLicense;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Location;
import com.rappidrive.domain.valueobjects.Phone;
import com.rappidrive.domain.valueobjects.TenantId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Driver entity representing a driver in the RappiDrive platform.
 * Aggregate root for driver-related operations.
 */
public class Driver {
    
    private final UUID id;
    private final String keycloakId;
    private final TenantId tenantId;
    private final String fullName;
    private final Email email;
    private final CPF cpf;
    private final Phone phone;
    private final DriverLicense driverLicense;
    private DriverStatus status;
    private Location currentLocation;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Creates a new Driver instance.
     * Driver starts in PENDING_APPROVAL status.
     * 
     * @param id unique identifier
     * @param keycloakId external identity identifier
     * @param tenantId tenant identifier for multi-tenancy
     * @param fullName driver's full name
     * @param email driver's email
     * @param cpf driver's CPF
     * @param phone driver's phone
     * @param driverLicense driver's CNH
     * @throws IllegalArgumentException if any required field is null
     */
    public Driver(UUID id, String keycloakId, TenantId tenantId, String fullName, Email email, 
                  CPF cpf, Phone phone, DriverLicense driverLicense) {
        if (id == null) {
            throw new IllegalArgumentException("Driver ID cannot be null");
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
        if (cpf == null) {
            throw new IllegalArgumentException("CPF cannot be null");
        }
        if (phone == null) {
            throw new IllegalArgumentException("Phone cannot be null");
        }
        if (driverLicense == null) {
            throw new IllegalArgumentException("Driver license cannot be null");
        }
        
        this.id = id;
        this.keycloakId = keycloakId;
        this.tenantId = tenantId;
        this.fullName = fullName.trim();
        this.email = email;
        this.cpf = cpf;
        this.phone = phone;
        this.driverLicense = driverLicense;
        this.status = DriverStatus.PENDING_APPROVAL;
        this.currentLocation = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Reconstruction constructor for persistence layer.
     * Used by infrastructure adapters to recreate domain entities from storage.
     */
    public Driver(UUID id, String keycloakId, TenantId tenantId, String fullName, Email email,
                  CPF cpf, Phone phone, DriverLicense driverLicense,
                  DriverStatus status, Location currentLocation) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.email = email;
        this.cpf = cpf;
        this.phone = phone;
        this.driverLicense = driverLicense;
        this.status = status;
        this.currentLocation = currentLocation;
        this.createdAt = LocalDateTime.now(); // Will be overridden by JPA
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Activates the driver after approval.
     * Can also be used to mark busy driver as available again.
     * 
     * @throws InvalidDriverStateException if driver is already active, blocked, 
     *         has invalid license category (A - motorcycle only), or CNH is not definitive
     */
    public void activate() {
        if (status == DriverStatus.ACTIVE) {
            throw new InvalidDriverStateException("Driver is already active");
        }
        if (status == DriverStatus.BLOCKED) {
            throw new InvalidDriverStateException("Cannot activate blocked driver");
        }
        
        // Business Rule 1: CNH category A (motorcycle only) is not allowed
        if ("A".equals(driverLicense.getCategory())) {
            throw new InvalidDriverStateException(
                "Cannot activate driver with CNH category A (motorcycle only). Minimum required: B"
            );
        }
        
        // Business Rule 2: CNH must be definitive (not PPD/temporary permission)
        if (!driverLicense.isDefinitive()) {
            throw new InvalidDriverStateException(
                "Cannot activate driver with temporary CNH (PPD). Definitive license required"
            );
        }
        
        this.status = DriverStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Marks the driver as busy (currently on a trip).
     * 
     * @throws InvalidDriverStateException if driver is not active
     */
    public void markAsBusy() {
        if (status != DriverStatus.ACTIVE) {
            throw new InvalidDriverStateException("Only active drivers can be marked as busy");
        }
        
        this.status = DriverStatus.BUSY;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Deactivates the driver.
     * 
     * @throws InvalidDriverStateException if driver is blocked or already inactive
     */
    public void deactivate() {
        if (status == DriverStatus.BLOCKED) {
            throw new InvalidDriverStateException("Cannot deactivate blocked driver");
        }
        if (status == DriverStatus.INACTIVE) {
            throw new InvalidDriverStateException("Driver is already inactive");
        }
        
        this.status = DriverStatus.INACTIVE;
        this.currentLocation = null; // Clear location when going offline
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Blocks the driver due to policy violations.
     */
    public void block() {
        if (status == DriverStatus.BLOCKED) {
            throw new InvalidDriverStateException("Driver is already blocked");
        }
        
        this.status = DriverStatus.BLOCKED;
        this.currentLocation = null;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Updates driver's current location.
     * Only allowed for active or busy drivers.
     * 
     * @param location new location
     * @throws InvalidDriverStateException if driver is not active or busy
     * @throws IllegalArgumentException if location is null
     */
    public void updateLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (status != DriverStatus.ACTIVE && status != DriverStatus.BUSY) {
            throw new InvalidDriverStateException(
                "Only active or busy drivers can update location"
            );
        }
        
        this.currentLocation = location;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Checks if driver is available to accept rides.
     * Driver must be active, have location set, and have valid (non-expired) license.
     * 
     * @return true if driver is active, has location, and license is valid
     */
    public boolean isAvailableForRide() {
        return status == DriverStatus.ACTIVE 
            && currentLocation != null 
            && driverLicense.isValid();
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
    
    public CPF getCpf() {
        return cpf;
    }
    
    public Phone getPhone() {
        return phone;
    }
    
    public DriverLicense getDriverLicense() {
        return driverLicense;
    }
    
    public DriverStatus getStatus() {
        return status;
    }
    
    public Optional<Location> getCurrentLocation() {
        return Optional.ofNullable(currentLocation);
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
        if (!(o instanceof Driver)) return false;
        Driver driver = (Driver) o;
        return id.equals(driver.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Driver(id=%s, name=%s, status=%s, tenant=%s)",
            id, fullName, status, tenantId);
    }
}
