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
import static org.mockito.Mockito.*;

class GetServiceAreaUseCaseTest {

    private ServiceAreaRepositoryPort repository;
    private GetServiceAreaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceAreaRepositoryPort.class);
        useCase = new GetServiceAreaUseCase(repository);
    }

    @Test
    void execute_shouldReturnServiceArea_whenFound() {
        ServiceArea area = buildServiceArea();
        UUID id = area.getId().getValue();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.of(area));

        ServiceArea result = useCase.execute(id);

        assertEquals(area, result);
        verify(repository).findById(ServiceAreaId.of(id));
    }

    @Test
    void execute_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(ServiceAreaId.of(id))).thenReturn(Optional.empty());

        assertThrows(ServiceAreaNotFoundException.class, () -> useCase.execute(id));
    }

    private ServiceArea buildServiceArea() {
        return ServiceArea.create(TenantId.generate(), "Centro",
            "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}");
    }
}
