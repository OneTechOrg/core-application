package com.rappidrive.infrastructure.persistence.adapters;

import com.rappidrive.application.ports.output.DriverGeoQueryPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.application.usecases.driver.DriverQueryException;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.valueobjects.CPF;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Location;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.persistence.entities.DriverJpaEntity;
import com.rappidrive.infrastructure.persistence.mappers.DriverMapper;
import com.rappidrive.infrastructure.persistence.repositories.SpringDataDriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA adapter implementation of DriverRepositoryPort and DriverGeoQueryPort.
 * This adapter converts between domain entities and JPA entities.
 * Implements geospatial queries using PostGIS with performance monitoring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaDriverRepositoryAdapter implements DriverRepositoryPort, DriverGeoQueryPort {
    
    private static final Logger log = LoggerFactory.getLogger(JpaDriverRepositoryAdapter.class);
    
    private final SpringDataDriverRepository jpaRepository;
    private final DriverMapper mapper;
    
    @Override
    public Driver save(Driver driver) {
        DriverJpaEntity entity;
        
        if (driver.getId() != null) {
            // Update existing
            entity = jpaRepository.findById(driver.getId())
                .orElseGet(() -> mapper.toJpaEntity(driver));
            mapper.updateJpaEntity(entity, driver);
        } else {
            // Create new
            entity = mapper.toJpaEntity(driver);
        }
        
        DriverJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Driver> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Driver> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public Optional<Driver> findByEmail(Email email) {
        return jpaRepository.findByEmail(email)
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Driver> findByCpf(CPF cpf) {
        return jpaRepository.findByCpf(cpf)
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Driver> findByStatus(DriverStatus status, TenantId tenantId) {
        return jpaRepository.findByStatusAndTenantId(status, tenantId).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email);
    }
    
    @Override
    public boolean existsByCpf(CPF cpf) {
        return jpaRepository.existsByCpf(cpf);
    }
    
    @Override
    public Optional<Driver> findByKeycloakId(String keycloakId, TenantId tenantId) {
        return jpaRepository.findByKeycloakIdAndTenantId(keycloakId, tenantId)
            .map(mapper::toDomain);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
    
    // DriverGeoQueryPort implementation
    
    @Override
    public List<Driver> findAvailableDriversNearby(Location pickupLocation, double radiusKm, TenantId tenantId) {
        long startTime = System.nanoTime();
        
        try {
            List<DriverJpaEntity> entities = jpaRepository.findDriversWithinRadius(
                    pickupLocation.getLatitude(),
                    pickupLocation.getLongitude(),
                    radiusKm * 1000, // Convert km to meters for PostGIS
                    tenantId.getValue()
                );
            
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            log.debug("Geospatial query completed in {}ms for tenant={}, location=({}, {}), radius={}km, results={}", 
                      durationMs, tenantId.getValue(), pickupLocation.getLatitude(), pickupLocation.getLongitude(),
                      radiusKm, entities.size());
            
            if (durationMs > 100) {
                log.warn("Slow geospatial query detected: {}ms (threshold: 100ms) - tenant={}, radius={}km",
                         durationMs, tenantId.getValue(), radiusKm);
            }
            
            return entities.stream()
                .map(mapper::toDomain)
                .toList();
                
        } catch (Exception e) {
            log.error("Geospatial query failed for tenant={}, location=({}, {}), radius={}km", 
                      tenantId.getValue(), pickupLocation.getLatitude(), pickupLocation.getLongitude(), radiusKm, e);
            throw new DriverQueryException("Failed to query drivers near location", e);
        }
    }
}