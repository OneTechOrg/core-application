package com.rappidrive.domain.exceptions;

/**
 * Exception thrown when an entity already exists in the system.
 * Maps to HTTP 409 Conflict.
 */
public class EntityAlreadyExistsException extends DomainException {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
