package com.rappidrive.application.ports.input.driver;

import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.TenantId;

/**
 * Input port for retrieving the current authenticated driver.
 */
public interface GetCurrentDriverInputPort {
    
    /**
     * Retrieves the driver profile linked to the current authenticated user.
     * 
     * @param tenantId current tenant identifier
     * @return the driver profile
     */
    Driver execute(TenantId tenantId);
}
