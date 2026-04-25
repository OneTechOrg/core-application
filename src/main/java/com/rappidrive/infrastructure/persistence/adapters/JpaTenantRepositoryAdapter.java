package com.rappidrive.infrastructure.persistence.adapters;

import com.rappidrive.application.ports.output.TenantRepositoryPort;
import com.rappidrive.domain.entities.Tenant;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.config.CacheConfiguration;
import com.rappidrive.infrastructure.persistence.entities.TenantJpaEntity;
import com.rappidrive.infrastructure.persistence.repositories.SpringDataTenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaTenantRepositoryAdapter implements TenantRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaTenantRepositoryAdapter.class);

    private final SpringDataTenantRepository repository;

    public JpaTenantRepositoryAdapter(SpringDataTenantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity entity = new TenantJpaEntity();
        entity.setId(UUID.fromString(tenant.getId().asString()));
        entity.setName(tenant.getName());
        entity.setSlug(tenant.getSlug());
        entity.setActive(tenant.isActive());
        entity.setCreatedAt(tenant.getCreatedAt());
        entity.setUpdatedAt(tenant.getUpdatedAt());
        TenantJpaEntity saved = repository.save(entity);
        repository.flush();
        log.debug("Persisted tenant id={} name={}", saved.getId(), saved.getName());
        return Tenant.create(TenantId.fromString(saved.getId().toString()), saved.getName(), saved.getSlug());
    }

    @Override
    @Cacheable(value = CacheConfiguration.TENANT_EXISTS_CACHE, key = "#tenantId.asString()")
    public boolean existsById(TenantId tenantId) {
        return repository.existsById(UUID.fromString(tenantId.asString()));
    }

    @Override
    public Optional<Tenant> findById(TenantId tenantId) {
        Optional<TenantJpaEntity> opt = repository.findById(UUID.fromString(tenantId.asString()));
        return opt.map(e -> Tenant.create(TenantId.fromString(e.getId().toString()), e.getName(), e.getSlug()));
    }

    @Override
    public void deleteById(TenantId tenantId) {
        repository.deleteById(UUID.fromString(tenantId.asString()));
    }
}
