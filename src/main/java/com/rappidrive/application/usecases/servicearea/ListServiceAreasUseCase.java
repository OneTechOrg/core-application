package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.input.servicearea.ListServiceAreasInputPort;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;

import java.util.List;

public class ListServiceAreasUseCase implements ListServiceAreasInputPort {

    private final ServiceAreaRepositoryPort serviceAreaRepository;

    public ListServiceAreasUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
    }

    @Override
    public List<ServiceArea> execute(ListServiceAreasQuery query) {
        if (query.activeOnly()) {
            return serviceAreaRepository.findActiveByTenantId(query.tenantId());
        }
        return serviceAreaRepository.findByTenantId(query.tenantId());
    }
}
