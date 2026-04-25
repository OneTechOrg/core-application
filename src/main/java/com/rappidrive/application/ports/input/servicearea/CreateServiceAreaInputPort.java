package com.rappidrive.application.ports.input.servicearea;

import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.TenantId;

public interface CreateServiceAreaInputPort {

    record CreateServiceAreaCommand(TenantId tenantId, String name, String geoJsonPolygon) {}

    ServiceArea execute(CreateServiceAreaCommand command);
}
