package com.rappidrive.infrastructure.persistence.entities;

import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.valueobjects.CPF;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Phone;
import com.rappidrive.domain.valueobjects.TenantId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for Driver.
 * This is part of the infrastructure layer and should NOT be exposed to domain or application layers.
 */
@Entity
@Table(name = "drivers")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Builder
public class DriverJpaEntity {

    public DriverJpaEntity() {}

    public DriverJpaEntity(UUID id,
                           String keycloakId,
                           TenantId tenantId,
                           String fullName,
                           Email email,
                           CPF cpf,
                           Phone phone,
                           String driverLicenseNumber,
                           String driverLicenseCategory,
                           LocalDate driverLicenseIssueDate,
                           LocalDate driverLicenseExpirationDate,
                           Boolean driverLicenseIsDefinitive,
                           DriverStatus status,
                           Double locationLatitude,
                           Double locationLongitude,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.email = email;
        this.cpf = cpf;
        this.phone = phone;
        this.driverLicenseNumber = driverLicenseNumber;
        this.driverLicenseCategory = driverLicenseCategory;
        this.driverLicenseIssueDate = driverLicenseIssueDate;
        this.driverLicenseExpirationDate = driverLicenseExpirationDate;
        this.driverLicenseIsDefinitive = driverLicenseIsDefinitive;
        this.status = status;
        this.locationLatitude = locationLatitude;
        this.locationLongitude = locationLongitude;
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
    
    @Column(nullable = false, unique = true, length = 11)
    private CPF cpf;
    
    @Column(nullable = false, length = 20)
    private Phone phone;
    
    // Driver License fields
    @Column(name = "driver_license_number", nullable = false, length = 11)
    private String driverLicenseNumber;
    
    @Column(name = "driver_license_category", nullable = false, length = 2)
    private String driverLicenseCategory;
    
    @Column(name = "driver_license_issue_date", nullable = false)
    private LocalDate driverLicenseIssueDate;
    
    @Column(name = "driver_license_expiration_date", nullable = false)
    private LocalDate driverLicenseExpirationDate;
    
    @Column(name = "driver_license_is_definitive", nullable = false)
    private Boolean driverLicenseIsDefinitive;
    
    // Status and Location
    @Column(nullable = false, length = 20)
    private DriverStatus status;
    
    @Column(name = "location_latitude")
    private Double locationLatitude;
    
    @Column(name = "location_longitude")
    private Double locationLongitude;
    
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

    // Explicit getters to ensure methods are available during compilation
    public UUID getId() {
        return this.id;
    }

    public String getKeycloakId() {
        return this.keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public TenantId getTenantId() {
        return this.tenantId;
    }

    public String getFullName() {
        return this.fullName;
    }

    public Email getEmail() {
        return this.email;
    }

    public CPF getCpf() {
        return this.cpf;
    }

    public Phone getPhone() {
        return this.phone;
    }

    public String getDriverLicenseNumber() {
        return this.driverLicenseNumber;
    }

    public String getDriverLicenseCategory() {
        return this.driverLicenseCategory;
    }

    public LocalDate getDriverLicenseIssueDate() {
        return this.driverLicenseIssueDate;
    }

    public LocalDate getDriverLicenseExpirationDate() {
        return this.driverLicenseExpirationDate;
    }

    public Boolean getDriverLicenseIsDefinitive() {
        return this.driverLicenseIsDefinitive;
    }

    public Double getLocationLatitude() {
        return this.locationLatitude;
    }

    public Double getLocationLongitude() {
        return this.locationLongitude;
    }
    public DriverStatus getStatus() { return this.status; }
    public void setStatus(DriverStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDriverLicenseNumber(String driverLicenseNumber) {
        this.driverLicenseNumber = driverLicenseNumber;
    }

    public void setDriverLicenseCategory(String driverLicenseCategory) {
        this.driverLicenseCategory = driverLicenseCategory;
    }

    public void setDriverLicenseIssueDate(LocalDate driverLicenseIssueDate) {
        this.driverLicenseIssueDate = driverLicenseIssueDate;
    }

    public void setDriverLicenseExpirationDate(LocalDate driverLicenseExpirationDate) {
        this.driverLicenseExpirationDate = driverLicenseExpirationDate;
    }

    public void setDriverLicenseIsDefinitive(Boolean driverLicenseIsDefinitive) {
        this.driverLicenseIsDefinitive = driverLicenseIsDefinitive;
    }

    public void setLocationLatitude(Double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    public void setLocationLongitude(Double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }
}
