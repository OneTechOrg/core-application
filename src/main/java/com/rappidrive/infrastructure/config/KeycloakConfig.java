package com.rappidrive.infrastructure.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin Client configuration.
 * Provides programmatic access to Keycloak for user/group management.
 */
@Configuration
public class KeycloakConfig {
    
    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;
    
    @Value("${keycloak.realm:rappidrive}")
    private String realm;
    
    @Value("${keycloak.admin.client-id:admin-cli}")
    private String clientId;
    
    @Value("${keycloak.admin.username:admin}")
    private String username;
    
    @Value("${keycloak.admin.password}")
    private String password;
    
    /**
     * Create Keycloak Admin Client bean.
     * This client authenticates as admin and can manage users, groups, roles.
     */
    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // Admin client uses master realm
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }
    
    /**
     * Target realm for tenant operations.
     */
    @Bean
    public String keycloakRealm() {
        return realm;
    }
}
