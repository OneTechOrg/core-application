package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.input.servicearea.ListServiceAreasInputPort.ListServiceAreasQuery;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListServiceAreasUseCaseTest {

    private ServiceAreaRepositoryPort repository;
    private ListServiceAreasUseCase useCase;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceAreaRepositoryPort.class);
        useCase = new ListServiceAreasUseCase(repository);
        tenantId = TenantId.generate();
    }

    @Test
    void execute_withActiveOnlyFalse_delegatesToFindByTenantId() {
        ServiceArea active = buildArea(true);
        ServiceArea inactive = buildArea(false);
        when(repository.findByTenantId(tenantId)).thenReturn(List.of(active, inactive));

        List<ServiceArea> result = useCase.execute(new ListServiceAreasQuery(tenantId, false));

        assertEquals(2, result.size());
        verify(repository).findByTenantId(tenantId);
        verify(repository, never()).findActiveByTenantId(any());
    }

    @Test
    void execute_withActiveOnlyTrue_delegatesToFindActiveByTenantId() {
        ServiceArea active = buildArea(true);
        when(repository.findActiveByTenantId(tenantId)).thenReturn(List.of(active));

        List<ServiceArea> result = useCase.execute(new ListServiceAreasQuery(tenantId, true));

        assertEquals(1, result.size());
        verify(repository).findActiveByTenantId(tenantId);
        verify(repository, never()).findByTenantId(any());
    }

    private ServiceArea buildArea(boolean active) {
        ServiceArea area = ServiceArea.create(tenantId, "Zona",
            "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}");
        return active ? area : area.deactivate();
    }
}
