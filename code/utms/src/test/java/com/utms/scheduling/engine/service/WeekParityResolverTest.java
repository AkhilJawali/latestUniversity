package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.enums.WeekGroup;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link WeekParityResolver} (A4-13, design section 11).
 * Verifies KD-61 / PD-81: first teaching week = WEEK_A, alternation by calendar-week index.
 */
class WeekParityResolverTest {

    private final WeekParityResolver resolver = new WeekParityResolver();

    // Semester starts on a Monday for clarity.
    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 1, 5); // Monday

    @Test
    void resolve_semesterStartWeek_isWeekA() {
        assertEquals(WeekGroup.WEEK_A, resolver.resolve(SEMESTER_START, SEMESTER_START));
    }

    @Test
    void resolve_oneWeekAfterStart_isWeekB() {
        assertEquals(WeekGroup.WEEK_B, resolver.resolve(SEMESTER_START.plusWeeks(1), SEMESTER_START));
    }

    @Test
    void resolve_twoWeeksAfterStart_isWeekA() {
        assertEquals(WeekGroup.WEEK_A, resolver.resolve(SEMESTER_START.plusWeeks(2), SEMESTER_START));
    }

    @Test
    void resolve_midWeekDate_resolvesByItsMonday() {
        // Wednesday of the start week is still WEEK_A.
        LocalDate wednesdayWeek0 = SEMESTER_START.plusDays(2);
        assertEquals(WeekGroup.WEEK_A, resolver.resolve(wednesdayWeek0, SEMESTER_START));
        // Friday of the second week is still WEEK_B.
        LocalDate fridayWeek1 = SEMESTER_START.plusWeeks(1).plusDays(4);
        assertEquals(WeekGroup.WEEK_B, resolver.resolve(fridayWeek1, SEMESTER_START));
    }

    @Test
    void resolve_semesterStartMidWeek_firstWeekStillWeekA() {
        // Semester starts on a Wednesday; the whole of that calendar week is WEEK_A.
        LocalDate startWednesday = LocalDate.of(2026, 1, 7);
        assertEquals(WeekGroup.WEEK_A, resolver.resolve(startWednesday, startWednesday));
        assertEquals(WeekGroup.WEEK_A, resolver.resolve(startWednesday.plusDays(2), startWednesday)); // Fri same wk
        assertEquals(WeekGroup.WEEK_B, resolver.resolve(startWednesday.plusWeeks(1), startWednesday));
    }
}
