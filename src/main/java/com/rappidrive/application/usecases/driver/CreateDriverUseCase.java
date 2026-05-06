package com.rappidrive.application.usecases.driver;

import com.rappidrive.application.ports.input.driver.CreateDriverInputPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.events.DriverKeycloakLinkedEvent;
import com.rappidrive.domain.events.DomainEventsCollector;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

public class CreateDriverUseCase implements CreateDriverInputPort {
    
    private final DriverRepositoryPort driverRepository;

    public CreateDriverUseCase(DriverRepositoryPort driverRepository) {
        this.driverRepository = driverRepository;
    }
    
    @Override
    public Driver execute(CreateDriverCommand command) {
        // Validate uniqueness
        if (driverRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Driver with email " + command.email() + " already exists");
        }
        
        if (driverRepository.existsByCpf(command.cpf())) {
            throw new IllegalArgumentException("Driver with CPF " + command.cpf() + " already exists");
        }
        
        // Create driver (starts as INACTIVE)
        Driver driver = new Driver(
            UUID.randomUUID(), // Generate new UUID
            command.keycloakId(),
            command.tenantId(),
            command.fullName(),
            command.email(),
            command.cpf(),
            command.phone(),
            command.driverLicense()
        );
        
        Driver savedDriver = driverRepository.save(driver);
        
        // Publish linking event
        DomainEventsCollector.instance().handle(
            new DriverKeycloakLinkedEvent(
                savedDriver.getId(),
                savedDriver.getKeycloakId(),
                savedDriver.getTenantId()
            )
        );
        
        return savedDriver;
    }
}
