package com.rappidrive.application.usecases.passenger;

import com.rappidrive.application.ports.input.passenger.CreatePassengerInputPort;
import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.valueobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePassengerUseCaseTest {

    @Mock
    private PassengerRepositoryPort passengerRepository;

    private CreatePassengerUseCase useCase;
    
    private CreatePassengerInputPort.CreatePassengerCommand command;

    @BeforeEach
    void setUp() {
        useCase = new CreatePassengerUseCase(passengerRepository);
        
        command = new CreatePassengerInputPort.CreatePassengerCommand(
            TenantId.generate(),
            UUID.randomUUID().toString(),
            "Jane Doe",
            new Email("jane@example.com"),
            new Phone("+5511888888888")
        );
    }

    @Test
    void execute_WhenValidCommand_ShouldCreateAndSavePassenger() {
        // Given
        when(passengerRepository.existsByEmail(command.email())).thenReturn(false);
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Passenger result = useCase.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(command.keycloakId());
        assertThat(result.getEmail()).isEqualTo(command.email());
        
        verify(passengerRepository).save(any(Passenger.class));
    }

    @Test
    void execute_WhenEmailAlreadyExists_ShouldThrowException() {
        // Given
        when(passengerRepository.existsByEmail(command.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }
}
