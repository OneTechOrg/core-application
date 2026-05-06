package com.rappidrive.infrastructure.persistence.repositories;

import com.rappidrive.domain.enums.PassengerStatus;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.infrastructure.persistence.entities.PassengerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PassengerJpaEntity.
 */
@Repository
public interface SpringDataPassengerRepository extends JpaRepository<PassengerJpaEntity, UUID> {
    
    Optional<PassengerJpaEntity> findByEmail(Email email);
    
    List<PassengerJpaEntity> findByTenantId(TenantId tenantId);

    Optional<PassengerJpaEntity> findByKeycloakIdAndTenantId(String keycloakId, TenantId tenantId);
    
    List<PassengerJpaEntity> findByStatusAndTenantId(PassengerStatus status, TenantId tenantId);
    
    boolean existsByEmail(Email email);
}
