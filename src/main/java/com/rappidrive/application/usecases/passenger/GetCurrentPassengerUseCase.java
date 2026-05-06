package com.rappidrive.application.usecases.passenger;

import com.rappidrive.application.ports.input.passenger.GetCurrentPassengerInputPort;
import com.rappidrive.application.ports.output.CurrentUserPort;
import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.exceptions.PassengerNotFoundException;
import com.rappidrive.domain.valueobjects.TenantId;

/**
 * Use case implementation for retrieving the current authenticated passenger.
 */
public class GetCurrentPassengerUseCase implements GetCurrentPassengerInputPort {
    
    private final PassengerRepositoryPort passengerRepository;
    private final CurrentUserPort currentUserPort;
    
    public GetCurrentPassengerUseCase(PassengerRepositoryPort passengerRepository, 
                                      CurrentUserPort currentUserPort) {
        this.passengerRepository = passengerRepository;
        this.currentUserPort = currentUserPort;
    }
    
    @Override
    public Passenger execute(TenantId tenantId) {
        CurrentUserPort.CurrentUser user = currentUserPort.getCurrentUser()
            .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
            
        return passengerRepository.findByKeycloakId(user.userId().toString(), tenantId)
            .orElseThrow(() -> new PassengerNotFoundException(
                "Passenger profile not found for the authenticated user"));
    }
}
