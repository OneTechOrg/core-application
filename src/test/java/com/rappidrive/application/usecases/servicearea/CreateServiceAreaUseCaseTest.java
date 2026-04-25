package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.input.servicearea.CreateServiceAreaInputPort.CreateServiceAreaCommand;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateServiceAreaUseCaseTest {

    private ServiceAreaRepositoryPort repository;
    private CreateServiceAreaUseCase useCase;

    private static final String VALID_GEOJSON =
        "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}";

    @BeforeEach
    void setUp() {
        repository = mock(ServiceAreaRepositoryPort.class);
        useCase = new CreateServiceAreaUseCase(repository);
    }

    @Test
    void execute_shouldCreateAndPersistServiceArea() {
        TenantId tenantId = TenantId.generate();
        CreateServiceAreaCommand command = new CreateServiceAreaCommand(tenantId, "Norte", VALID_GEOJSON);
        when(repository.save(any(ServiceArea.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceArea result = useCase.execute(command);

        assertEquals("Norte", result.getName());
        assertEquals(tenantId, result.getTenantId());
        assertTrue(result.isActive());
        verify(repository).save(any(ServiceArea.class));
    }

    @Test
    void execute_shouldThrow_whenGeoJsonIsInvalid() {
        TenantId tenantId = TenantId.generate();
        CreateServiceAreaCommand command = new CreateServiceAreaCommand(tenantId, "Norte", "not-json");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
        verify(repository, never()).save(any());
    }
}
