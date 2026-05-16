package com.rappidrive.infrastructure.security;

import com.rappidrive.application.ports.output.CurrentUserPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
 * HIST-2026-014: Atualizado para Keycloak JWT
 */
@Component
public class SpringSecurityCurrentUserAdapter implements CurrentUserPort {

    @Value("${rappidrive.security.test-mode:false}")
    private boolean testMode;

    @Override
    public Optional<CurrentUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            // Se estiver em test-mode, tenta buscar do header X-User-Id (suporte para testes E2E)
            if (testMode) {
                return getMockUserFromHeader();
            }
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

    private Optional<CurrentUser> getMockUserFromHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                return parseUuid(userIdHeader).map(uuid -> 
                    new CurrentUser(uuid, "test-user", "test@example.com", List.of("PASSENGER", "DRIVER"), List.of("openid"))
                );
            }
        }
        return Optional.empty();
    }

    /**
     * Extrai o UUID do usuário a partir do claim 'sub' do JWT.
     */
    private Optional<UUID> extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String subject = jwt.getSubject();
            if (subject != null) {
                return parseUuid(subject);
            }
            Object userIdClaim = jwt.getClaim("user_id");
            if (userIdClaim != null) {
                return parseUuid(userIdClaim.toString());
            }
        }
        return parseUuid(authentication.getName());
    }

    private String extractUsername(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String preferredUsername = jwt.getClaim("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
            String email = jwt.getClaim("email");
            if (email != null) {
                return email;
            }
        }
        return authentication.getName();
    }

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
