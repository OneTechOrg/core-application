package com.rappidrive.application.ports.input.servicearea;

import com.rappidrive.domain.entities.ServiceArea;

import java.util.UUID;

public interface ToggleServiceAreaStatusInputPort {
    ServiceArea deactivate(UUID id);
    ServiceArea activate(UUID id);
}
