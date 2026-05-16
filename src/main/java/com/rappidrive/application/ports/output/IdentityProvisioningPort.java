package com.rappidrive.application.ports.output;

import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;

/**
 * Output Port for Identity Provider (Keycloak) operations.
 * Handles user and group provisioning for multi-tenancy.
 * 
 * Architecture Decision: Single Realm + Groups per Tenant
 * - All tenants share the same Keycloak Realm ("rappidrive")
 * - Each tenant has a root group (e.g., "tenant:goiania-go")
 * - Users are assigned to tenant groups with tenant_id attribute
 * - Roles (ROLE_ADMIN, ROLE_DRIVER, ROLE_PASSENGER) are realm-level
 */
public interface IdentityProvisioningPort {
    
    /**
     * Create a tenant admin user in Keycloak.
     * 
     * Steps:
     * 1. Create user with email as username
     * 2. Set temporary password (user must change on first login)
     * 3. Create tenant group if not exists: "tenant:{tenantId}"
     * 4. Add user to tenant group
     * 5. Assign ROLE_ADMIN role
     * 6. Set user attribute: tenant_id = {tenantId}
     * 7. Send verification email
     * 
     * @param tenantId The tenant identifier
     * @param adminEmail The admin user email
     * @param tempPassword Temporary password
     * @return The Keycloak user ID
     * @throws IdentityProvisioningException if user creation fails
     */
    String createTenantAdmin(TenantId tenantId, Email adminEmail, String tempPassword);

    /**
     * Create a mobile user (Driver or Passenger) in Keycloak.
     *
     * @param email The user email
     * @param phone The user phone
     * @param fullName The user full name
     * @param password The user password
     * @param role The role to assign (e.g., ROLE_DRIVER, ROLE_PASSENGER)
     * @param tenantId The tenant identifier
     * @return The Keycloak user ID
     */
    String createMobileUser(String email, String phone, String fullName,
                            String password, String role, TenantId tenantId);
    
    /**
     * Create a tenant group in Keycloak.
     * Group name format: "tenant:{tenantId}"
     * 
     * @param tenantId The tenant identifier
     * @return The Keycloak group ID
     * @throws IdentityProvisioningException if group creation fails
     */
    String createTenantGroup(TenantId tenantId);
    
    /**
     * Check if a tenant group already exists.
     * 
     * @param tenantId The tenant identifier
     * @return true if group exists, false otherwise
     */
    boolean tenantGroupExists(TenantId tenantId);
    
    /**
     * Delete a tenant group and all its users.
     * WARNING: This is a destructive operation.
     * 
     * @param tenantId The tenant identifier
     */
    void deleteTenantGroup(TenantId tenantId);
}
