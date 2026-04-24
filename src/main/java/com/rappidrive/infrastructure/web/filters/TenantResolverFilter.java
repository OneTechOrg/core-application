package com.rappidrive.infrastructure.web.filters;

import com.rappidrive.application.ports.output.TenantRepositoryPort;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * HTTP Filter that resolves and validates the Tenant ID from the X-Tenant-ID header.
 * Executes BEFORE Spring Security filter chain to ensure tenant context is available
 * for authentication and authorization decisions.
 * 
 * <p>Resolution Logic:
 * <ol>
 *   <li>Extracts X-Tenant-ID header from request</li>
 *   <li>Validates Tenant ID format (UUID)</li>
 *   <li>Checks if Tenant exists in database (TODO: add caching)</li>
 *   <li>Stores in TenantContext for request lifecycle</li>
 *   <li>Returns 400 Bad Request if header missing/invalid</li>
 *   <li>Returns 404 Not Found if tenant doesn't exist</li>
 *   <li>Clears context in finally block to prevent leaks</li>
 * </ol>
 * 
 * <p>If a valid JWT is present in the SecurityContext (Spring Security Order -100 runs first),
 * the filter also verifies that the JWT {@code tenant_id} claim matches the header value,
 * returning 403 if they differ.
 */
@Component
@Order(1) // Spring Security FilterChainProxy runs at Order(-100); this filter sees an already-authenticated context
@Profile("!test & !e2e")
public class TenantResolverFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(TenantResolverFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // Patterns for endpoints that don't require tenant context.
    // Uses AntPathMatcher so /api/admin/** does not inadvertently match /api/administrators/.
    private static final Set<String> PUBLIC_ENDPOINT_PATTERNS = Set.of(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui",
        "/swagger-ui/**",
        "/api-docs",
        "/api-docs/**",
        "/api/admin/**",
        "/error"
    );
    
    private final TenantRepositoryPort tenantRepository;
    
    public TenantResolverFilter(TenantRepositoryPort tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip tenant resolution for public endpoints
        if (isPublicEndpoint(requestUri)) {
            log.trace("Skipping tenant resolution for public endpoint: {} {}", method, requestUri);
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String tenantIdHeader = request.getHeader(TENANT_HEADER);
            
            // Validate header presence
            if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
                log.warn("Missing {} header for request: {} {}", TENANT_HEADER, method, requestUri);
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required header: " + TENANT_HEADER);
                return;
            }
            
            // Parse and validate Tenant ID format
            TenantId tenantId;
            try {
                tenantId = TenantId.fromString(tenantIdHeader);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid {} format: {} for request: {} {}", 
                    TENANT_HEADER, tenantIdHeader, method, requestUri, e);
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid Tenant ID format. Expected UUID.");
                return;
            }
            
            // Validate tenant exists in database
            // TODO: Add caching here to avoid DB hit on every request (Caffeine cache, TTL: 5min)
            boolean tenantExists = tenantRepository.existsById(tenantId);
            if (!tenantExists) {
                log.warn("Tenant not found: {} for request: {} {}", 
                    tenantId.asString(), method, requestUri);
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    "Tenant not found");
                return;
            }
            
            // Validate JWT tenant_id claim matches X-Tenant-ID header.
            // Spring Security (Order -100) runs before this filter (Order 1), so the
            // SecurityContext is already populated when we reach this point.
            if (!isJwtTenantConsistent(tenantId, response, method, requestUri)) {
                return;
            }

            // Set tenant in context for downstream components
            TenantContext.setTenant(tenantId);
            log.debug("Tenant resolved: {} for request: {} {}", 
                tenantId.asString(), method, requestUri);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // CRITICAL: Always clear context to prevent memory leaks in thread pools
            TenantContext.clear();
            log.trace("Tenant context cleared for request: {} {}", method, requestUri);
        }
    }
    
    /**
     * Returns false and writes a 403 response if the authenticated JWT contains a
     * {@code tenant_id} claim that does not match the resolved tenant header.
     * Returns true in all other cases (no auth, no claim, or matching claim).
     */
    private boolean isJwtTenantConsistent(TenantId resolvedTenantId,
                                          HttpServletResponse response,
                                          String method,
                                          String requestUri) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return true;
        }
        String jwtTenantId = jwtAuth.getToken().getClaimAsString("tenant_id");
        if (jwtTenantId == null) {
            return true;
        }
        if (!jwtTenantId.equals(resolvedTenantId.asString())) {
            log.warn("JWT tenant mismatch: jwt={}, header={}, request: {} {}",
                jwtTenantId, resolvedTenantId.asString(), method, requestUri);
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                "Tenant mismatch between JWT and header");
            return false;
        }
        return true;
    }

    /**
     * Checks if the request URI matches any public endpoint pattern.
     */
    private boolean isPublicEndpoint(String requestUri) {
        return PUBLIC_ENDPOINT_PATTERNS.stream()
            .anyMatch(pattern -> PATH_MATCHER.match(pattern, requestUri));
    }
    
    /**
     * Sends a JSON error response with proper content type and status code.
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String errorMessage) 
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
            errorMessage,
            statusCode,
            java.time.Instant.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
