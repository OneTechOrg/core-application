package com.rappidrive.application.ports.input.servicearea;

import com.rappidrive.domain.entities.ServiceArea;

import java.util.UUID;

public interface GetServiceAreaInputPort {
    ServiceArea execute(UUID id);
}
