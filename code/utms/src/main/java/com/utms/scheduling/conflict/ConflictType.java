package com.utms.scheduling.conflict;

/**
 * The canonical catalogue of real-time conflict types (A4-16, design KD-63).
 *
 * <p>This enum is OWNED by the real-time conflict detection engine and consumed by
 * the drag-and-drop editor (A4-15). It is the machine-readable contract for the
 * conflicts surfaced during interactive editing.
 *
 * <p>Detection status per the approved design:
 * <ul>
 *   <li>Actively detected: FACULTY_DOUBLE_BOOKING, ROOM_DOUBLE_BOOKING, BATCH_CLASH,
 *       ROOM_CAPACITY, FACULTY_DAILY_HOURS, FACULTY_WEEKLY_HOURS, FACULTY_CONSECUTIVE_HOURS.</li>
 *   <li>ROOM_HARD_BLOCK — value defined; detection DEFERRED (engine stub not invoked).</li>
 *   <li>FACULTY_HARD_BLOCK — value defined; detection DEFERRED (PD-99, faculty hard-availability stub).</li>
 *   <li>TRAVEL_TIME — value defined; detection DEFERRED (PD-98, no travel-time/distance model).</li>
 *   <li>PREREQUISITE_SEQUENCE — value defined; detection DEFERRED (PD-96, pending stakeholder confirmation).</li>
 * </ul>
 * The deferred values exist so consumers have a stable contract for when detection lands.
 */
public enum ConflictType {

    /** HC-ENG-1: Faculty is double-booked in the same time slot. */
    FACULTY_DOUBLE_BOOKING,

    /** HC-ENG-2: Room is double-booked in the same time slot. */
    ROOM_DOUBLE_BOOKING,

    /** HC-ENG-3: Batch/section has overlapping sessions. */
    BATCH_CLASH,

    /** HC-ENG-4: Room capacity is less than batch strength. */
    ROOM_CAPACITY,

    /** HC-ENG-11: Faculty daily teaching hours exceed maximum. */
    FACULTY_DAILY_HOURS,

    /** HC-ENG-11: Faculty weekly teaching hours exceed maximum. */
    FACULTY_WEEKLY_HOURS,

    /** HC-ENG-12: Faculty consecutive teaching hours exceed maximum. */
    FACULTY_CONSECUTIVE_HOURS,

    /** Room has a hard block during this time (DEFERRED detection). */
    ROOM_HARD_BLOCK,

    /** Faculty has declared hard unavailability (DEFERRED detection, PD-99). */
    FACULTY_HARD_BLOCK,

    /** Travel-time buffer violated between campuses (DEFERRED detection, PD-98). */
    TRAVEL_TIME,

    /** Prerequisite course sequencing violated (DEFERRED detection, PD-96). */
    PREREQUISITE_SEQUENCE
}
