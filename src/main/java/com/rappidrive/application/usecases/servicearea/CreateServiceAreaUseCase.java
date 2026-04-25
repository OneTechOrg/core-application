package com.rappidrive.application.usecases.servicearea;

import com.rappidrive.application.ports.input.servicearea.CreateServiceAreaInputPort;
import com.rappidrive.application.ports.output.ServiceAreaRepositoryPort;
import com.rappidrive.domain.entities.ServiceArea;

public class CreateServiceAreaUseCase implements CreateServiceAreaInputPort {

    private final ServiceAreaRepositoryPort serviceAreaRepository;

    public CreateServiceAreaUseCase(ServiceAreaRepositoryPort serviceAreaRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
    }

    @Override
    public ServiceArea execute(CreateServiceAreaCommand command) {
        ServiceArea serviceArea = ServiceArea.create(
            command.tenantId(),
            command.name(),
            command.geoJsonPolygon()
        );
        return serviceAreaRepository.save(serviceArea);
    }
}
