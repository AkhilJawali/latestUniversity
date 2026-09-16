package com.utms.common.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class BusinessRuleViolationException extends RuntimeException {

    private final List<Map<String, Object>> details;

    public BusinessRuleViolationException(String message) {
        super(message);
        this.details = List.of();
    }

    public BusinessRuleViolationException(String message, List<Map<String, Object>> details) {
        super(message);
        this.details = details;
    }
}
