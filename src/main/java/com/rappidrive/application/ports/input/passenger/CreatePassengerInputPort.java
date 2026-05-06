package com.rappidrive.application.ports.input.passenger;

import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.valueobjects.Email;
import com.rappidrive.domain.valueobjects.Phone;
import com.rappidrive.domain.valueobjects.TenantId;

/**
 * Input port for creating a new passenger.
 */
public interface CreatePassengerInputPort {
    
    /**
     * Creates a new passenger in the system.
     *
     * @param command the command containing passenger creation data
     * @return the created passenger
     */
    Passenger execute(CreatePassengerCommand command);
    
    /**
     * Command record for creating a passenger.
     */
    record CreatePassengerCommand(
        TenantId tenantId,
        String keycloakId,
        String fullName,
        Email email,
        Phone phone
    ) {}
}
