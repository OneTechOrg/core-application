package com.rappidrive.domain.valueobjects;

import com.rappidrive.domain.enums.AdminRole;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class AdminUser {
    private final UUID id;
    private final Email email;
    private final AdminRole role;
    private final String fullName;
    private final TenantId tenantId;
    private final LocalDateTime createdAt;
    
    public AdminUser(UUID id, Email email, AdminRole role, String fullName,
                     TenantId tenantId, LocalDateTime createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("Admin ID cannot be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or empty");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName.trim();
        this.tenantId = tenantId;
        this.createdAt = createdAt;
    }
    
    public UUID id() {
        return id;
    }
    
    public Email email() {
        return email;
    }
    
    public AdminRole role() {
        return role;
    }
    
    public String fullName() {
        return fullName;
    }

    public TenantId tenantId() {
        return tenantId;
    }
    
    public LocalDateTime createdAt() {
        return createdAt;
    }
    
    public boolean canApproveDrivers() {
        return role == AdminRole.SUPER_ADMIN || 
               role == AdminRole.COMPLIANCE_OFFICER;
    }
    
    public boolean canRejectDrivers() {
        return role == AdminRole.SUPER_ADMIN || 
               role == AdminRole.COMPLIANCE_OFFICER;
    }
    
    public boolean hasRole(AdminRole requiredRole) {
        return this.role == requiredRole;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminUser)) return false;
        AdminUser adminUser = (AdminUser) o;
        return Objects.equals(id, adminUser.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "AdminUser{" +
                "id=" + id +
                ", email=" + email +
                ", role=" + role +
            ", tenantId=" + tenantId +
                '}';
    }
}
