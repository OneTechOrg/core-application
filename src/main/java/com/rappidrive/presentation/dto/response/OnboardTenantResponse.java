package com.rappidrive.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for tenant onboarding operation.
 */
public record OnboardTenantResponse(
        
        @JsonProperty("tenant_id")
        String tenantId,
        
        @JsonProperty("display_name")
        String displayName,
        
        @JsonProperty("admin_email")
        String adminEmail,
        
        @JsonProperty("keycloak_user_id")
        String keycloakUserId,
        
        @JsonProperty("keycloak_group_id")
        String keycloakGroupId,
        
        @JsonProperty("service_area_id")
        String serviceAreaId,
        
        @JsonProperty("fare_configuration_id")
        String fareConfigurationId,
        
        @JsonProperty("message")
        String message
) {
}
