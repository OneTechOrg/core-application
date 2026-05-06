package com.rappidrive.application.usecases.driver;

import com.rappidrive.application.ports.input.driver.GetCurrentDriverInputPort;
import com.rappidrive.application.ports.output.CurrentUserPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.exceptions.DriverNotFoundException;
import com.rappidrive.domain.valueobjects.TenantId;

/**
 * Use case implementation for retrieving the current authenticated driver.
 */
public class GetCurrentDriverUseCase implements GetCurrentDriverInputPort {
    
    private final DriverRepositoryPort driverRepository;
    private final CurrentUserPort currentUserPort;
    
    public GetCurrentDriverUseCase(DriverRepositoryPort driverRepository, 
                                   CurrentUserPort currentUserPort) {
        this.driverRepository = driverRepository;
        this.currentUserPort = currentUserPort;
    }
    
    @Override
    public Driver execute(TenantId tenantId) {
        CurrentUserPort.CurrentUser user = currentUserPort.getCurrentUser()
            .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
            
        return driverRepository.findByKeycloakId(user.userId().toString(), tenantId)
            .orElseThrow(() -> new DriverNotFoundException(
                "Driver profile not found for the authenticated user"));
    }
}
