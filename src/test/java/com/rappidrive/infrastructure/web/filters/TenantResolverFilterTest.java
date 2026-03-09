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
}
