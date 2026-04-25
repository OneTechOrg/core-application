package com.rappidrive.domain.exceptions;

import java.util.UUID;

public class ServiceAreaNotFoundException extends DomainException {

    public ServiceAreaNotFoundException(UUID id) {
        super(String.format("Service area not found with ID: %s", id));
    }
}
