package com.rappidrive.infrastructure.security;

import com.rappidrive.application.ports.output.CurrentUserPort;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter que extrai informações do usuário autenticado a partir do contexto de segurança do Spring.
 * 
 * Implementa CurrentUserPort (application layer) isolando a camada de domínio de detalhes do Spring Security.
 * 
 * Com Keycloak, extrai:
 * - sub (UUID do usuário no Keycloak)
 * - email (claim padrão do OIDC)
 * - preferred_username (username do Keycloak)
 * - realm_access.roles (roles do realm)
 * - resource_access.{client-id}.roles (roles do client)
 * 
 * HIST-2026-014: Atualizado para Keycloak JWT
 */
@Component
public class SpringSecurityCurrentUserAdapter implements CurrentUserPort {

    @Override
    public Optional<CurrentUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        UUID userId = extractUserId(authentication).orElse(null);
        String username = extractUsername(authentication);
        String email = extractEmail(authentication);
        List<String> roles = filterAuthorities(authorities, "ROLE_");
        List<String> scopes = filterAuthorities(authorities, "SCOPE_");

        return Optional.of(new CurrentUser(userId, username, email, roles, scopes));
    }

    /**
     * Extrai o UUID do usuário a partir do claim 'sub' do JWT.
     * 
     * Keycloak sempre retorna o UUID do usuário no claim 'sub' (subject).
     */
    private Optional<UUID> extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            
            // Keycloak: sub = UUID do usuário
            String subject = jwt.getSubject();
            if (subject != null) {
                return parseUuid(subject);
            }
            
            // Fallback: user_id claim (caso customizado)
            Object userIdClaim = jwt.getClaim("user_id");
            if (userIdClaim != null) {
                return parseUuid(userIdClaim.toString());
            }
        }
        
        // Fallback final: tenta parsear o nome como UUID
        return parseUuid(authentication.getName());
    }

    /**
     * Extrai o username a partir do claim 'preferred_username' do JWT.
     * 
     * Keycloak usa 'preferred_username' como username padrão.
     */
    private String extractUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            
            // Keycloak: preferred_username
            String preferredUsername = jwt.getClaim("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
            
            // Fallback: email
            String email = jwt.getClaim("email");
            if (email != null) {
                return email;
            }
        }
        
        return authentication.getName();
    }

    /**
     * Extrai o email a partir do claim 'email' do JWT.
     * 
     * Keycloak inclui 'email' como claim padrão do OIDC.
     */
    private String extractEmail(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim("email");
        }
        return null;
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private List<String> filterAuthorities(Collection<String> authorities, String prefix) {
        return authorities.stream()
            .filter(Objects::nonNull)
            .filter(auth -> auth.startsWith(prefix))
            .map(auth -> auth.substring(prefix.length()))
            .toList();
    }
}
