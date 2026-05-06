package com.rappidrive.application.ports.output;

import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.domain.enums.PassengerStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Passenger repository operations.
 * This interface defines the contract for persisting and retrieving Passenger entities.
 * Implementations must be provided in the infrastructure layer.
 */
public interface PassengerRepositoryPort {
    
    /**
     * Saves a passenger entity.
     *
     * @param passenger the passenger to save
     * @return the saved passenger with updated metadata (e.g., generated ID if new)
     */
    Passenger save(Passenger passenger);
    
    /**
     * Finds a passenger by its unique identifier.
     *
     * @param id the passenger ID
     * @return an Optional containing the passenger if found, empty otherwise
     */
    Optional<Passenger> findById(UUID id);
    
    /**
     * Finds all passengers belonging to a specific tenant.
     *
     * @param tenantId the tenant identifier
     * @return list of passengers for the tenant
     */
    List<Passenger> findByTenantId(TenantId tenantId);
    
    /**
     * Finds a passenger by email address.
     *
     * @param email the passenger's email
     * @return an Optional containing the passenger if found, empty otherwise
     */
    Optional<Passenger> findByEmail(Email email);
    
    /**
     * Finds all passengers with a specific status for a given tenant.
     *
     * @param status the passenger status to filter by
     * @param tenantId the tenant identifier
     * @return list of passengers matching the status
     */
    List<Passenger> findByStatus(PassengerStatus status, TenantId tenantId);
    
    /**
     * Checks if a passenger with the given email exists.
     *
     * @param email the email to check
     * @return true if a passenger with this email exists, false otherwise
     */
    boolean existsByEmail(Email email);

    /**
     * Finds a passenger by their external Keycloak identity ID.
     *
     * @param keycloakId the Keycloak user ID (sub claim)
     * @param tenantId the tenant identifier
     * @return an Optional containing the passenger if found, empty otherwise
     */
    Optional<Passenger> findByKeycloakId(String keycloakId, TenantId tenantId);
    
    /**
     * Deletes a passenger by ID.
     *
     * @param id the passenger ID to delete
     */
    void delete(UUID id);
}
