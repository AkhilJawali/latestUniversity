package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.enums.SoftConstraintType;

/**
 * A soft constraint violation identified during optimization.
 * Stored in soft_constraint_violations table after generation.
 */
public record SoftViolation(
    SoftConstraintType type,
    String entityType,
    Long entityId,
    String description,
    String reason
) {}
