package com.rappidrive.application.usecases.driver;

import com.rappidrive.application.ports.output.CurrentUserPort;
import com.rappidrive.application.ports.output.DriverRepositoryPort;
import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.exceptions.DriverNotFoundException;
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
class GetCurrentDriverUseCaseTest {

    @Mock
    private DriverRepositoryPort driverRepository;

    @Mock
    private CurrentUserPort currentUserPort;

    private GetCurrentDriverUseCase useCase;
    private TenantId tenantId;
    private UUID keycloakId;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentDriverUseCase(driverRepository, currentUserPort);
        tenantId = TenantId.generate();
        keycloakId = UUID.randomUUID();
    }

    @Test
    void execute_WhenDriverExists_ShouldReturnDriver() {
        // Given
        CurrentUserPort.CurrentUser currentUser = new CurrentUserPort.CurrentUser(
            keycloakId, "username", "email@test.com", java.util.List.of("DRIVER"), java.util.List.of()
        );
        when(currentUserPort.getCurrentUser()).thenReturn(Optional.of(currentUser));
        
        Driver driver = mock(Driver.class);
        when(driverRepository.findByKeycloakId(keycloakId.toString(), tenantId)).thenReturn(Optional.of(driver));

        // When
        Driver result = useCase.execute(tenantId);

        // Then
        assertThat(result).isEqualTo(driver);
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
    void execute_WhenDriverProfileNotFound_ShouldThrowException() {
        // Given
        CurrentUserPort.CurrentUser currentUser = new CurrentUserPort.CurrentUser(
            keycloakId, "username", "email@test.com", java.util.List.of("DRIVER"), java.util.List.of()
        );
        when(currentUserPort.getCurrentUser()).thenReturn(Optional.of(currentUser));
        when(driverRepository.findByKeycloakId(keycloakId.toString(), tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.execute(tenantId))
            .isInstanceOf(DriverNotFoundException.class)
            .hasMessageContaining("Driver profile not found");
    }
}
