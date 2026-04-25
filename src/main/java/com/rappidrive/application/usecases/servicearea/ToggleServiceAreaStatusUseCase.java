package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.input.servicearea.ToggleServiceAreaStatusInputPort;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.exceptions.ServiceAreaNotFoundException;
import com.rappidrive.domain.valueobjects.ServiceAreaId;

import java.util.UUID;

public class ToggleServiceAreaStatusUseCase implements ToggleServiceAreaStatusInputPort {

    private final ServiceAreaRepositoryPort serviceAreaRepository;

    public ToggleServiceAreaStatusUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
    }

    @Override
    public ServiceArea deactivate(UUID id) {
        ServiceArea area = findOrThrow(id);
        ServiceArea deactivated = area.deactivate();
        return serviceAreaRepository.save(deactivated);
    }

    @Override
    public ServiceArea activate(UUID id) {
        ServiceArea area = findOrThrow(id);
        ServiceArea activated = area.activate();
        return serviceAreaRepository.save(activated);
    }

    private ServiceArea findOrThrow(UUID id) {
        return serviceAreaRepository.findById(ServiceAreaId.of(id))
            .orElseThrow(() -> new ServiceAreaNotFoundException(id));
    }
}
