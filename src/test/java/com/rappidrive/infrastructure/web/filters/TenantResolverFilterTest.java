package com.rappidrive.infrastructure.web.filters;

import com.rappidrive.application.ports.output.TenantRepositoryPort;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.context.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TenantResolverFilterTest {

    private TenantRepositoryPort tenantRepository;
    private TenantResolverFilter filter;

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepositoryPort.class);
        filter = new TenantResolverFilter(tenantRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldSkipTenantHeaderForAdminEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verifyNoInteractions(tenantRepository);
    }

    @Test
    void doFilterInternal_shouldNotSkipTenantHeaderForNonAdminPrefixedEndpoints() throws Exception {
        // /api/admin-tools/ starts with "/api/admin" but must NOT bypass tenant validation
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin-tools/sync");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // No X-Tenant-ID header → filter returns 400
        assertEquals(400, response.getStatus());
    }

    @Test
    void doFilterInternal_shouldSetAndClearTenantContextForProtectedEndpoint() throws Exception {
        TenantId tenantId = TenantId.generate();
        when(tenantRepository.existsById(any(TenantId.class))).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trips/123");
        request.addHeader("X-Tenant-ID", tenantId.asString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<TenantId> tenantDuringFilter = new AtomicReference<>();
        FilterChain chain = (req, res) -> tenantDuringFilter.set(TenantContext.getTenant());

        filter.doFilter(request, response, chain);

        assertEquals(tenantId, tenantDuringFilter.get());
        assertTrue(TenantContext.getTenantIfPresent().isEmpty());
    }

    @Test
    void doFilterInternal_shouldPassWhenJwtTenantClaimMatchesHeader() throws Exception {
        TenantId tenantId = TenantId.generate();
        when(tenantRepository.existsById(any(TenantId.class))).thenReturn(true);

        setJwtAuthentication(tenantId.asString());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trips/123");
        request.addHeader("X-Tenant-ID", tenantId.asString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_shouldReturn403WhenJwtTenantClaimDiffersFromHeader() throws Exception {
        TenantId headerTenant = TenantId.generate();
        TenantId jwtTenant = TenantId.generate();
        when(tenantRepository.existsById(any(TenantId.class))).thenReturn(true);

        setJwtAuthentication(jwtTenant.asString());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trips/123");
        request.addHeader("X-Tenant-ID", headerTenant.asString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(TenantContext.getTenantIfPresent().isEmpty());
    }

    @Test
    void doFilterInternal_shouldPassWhenJwtHasNoTenantClaim() throws Exception {
        TenantId tenantId = TenantId.generate();
        when(tenantRepository.existsById(any(TenantId.class))).thenReturn(true);

        // JWT without tenant_id claim
        setJwtAuthentication(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trips/123");
        request.addHeader("X-Tenant-ID", tenantId.asString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    // --- helpers ---

    private void setJwtAuthentication(String tenantIdClaim) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("tenant_id")).thenReturn(tenantIdClaim);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
