package com.rappidrive.application.usecases.driver;

import com.rappidrive.application.ports.input.driver.CreateDriverInputPort;
import com.rappidrive.application.ports.output.DriverApprovalRepositoryPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateDriverUseCaseTest {

    @Mock
    private DriverRepositoryPort driverRepository;
    
    @Mock
    private DriverApprovalRepositoryPort approvalRepository;

    private CreateDriverUseCase useCase;
    
    private CreateDriverInputPort.CreateDriverCommand command;

    @BeforeEach
    void setUp() {
        useCase = new CreateDriverUseCase(driverRepository, approvalRepository);
        
        DriverLicense license = new DriverLicense("12345678901", "B", 
            LocalDate.now().minusYears(1), LocalDate.now().plusYears(5), true);
            
        command = new CreateDriverInputPort.CreateDriverCommand(
            TenantId.generate(),
            UUID.randomUUID().toString(),
            "John Doe",
            new Email("john@example.com"),
            new CPF("12345678909"),
            new Phone("+5511999999999"),
            license,
            java.util.List.of("https://docs.com/cnh.jpg")
        );
    }

    @Test
    void execute_WhenValidCommand_ShouldCreateAndSaveDriverAndApproval() {
        // Given
        when(driverRepository.existsByEmail(command.email())).thenReturn(false);
        when(driverRepository.existsByCpf(command.cpf())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Driver result = useCase.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(command.keycloakId());
        assertThat(result.getEmail()).isEqualTo(command.email());
        
        verify(driverRepository).save(any(Driver.class));
        verify(approvalRepository).save(any());
    }

    @Test
    void execute_WhenNoDocumentsProvided_ShouldThrowException() {
        // Given
        command = new CreateDriverInputPort.CreateDriverCommand(
            command.tenantId(),
            command.keycloakId(),
            command.fullName(),
            command.email(),
            command.cpf(),
            command.phone(),
            command.driverLicense(),
            java.util.List.of()
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one document URL is required");
    }

    @Test
    void execute_WhenEmailAlreadyExists_ShouldThrowException() {
        // Given
        when(driverRepository.existsByEmail(command.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void execute_WhenCpfAlreadyExists_ShouldThrowException() {
        // Given
        when(driverRepository.existsByEmail(command.email())).thenReturn(false);
        when(driverRepository.existsByCpf(command.cpf())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
}
