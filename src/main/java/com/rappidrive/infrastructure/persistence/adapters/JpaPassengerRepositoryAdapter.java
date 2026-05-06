package com.rappidrive.infrastructure.persistence.adapters;

import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.enums.PassengerStatus;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.persistence.entities.PassengerJpaEntity;
import com.rappidrive.infrastructure.persistence.mappers.PassengerMapper;
import com.rappidrive.infrastructure.persistence.repositories.SpringDataPassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementation of PassengerRepositoryPort.
 * This adapter converts between domain entities and JPA entities.
 */
@Component
@RequiredArgsConstructor
public class JpaPassengerRepositoryAdapter implements PassengerRepositoryPort {
    
    private final SpringDataPassengerRepository jpaRepository;
    private final PassengerMapper mapper;
    
    @Override
    public Passenger save(Passenger passenger) {
        PassengerJpaEntity entity;
        
        if (passenger.getId() != null) {
            // Update existing
            entity = jpaRepository.findById(passenger.getId())
                .orElseGet(() -> mapper.toJpaEntity(passenger));
            mapper.updateJpaEntity(entity, passenger);
        } else {
            // Create new
            entity = mapper.toJpaEntity(passenger);
        }
        
        PassengerJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Passenger> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Passenger> findByTenantId(TenantId tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public Optional<Passenger> findByEmail(Email email) {
        return jpaRepository.findByEmail(email)
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Passenger> findByStatus(PassengerStatus status, TenantId tenantId) {
        return jpaRepository.findByStatusAndTenantId(status, tenantId).stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<Passenger> findByKeycloakId(String keycloakId, TenantId tenantId) {
        return jpaRepository.findByKeycloakIdAndTenantId(keycloakId, tenantId)
            .map(mapper::toDomain);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
