package com.utms.masterdata.timeslot;

/**
 * Days of the week used for day-specific slot overrides.
 * When a slot's applicable_day is null, it applies to ALL days.
 * When set, it is a day-specific override (KD-44).
 */
public enum DayOfWeekEnum {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
