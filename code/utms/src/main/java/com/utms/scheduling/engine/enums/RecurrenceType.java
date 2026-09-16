package com.utms.scheduling.engine.enums;

/**
 * Recurrence pattern for a scheduled session (A4-13, KD-60).
 *
 * <p>WEEKLY is the default and preserves the canonical behavior assumed by the
 * generation engine (A4-11): the session occurs every teaching week.
 * FORTNIGHTLY means the session occurs on every other teaching week, on the
 * {@link WeekGroup} it is assigned to.</p>
 */
public enum RecurrenceType {
    WEEKLY,
    FORTNIGHTLY
}
