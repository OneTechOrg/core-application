package com.rappidrive.infrastructure.adapters.keycloak;

import com.rappidrive.application.exceptions.IdentityProvisioningException;
import com.rappidrive.application.ports.output.IdentityProvisioningPort;
import com.rappidrive.domain.exceptions.EntityAlreadyExistsException;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Keycloak implementation of IdentityProvisioningPort.
 * Handles user and group creation for multi-tenant architecture.
 * 
 * Architecture: Single Realm + Groups per Tenant
 * - Realm: "rappidrive"
 * - Group per tenant: "tenant:{tenantId}"
 * - User attributes: tenant_id = {tenantId}
 * - Roles: ROLE_ADMIN, ROLE_DRIVER, ROLE_PASSENGER (realm-level)
 */
@Component
public class KeycloakProvisioningAdapter implements IdentityProvisioningPort {
    
    private static final Logger log = LoggerFactory.getLogger(KeycloakProvisioningAdapter.class);
    
    private final Keycloak keycloakAdmin;
    private final String realm;
    
    public KeycloakProvisioningAdapter(Keycloak keycloakAdmin, String keycloakRealm) {
        this.keycloakAdmin = keycloakAdmin;
        this.realm = keycloakRealm;
    }
    
    @Override
    public String createTenantAdmin(TenantId tenantId, Email adminEmail, String tempPassword) {
        log.info("Creating tenant admin for tenant: {}, email: {}", tenantId, adminEmail);
        
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();
            
            // 1. Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(adminEmail.getValue());
            user.setEmail(adminEmail.getValue());
            user.setEnabled(true);
            user.setEmailVerified(true); // Auto-verify for admin
            user.setFirstName("Admin");
            user.setLastName(tenantId.getValue().toString());
            
            // Set tenant_id attribute
            user.setAttributes(Map.of("tenant_id", List.of(tenantId.getValue().toString())));
            
            // 2. Create user in Keycloak
            Response response = usersResource.create(user);
            
            if (response.getStatus() != 201) {
                throw new IdentityProvisioningException(
                        "Failed to create user in Keycloak. Status: " + response.getStatus() +
                        ", Reason: " + response.getStatusInfo().getReasonPhrase()
                );
            }
            
            // Extract user ID from Location header
            String userId = extractIdFromLocation(response.getLocation().getPath());
            log.info("User created with ID: {}", userId);
            
            // 3. Set temporary password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(tempPassword);
            credential.setTemporary(true); // User must change on first login
            
            usersResource.get(userId).resetPassword(credential);
            log.info("Temporary password set for user: {}", userId);
            
            // 4. Create tenant group if not exists
            String groupId = createTenantGroup(tenantId);
            
            // 5. Add user to tenant group
            realmResource.users().get(userId).joinGroup(groupId);
            log.info("User {} added to group {}", userId, groupId);
            
            // 6. Assign ROLE_ADMIN role
            assignRoleToUser(userId, "ROLE_ADMIN");
            
            response.close();
            return userId;
            
        } catch (Exception e) {
            log.error("Error creating tenant admin in Keycloak", e);
            throw new IdentityProvisioningException("Failed to create tenant admin: " + e.getMessage(), e);
        }
    }

    @Override
    public String createMobileUser(String email, String phone, String fullName,
                                   String password, String role, TenantId tenantId) {
        log.info("Creating mobile user for tenant: {}, email: {}, role: {}", tenantId, email, role);

        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            UsersResource usersResource = realmResource.users();

            // 1. Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setEnabled(true);
            user.setEmailVerified(true); // Verified via OTP before this call
            user.setFirstName(fullName);

            // Set attributes
            user.setAttributes(Map.of(
                    "tenant_id", List.of(tenantId.getValue().toString()),
                    "phone", List.of(phone)
            ));

            // 2. Create user in Keycloak
            Response response = usersResource.create(user);

            if (response.getStatus() == 409) {
                throw new EntityAlreadyExistsException("User with email " + email + " already exists in Keycloak");
            }

            if (response.getStatus() != 201) {
                throw new IdentityProvisioningException(
                        "Failed to create user in Keycloak. Status: " + response.getStatus() +
                        ", Reason: " + response.getStatusInfo().getReasonPhrase()
                );
            }

            // Extract user ID
            String userId = extractIdFromLocation(response.getLocation().getPath());
            log.info("User created with ID: {}", userId);

            // 3. Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);

            usersResource.get(userId).resetPassword(credential);
            log.info("Password set for user: {}", userId);

            // 4. Create tenant group if not exists
            String groupId = createTenantGroup(tenantId);

            // 5. Add user to tenant group
            realmResource.users().get(userId).joinGroup(groupId);
            log.info("User {} added to group {}", userId, groupId);

            // 6. Assign role
            assignRoleToUser(userId, role);

            response.close();
            return userId;

        } catch (EntityAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating mobile user in Keycloak", e);
            throw new IdentityProvisioningException("Failed to create mobile user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String createTenantGroup(TenantId tenantId) {
        log.info("Creating tenant group for: {}", tenantId);
        
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            String groupName = "tenant:" + tenantId.getValue();
            
            // Check if group already exists
            List<GroupRepresentation> existingGroups = realmResource.groups()
                    .groups(groupName, 0, 1);
            
            if (!existingGroups.isEmpty()) {
                String existingGroupId = existingGroups.get(0).getId();
                log.info("Tenant group already exists with ID: {}", existingGroupId);
                return existingGroupId;
            }
            
            // Create new group
            GroupRepresentation group = new GroupRepresentation();
            group.setName(groupName);
            group.setAttributes(Map.of(
                    "tenant_id", List.of(tenantId.getValue().toString()),
                    "description", List.of("Group for tenant: " + tenantId)
            ));
            
            Response response = realmResource.groups().add(group);
            
            if (response.getStatus() != 201) {
                throw new IdentityProvisioningException(
                        "Failed to create group in Keycloak. Status: " + response.getStatus()
                );
            }
            
            String groupId = extractIdFromLocation(response.getLocation().getPath());
            log.info("Tenant group created with ID: {}", groupId);
            
            response.close();
            return groupId;
            
        } catch (Exception e) {
            log.error("Error creating tenant group in Keycloak", e);
            throw new IdentityProvisioningException("Failed to create tenant group: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean tenantGroupExists(TenantId tenantId) {
        try {
            String groupName = "tenant:" + tenantId.getValue();
            List<GroupRepresentation> groups = keycloakAdmin.realm(realm)
                    .groups()
                    .groups(groupName, 0, 1);
            return !groups.isEmpty();
        } catch (Exception e) {
            log.error("Error checking if tenant group exists", e);
            return false;
        }
    }
    
    @Override
    public void deleteTenantGroup(TenantId tenantId) {
        log.warn("Deleting tenant group for: {}", tenantId);
        
        try {
            String groupName = "tenant:" + tenantId.getValue();
            List<GroupRepresentation> groups = keycloakAdmin.realm(realm)
                    .groups()
                    .groups(groupName, 0, 1);
            
            if (groups.isEmpty()) {
                log.warn("Tenant group not found for deletion: {}", tenantId);
                return;
            }
            
            String groupId = groups.get(0).getId();
            keycloakAdmin.realm(realm).groups().group(groupId).remove();
            log.info("Tenant group deleted: {}", groupId);
            
        } catch (Exception e) {
            log.error("Error deleting tenant group", e);
            throw new IdentityProvisioningException("Failed to delete tenant group: " + e.getMessage(), e);
        }
    }
    
    /**
     * Assign a role to a user.
     * Roles must exist at realm level (created via Keycloak Admin UI or startup script).
     */
    private void assignRoleToUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloakAdmin.realm(realm);
            var roleResource = realmResource.roles().get(roleName).toRepresentation();
            
            realmResource.users().get(userId).roles().realmLevel()
                    .add(Collections.singletonList(roleResource));
            
            log.info("Role {} assigned to user {}", roleName, userId);
            
        } catch (Exception e) {
            log.error("Error assigning role to user", e);
            throw new IdentityProvisioningException("Failed to assign role: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract ID from Keycloak Location header.
     * Example: http://localhost:8080/admin/realms/rappidrive/users/{id}
     */
    private String extractIdFromLocation(String locationPath) {
        String[] parts = locationPath.split("/");
        return parts[parts.length - 1];
    }
}
