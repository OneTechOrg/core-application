package com.rappidrive.infrastructure.config;

import com.rappidrive.infrastructure.security.keycloak.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança centralizada para o RappiDrive.
 * Unifica os modos de produção, teste e desenvolvimento em um único componente.
 * 
 * HIST-2026-025: Hardening de segurança e isolamento.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8180/realms/rappidrive-test/protocol/openid-connect/certs}")
    private String jwkSetUri;
    
    @Value("${keycloak.resource:rappidrive-api}")
    private String keycloakClientId;

    @Value("${rappidrive.security.e2e-permit-all:false}")
    private boolean e2ePermitAll;

    @Value("${rappidrive.docs.public:true}")
    private boolean docsPublic;

    /**
     * Configuração para ambiente de teste ou quando a segurança está desabilitada.
     * Ativada se test-mode=true OU security.enabled=false.
     */
    @Bean
    @Order(1)
    @ConditionalOnExpression("${rappidrive.security.test-mode:false} || !${rappidrive.security.enabled:true}")
    public SecurityFilterChain permitAllFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    /**
     * Configuração padrão de produção (OAuth2 / Keycloak).
     * Ativada SOMENTE se security.enabled=true E test-mode=false.
     */
    @Bean
    @Order(2)
    @ConditionalOnExpression("${rappidrive.security.enabled:true} && !${rappidrive.security.test-mode:false}")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                if (docsPublic) {
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                }

                auth.requestMatchers(
                    "/actuator/health",
                    "/actuator/prometheus",
                    "/api/health"
                ).permitAll();

                // Public onboarding endpoints
                auth.requestMatchers("/api/v1/drivers", "/api/v1/passengers").permitAll();
                
                if (e2ePermitAll) {
                    auth.requestMatchers("/api/v1/**").permitAll();
                } else {
                    auth.requestMatchers("/api/v1/drivers/**").hasRole("DRIVER")
                        .requestMatchers("/api/v1/passengers/**").hasRole("PASSENGER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/trips/**").authenticated()
                        .requestMatchers("/api/v1/vehicles/**").authenticated()
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        .requestMatchers("/api/v1/ratings/**").authenticated()
                        .requestMatchers("/api/v1/notifications/**").authenticated();
                }
                
                auth.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public KeycloakJwtAuthenticationConverter jwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter(keycloakClientId);
    }
}
