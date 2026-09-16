package com.utms.scheduling.engine.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SoftConstraintViolationDto {
    private final Long id;
    private final String constraintType;
    private final String affectedEntityType;
    private final Long affectedEntityId;
    private final String description;
    private final String relaxationReason;
}
