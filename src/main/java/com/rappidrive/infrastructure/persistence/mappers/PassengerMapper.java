package com.rappidrive.infrastructure.persistence.mappers;

import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.infrastructure.persistence.entities.PassengerJpaEntity;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between Passenger domain entity and PassengerJpaEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PassengerMapper {
    
    /**
     * Maps JPA entity to domain entity.
     * Uses reconstruction constructor for immutable domain entity.
     */
    default Passenger toDomain(PassengerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new Passenger(
            entity.getId(),
            entity.getKeycloakId(),
            entity.getTenantId(),
            entity.getFullName(),
            entity.getEmail(),
            entity.getPhone(),
            entity.getStatus()
        );
    }
    
    /**
     * Maps domain entity to JPA entity.
     */
    default PassengerJpaEntity toJpaEntity(Passenger domain) {
        if (domain == null) {
            return null;
        }
        
        PassengerJpaEntity entity = new PassengerJpaEntity();
        entity.setId(domain.getId());
        entity.setKeycloakId(domain.getKeycloakId());
        entity.setTenantId(domain.getTenantId());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setStatus(domain.getStatus());
        
        return entity;
    }
    
    /**
     * Updates an existing JPA entity from domain entity.
     */
    default void updateJpaEntity(PassengerJpaEntity entity, Passenger domain) {
        if (entity == null || domain == null) {
            return;
        }
        
        entity.setKeycloakId(domain.getKeycloakId());
        entity.setTenantId(domain.getTenantId());
        entity.setFullName(domain.getFullName());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setStatus(domain.getStatus());
    }
}
