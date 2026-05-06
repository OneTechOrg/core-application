package com.rappidrive.application.ports.output;

import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.CPF;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.domain.enums.DriverStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Driver repository operations.
 * This interface defines the contract for persisting and retrieving Driver entities.
 * Implementations must be provided in the infrastructure layer.
 */
public interface DriverRepositoryPort {
    
    /**
     * Saves a driver entity.
     *
     * @param driver the driver to save
     * @return the saved driver with updated metadata (e.g., generated ID if new)
     */
    Driver save(Driver driver);
    
    /**
     * Finds a driver by its unique identifier.
     *
     * @param id the driver ID
     * @return an Optional containing the driver if found, empty otherwise
     */
    Optional<Driver> findById(UUID id);
    
    /**
     * Finds all drivers belonging to a specific tenant.
     *
     * @param tenantId the tenant identifier
     * @return list of drivers for the tenant
     */
    List<Driver> findByTenantId(TenantId tenantId);
    
    /**
     * Finds a driver by email address.
     *
     * @param email the driver's email
     * @return an Optional containing the driver if found, empty otherwise
     */
    Optional<Driver> findByEmail(Email email);
    
    /**
     * Finds a driver by CPF (Brazilian tax ID).
     *
     * @param cpf the driver's CPF
     * @return an Optional containing the driver if found, empty otherwise
     */
    Optional<Driver> findByCpf(CPF cpf);
    
    /**
     * Finds all drivers with a specific status for a given tenant.
     *
     * @param status the driver status to filter by
     * @param tenantId the tenant identifier
     * @return list of drivers matching the status
     */
    List<Driver> findByStatus(DriverStatus status, TenantId tenantId);
    
    /**
     * Checks if a driver with the given email exists.
     *
     * @param email the email to check
     * @return true if a driver with this email exists, false otherwise
     */
    boolean existsByEmail(Email email);
    
    /**
     * Checks if a driver with the given CPF exists.
     *
     * @param cpf the CPF to check
     * @return true if a driver with this CPF exists, false otherwise
     */
    boolean existsByCpf(CPF cpf);
    
    /**
     * Finds a driver by their external Keycloak identity ID.
     *
     * @param keycloakId the Keycloak user ID (sub claim)
     * @param tenantId the tenant identifier
     * @return an Optional containing the driver if found, empty otherwise
     */
    Optional<Driver> findByKeycloakId(String keycloakId, TenantId tenantId);
    
    /**
     * Deletes a driver by ID.
     *
     * @param id the driver ID to delete
     */
    void delete(UUID id);
}
