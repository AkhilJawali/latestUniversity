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
 *   <li>Actively detected (reuse A4-11 rule predicates): faculty double-booking,
 *       room double-booking, batch clash, room capacity, and — when faculty limits
 *       are supplied — daily/weekly/consecutive hours.</li>
 *   <li>{@link #ROOM_HARD_BLOCK} — value defined; detection DEFERRED. The engine's
 *       {@code CSPState.isRoomHardBlocked} rule is not yet invoked from the real-time
 *       checker.</li>
 *   <li>{@link #FACULTY_HARD_BLOCK} — value defined; detection DEFERRED (PD-99). Not
 *       invoked from the checker; the engine's faculty hard-availability check is a
 *       stub.</li>
 *   <li>{@link #TRAVEL_TIME} — value defined; detection DEFERRED (PD-98, no
 *       travel-time/distance data model exists).</li>
 *   <li>{@link #PREREQUISITE_SEQUENCE} — value defined; detection DEFERRED (PD-96,
 *       pending stakeholder confirmation of the interpretation).</li>
 * </ul>
 * The deferred values exist so consumers have a stable contract for when detection
 * lands.
 */
public enum ConflictType {
    FACULTY_DOUBLE_BOOKING,
    ROOM_DOUBLE_BOOKING,
    BATCH_CLASH,
    ROOM_CAPACITY,
    FACULTY_DAILY_HOURS,
    FACULTY_WEEKLY_HOURS,
    FACULTY_CONSECUTIVE_HOURS,
    ROOM_HARD_BLOCK,
    FACULTY_HARD_BLOCK,
    TRAVEL_TIME,
    PREREQUISITE_SEQUENCE
}
