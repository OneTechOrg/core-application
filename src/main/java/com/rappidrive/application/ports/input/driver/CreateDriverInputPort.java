package com.rappidrive.application.ports.input.driver;

import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.*;

/**
 * Input port for creating a new driver.
 */
public interface CreateDriverInputPort {
    
    /**
     * Creates a new driver in the system.
     *
     * @param command the command containing driver creation data
     * @return the created driver
     */
    Driver execute(CreateDriverCommand command);
    
    /**
     * Command record for creating a driver.
     */
    record CreateDriverCommand(
        TenantId tenantId,
        String keycloakId,
        String fullName,
        Email email,
        CPF cpf,
        Phone phone,
        DriverLicense driverLicense
    ) {}
}
