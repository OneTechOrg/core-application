package com.rappidrive.application.ports.input.passenger;

import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.valueobjects.TenantId;

/**
 * Input port for retrieving the current authenticated passenger.
 */
public interface GetCurrentPassengerInputPort {
    
    /**
     * Retrieves the passenger profile linked to the current authenticated user.
     * 
     * @param tenantId current tenant identifier
     * @return the passenger profile
     */
    Passenger execute(TenantId tenantId);
}
