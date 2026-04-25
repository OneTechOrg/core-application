package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.input.servicearea.GetServiceAreaInputPort;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.exceptions.ServiceAreaNotFoundException;
import com.rappidrive.domain.valueobjects.ServiceAreaId;

import java.util.UUID;

public class GetServiceAreaUseCase implements GetServiceAreaInputPort {

    private final ServiceAreaRepositoryPort serviceAreaRepository;

    public GetServiceAreaUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
    }

    @Override
    public ServiceArea execute(UUID id) {
        return serviceAreaRepository.findById(ServiceAreaId.of(id))
            .orElseThrow(() -> new ServiceAreaNotFoundException(id));
    }
}
