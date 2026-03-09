package com.rappidrive.infrastructure.persistence.config;

import com.rappidrive.infrastructure.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect that automatically enables Hibernate Tenant Filter before repository operations.
 * Ensures all JPA queries include "WHERE tenant_id = :tenantId" clause automatically.
 * 
 * <p>This provides an additional layer of security by enforcing tenant isolation at the
 * database query level, preventing accidental data leaks between tenants.
 * 
 * <p>The filter is activated before any repository method execution, and the tenant ID
 * is retrieved from {@link TenantContext}.
 * 
 * <p><b>Note:</b> All JPA entities that require tenant isolation must be annotated with:
 * <pre>
 * {@literal @}FilterDef(name = "tenantFilter", parameters = {@literal @}ParamDef(name = "tenantId", type = UUID.class))
 * {@literal @}Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
 * </pre>
 */
@Aspect
@Component
@Profile("!test & !e2e")
public class TenantFilterAspect {
    
    private static final Logger log = LoggerFactory.getLogger(TenantFilterAspect.class);
    private static final String TENANT_FILTER_NAME = "tenantFilter";
    private static final String TENANT_PARAM_NAME = "tenantId";
    
    private final EntityManager entityManager;
    
    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    /**
     * Enables Hibernate tenant filter before any Spring Data JPA repository method.
     * The filter is only enabled if a tenant is present in the current context.
     * 
     * <p>Pointcut: All methods in packages matching:
     * - com.rappidrive.infrastructure.persistence.repositories..*
     * - com.rappidrive.infrastructure.persistence.adapters..*
     */
    @Before("execution(* com.rappidrive.infrastructure.persistence.repositories..*(..)) || " +
            "execution(* com.rappidrive.infrastructure.persistence.adapters..*(..))")
    public void enableTenantFilter() {
        TenantContext.getTenantIfPresent().ifPresent(tenantId -> {
            try {
                Session session = entityManager.unwrap(Session.class);
                org.hibernate.Filter filter = session.enableFilter(TENANT_FILTER_NAME);
                filter.setParameter(TENANT_PARAM_NAME, UUID.fromString(tenantId.asString()));
                
                log.trace("Tenant filter enabled for tenant: {}", tenantId.asString());
            } catch (Exception e) {
                log.error("Failed to enable tenant filter for tenant: {}", tenantId.asString(), e);
                throw new IllegalStateException("Unable to enforce tenant isolation filter", e);
            }
        });
    }
}
