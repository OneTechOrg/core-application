package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.exceptions.ServiceAreaNotFoundException;
import com.rappidrive.domain.valueobjects.ServiceAreaId;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ToggleServiceAreaStatusUseCaseTest {

    private ServiceAreaRepositoryPort repository;
    private ToggleServiceAreaStatusUseCase useCase;

    private static final String VALID_GEOJSON =
        "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}";

    @BeforeEach
    void setUp() {
        repository = mock(ServiceAreaRepositoryPort.class);
        useCase = new ToggleServiceAreaStatusUseCase(repository);
    }

    @Test
    void deactivate_shouldDeactivateActiveArea() {
        ServiceArea area = ServiceArea.create(TenantId.generate(), "Sul", VALID_GEOJSON);
        UUID id = area.getId().getValue();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.of(area));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceArea result = useCase.deactivate(id);

        assertFalse(result.isActive());
        verify(repository).save(any(ServiceArea.class));
    }

    @Test
    void deactivate_shouldThrow_whenAlreadyInactive() {
        ServiceArea area = ServiceArea.create(TenantId.generate(), "Sul", VALID_GEOJSON).deactivate();
        UUID id = area.getId().getValue();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.of(area));

        assertThrows(IllegalStateException.class, () -> useCase.deactivate(id));
        verify(repository, never()).save(any());
    }

    @Test
    void activate_shouldActivateInactiveArea() {
        ServiceArea area = ServiceArea.create(TenantId.generate(), "Leste", VALID_GEOJSON).deactivate();
        UUID id = area.getId().getValue();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.of(area));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceArea result = useCase.activate(id);

        assertTrue(result.isActive());
        verify(repository).save(any(ServiceArea.class));
    }

    @Test
    void activate_shouldThrow_whenAlreadyActive() {
        ServiceArea area = ServiceArea.create(TenantId.generate(), "Oeste", VALID_GEOJSON);
        UUID id = area.getId().getValue();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.of(area));

        assertThrows(IllegalStateException.class, () -> useCase.activate(id));
        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.empty());

        assertThrows(ServiceAreaNotFoundException.class, () -> useCase.deactivate(id));
    }

    @Test
    void activate_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.empty());

        assertThrows(ServiceAreaNotFoundException.class, () -> useCase.activate(id));
    }
}
