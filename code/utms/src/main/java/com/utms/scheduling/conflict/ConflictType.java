package com.utms.scheduling.conflict;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Conflict types for real-time conflict detection (A4-16).
 * 
 * Types marked INTERNAL arise from constraints within the draft itself
 * (e.g., faculty double-booking). Types marked CROSS_DRAFT arise from
 * clashes with other drafts (e.g., cross-draft room double-booking).
 * 
 * KD-64: All conflict types are recurrence-aware.
 */
@Getter
@RequiredArgsConstructor
public enum ConflictType {

    // Internal constraints (within draft)
    FACULTY_DOUBLE_BOOKING("Faculty double-booking", ConflictScope.INTERNAL),
    ROOM_DOUBLE_BOOKING("Room double-booking", ConflictScope.INTERNAL),
    BATCH_CLASH("Batch clash", ConflictScope.INTERNAL),

    // Capacity constraints
    ROOM_CAPACITY_EXCEEDED("Room capacity exceeded", ConflictScope.INTERNAL),

    // Faculty workload constraints
    FACULTY_DAILY_HOURS_EXCEEDED("Faculty daily hours exceeded", ConflictScope.INTERNAL),
    FACULTY_WEEKLY_HOURS_EXCEEDED("Faculty weekly hours exceeded", ConflictScope.INTERNAL),
    FACULTY_CONSECUTIVE_HOURS_EXCEEDED("Faculty consecutive hours exceeded", ConflictScope.INTERNAL),

    // Hard block constraints
    ROOM_HARD_BLOCK("Room hard block", ConflictScope.INTERNAL),
    FACULTY_HARD_BLOCK("Faculty hard block", ConflictScope.INTERNAL),

    // Travel time constraint (KD-63: DEFINED but not detected yet - PD-98)
    TRAVEL_TIME_VIOLATION("Travel time violation", ConflictScope.INTERNAL),

    // Prerequisite sequence (KD-63: DEFINED but not detected yet - PD-96)
    PREREQUISITE_SEQUENCE_VIOLATION("Prerequisite sequence violation", ConflictScope.INTERNAL),

    // Cross-draft conflicts
    CROSS_DRAFT_FACULTY_DOUBLE_BOOKING("Cross-draft faculty double-booking", ConflictScope.CROSS_DRAFT),
    CROSS_DRAFT_ROOM_DOUBLE_BOOKING("Cross-draft room double-booking", ConflictScope.CROSS_DRAFT);

    private final String label;
    private final ConflictScope scope;

    public boolean isInternal() {
        return scope == ConflictScope.INTERNAL;
    }

    public boolean isCrossDraft() {
        return scope == ConflictScope.CROSS_DRAFT;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ConflictScope {
        INTERNAL("Internal"),
        CROSS_DRAFT("Cross-draft");

        private final String label;
    }
}
