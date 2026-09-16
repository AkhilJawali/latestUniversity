package com.utms.scheduling.engine.enums;

/**
 * Types of soft constraints optimized by the scheduling engine.
 * Maps to SC-ENG-1 through SC-ENG-6 in the design.
 */
public enum SoftConstraintType {
    FACULTY_TIME_PREFERENCE,
    FACULTY_DISTRIBUTION_PREFERENCE,
    ROOM_PROXIMITY,
    GAP_MINIMIZATION,
    SOFT_BLOCK_OVERRIDE,
    DAY_PATTERN_BALANCE
}
