package com.rappidrive.presentation.mappers;

import com.rappidrive.application.ports.input.servicearea.CreateServiceAreaInputPort.CreateServiceAreaCommand;
import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.TenantId;
import com.rappidrive.presentation.dto.request.CreateServiceAreaRequest;
import com.rappidrive.presentation.dto.response.ServiceAreaResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceAreaDtoMapper {

    public ServiceAreaResponse toResponse(ServiceArea serviceArea) {
        return new ServiceAreaResponse(
            serviceArea.getId().getValue(),
            serviceArea.getTenantId().getValue(),
            serviceArea.getName(),
            serviceArea.getGeoJsonPolygon(),
            serviceArea.isActive(),
            serviceArea.getCreatedAt(),
            serviceArea.getUpdatedAt()
        );
    }

    public List<ServiceAreaResponse> toResponseList(List<ServiceArea> serviceAreas) {
        return serviceAreas.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public CreateServiceAreaCommand toCommand(CreateServiceAreaRequest request) {
        return new CreateServiceAreaCommand(
            new TenantId(request.tenantId()),
            request.name(),
            request.geoJsonPolygon()
        );
    }
}
