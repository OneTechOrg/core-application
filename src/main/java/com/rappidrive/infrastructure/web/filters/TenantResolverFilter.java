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
import org.springframework.stereotype.Component;
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
 * <p>Future: Validate JWT claim 'tenant_id' matches X-Tenant-ID when authentication is implemented.
 */
@Component
@Order(1) // Execute before Spring Security filter (typically @Order(5))
@Profile("!test & !e2e")
public class TenantResolverFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(TenantResolverFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    // Public endpoints that don't require tenant context
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus",
        "/v3/api-docs",
        "/swagger-ui",
        "/api-docs",
        "/api/admin/",
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
            
            // TODO: Future - Validate JWT claim 'tenant_id' matches X-Tenant-ID
            // When Spring Security is implemented:
            // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // if (auth != null && auth.isAuthenticated()) {
            //     String jwtTenantId = extractTenantFromJWT(auth);
            //     if (!jwtTenantId.equals(tenantId.asString())) {
            //         log.error("JWT tenant mismatch: JWT={}, Header={}", jwtTenantId, tenantId.asString());
            //         sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
            //             "Tenant mismatch between JWT and header");
            //         return;
            //     }
            // }
            
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
     * Checks if the request URI matches any public endpoint pattern.
     */
    private boolean isPublicEndpoint(String requestUri) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(requestUri::startsWith);
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
