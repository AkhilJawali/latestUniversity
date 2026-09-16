package com.utms.scheduling.engine.enums;

/**
 * Status of a generation request.
 */
public enum GenerationStatus {
    IN_PROGRESS,
    COMPLETED,
    TIMED_OUT,
    INFEASIBLE,
    FAILED,
    CANCELLED
}
