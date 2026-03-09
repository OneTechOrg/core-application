package com.rappidrive.infrastructure.persistence.config;

import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TenantFilterAspectTest {

    private EntityManager entityManager;
    private TenantFilterAspect aspect;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        aspect = new TenantFilterAspect(entityManager);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void enableTenantFilter_whenSessionFails_shouldThrowIllegalStateException() {
        TenantContext.setTenant(TenantId.generate());
        Session session = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenThrow(new RuntimeException("filter failure"));

        assertThrows(IllegalStateException.class, () -> aspect.enableTenantFilter());
    }

    @Test
    void enableTenantFilter_whenNoTenantInContext_shouldDoNothing() {
        aspect.enableTenantFilter();

        verifyNoInteractions(entityManager);
    }
}

