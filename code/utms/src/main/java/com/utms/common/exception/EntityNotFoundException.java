package com.utms.common.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityType, Long id) {
        super(entityType + " with id " + id + " not found");
    }

    public EntityNotFoundException(String entityType, String field, String value) {
        super(entityType + " with " + field + " '" + value + "' not found");
    }
}
