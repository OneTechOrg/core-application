package com.rappidrive.application.usecases.passenger;

import com.rappidrive.application.ports.input.passenger.CreatePassengerInputPort;
import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.enums.PassengerStatus;
import com.rappidrive.domain.events.PassengerKeycloakLinkedEvent;
import com.rappidrive.domain.events.DomainEventsCollector;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

public class CreatePassengerUseCase implements CreatePassengerInputPort {
    
    private final PassengerRepositoryPort passengerRepository;

    public CreatePassengerUseCase(PassengerRepositoryPort passengerRepository) {
        this.passengerRepository = passengerRepository;
    }
    
    @Override
    public Passenger execute(CreatePassengerCommand command) {
        // Validate uniqueness
        if (passengerRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Passenger with email " + command.email() + " already exists");
        }
        
        // Create passenger (starts as INACTIVE)
        Passenger passenger = new Passenger(
            UUID.randomUUID(), // Generate ID
            command.keycloakId(),
            command.tenantId(),
            command.fullName(),
            command.email(),
            command.phone()
        );
        
        Passenger savedPassenger = passengerRepository.save(passenger);
        
        // Publish linking event
        DomainEventsCollector.instance().handle(
            new PassengerKeycloakLinkedEvent(
                savedPassenger.getId(),
                savedPassenger.getKeycloakId(),
                savedPassenger.getTenantId()
            )
        );
        
        return savedPassenger;
    }
}
