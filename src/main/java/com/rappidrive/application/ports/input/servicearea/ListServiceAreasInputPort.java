package com.rappidrive.application.ports.input.servicearea;

import com.rappidrive.domain.entities.ServiceArea;
import com.rappidrive.domain.valueobjects.TenantId;

import java.util.List;

public interface ListServiceAreasInputPort {

    record ListServiceAreasQuery(TenantId tenantId, boolean activeOnly) {}

    List<ServiceArea> execute(ListServiceAreasQuery query);
}
