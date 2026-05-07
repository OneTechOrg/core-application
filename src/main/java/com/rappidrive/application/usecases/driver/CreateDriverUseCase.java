package com.rappidrive.application.usecases.driver;

import com.rappidrive.application.ports.input.driver.CreateDriverInputPort;
import com.rappidrive.application.ports.output.DriverApprovalRepositoryPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.entities.DriverApproval;
import com.rappidrive.domain.enums.DriverStatus;
import com.rappidrive.domain.exceptions.EntityAlreadyExistsException;
import com.rappidrive.domain.events.DriverKeycloakLinkedEvent;
import com.rappidrive.domain.events.DriverApprovalSubmittedEvent;
import com.rappidrive.domain.events.DomainEventsCollector;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateDriverUseCase implements CreateDriverInputPort {
    
    private final DriverRepositoryPort driverRepository;
    private final DriverApprovalRepositoryPort approvalRepository;

    public CreateDriverUseCase(DriverRepositoryPort driverRepository,
                               DriverApprovalRepositoryPort approvalRepository) {
        this.driverRepository = driverRepository;
        this.approvalRepository = approvalRepository;
    }
    
    @Override
    public Driver execute(CreateDriverCommand command) {
        // Validate uniqueness
        if (driverRepository.existsByEmail(command.email())) {
            throw new EntityAlreadyExistsException("Driver with email " + command.email() + " already exists");
        }
        
        if (driverRepository.existsByCpf(command.cpf())) {
            throw new EntityAlreadyExistsException("Driver with CPF " + command.cpf() + " already exists");
        }
        
        // Validate documents (at least 1)
        List<String> documents = sanitizeDocuments(command.documentUrls());
        if (documents.isEmpty()) {
            throw new IllegalArgumentException("At least one document URL is required for registration");
        }
        
        // Create driver (starts as PENDING_APPROVAL)
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
        
        // Create approval request automatically
        UUID approvalId = UUID.randomUUID();
        DriverApproval approval = new DriverApproval(
            approvalId,
            savedDriver.getId(),
            savedDriver.getTenantId(),
            toJsonArray(documents)
        );
        
        DriverApproval savedApproval = approvalRepository.save(approval);
        
        // Publish approval submitted event
        DomainEventsCollector.instance().handle(
            new DriverApprovalSubmittedEvent(
                approvalId.toString(),
                LocalDateTime.now(),
                savedDriver.getId(),
                savedApproval.id(),
                documents.size()
            )
        );
        
        return savedDriver;
    }

    private List<String> sanitizeDocuments(List<String> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
            .filter(doc -> doc != null && !doc.isBlank())
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private String toJsonArray(List<String> items) {
        return items.stream()
            .map(item -> "\"" + item + "\"")
            .collect(Collectors.joining(",", "[", "]"));
    }
}
