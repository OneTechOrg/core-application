package com.rappidrive.application.usecases.passenger;

import com.rappidrive.application.ports.output.CurrentUserPort;
import com.rappidrive.application.ports.output.PassengerRepositoryPort;
import com.rappidrive.domain.entities.Passenger;
import com.rappidrive.domain.exceptions.PassengerNotFoundException;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentPassengerUseCaseTest {

    @Mock
    private PassengerRepositoryPort passengerRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private GetCurrentPassengerUseCase useCase;
    private TenantId tenantId;
    private UUID keycloakId;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentPassengerUseCase(passengerRepository, currentUserPort);
        tenantId = TenantId.generate();
        keycloakId = UUID.randomUUID();
    }

    @Test
    void execute_WhenPassengerExists_ShouldReturnPassenger() {
        // Given
        CurrentUserPort.CurrentUser currentUser = new CurrentUserPort.CurrentUser(
            keycloakId, "username", "email@test.com", java.util.List.of("PASSENGER"), java.util.List.of()
        );
        when(currentUserPort.getCurrentUser()).thenReturn(Optional.of(currentUser));
        
        Passenger passenger = mock(Passenger.class);
        when(passengerRepository.findByKeycloakId(keycloakId.toString(), tenantId)).thenReturn(Optional.of(passenger));

        // When
        Passenger result = useCase.execute(tenantId);

        // Then
        assertThat(result).isEqualTo(passenger);
    }

    @Test
    void execute_WhenNoAuthenticatedUser_ShouldThrowException() {
        // Given
        when(currentUserPort.getCurrentUser()).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.execute(tenantId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No authenticated user found");
    }

    @Test
    void execute_WhenPassengerProfileNotFound_ShouldThrowException() {
        // Given
        CurrentUserPort.CurrentUser currentUser = new CurrentUserPort.CurrentUser(
            keycloakId, "username", "email@test.com", java.util.List.of("PASSENGER"), java.util.List.of()
        );
        when(currentUserPort.getCurrentUser()).thenReturn(Optional.of(currentUser));
        when(passengerRepository.findByKeycloakId(keycloakId.toString(), tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.execute(tenantId))
            .isInstanceOf(PassengerNotFoundException.class)
            .hasMessageContaining("Passenger profile not found");
    }
}
