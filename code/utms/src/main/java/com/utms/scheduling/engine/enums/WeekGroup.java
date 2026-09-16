package com.utms.scheduling.engine.enums;

/**
 * The alternating-week group a fortnightly session occupies (A4-13, PD-83).
 *
 * <p>Two disjoint groups cover alternating teaching weeks. Week parity is
 * anchored to the semester start date (PD-81): the first teaching week is
 * {@link #WEEK_A}, the next {@link #WEEK_B}, and so on. A session is assigned
 * a week group only when its {@link RecurrenceType} is FORTNIGHTLY; it is
 * {@code null} for WEEKLY sessions.</p>
 */
public enum WeekGroup {
    WEEK_A,
    WEEK_B
}
