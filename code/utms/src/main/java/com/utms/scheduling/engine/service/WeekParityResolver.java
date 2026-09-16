package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.enums.WeekGroup;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Resolves which {@link WeekGroup} a given teaching date belongs to (A4-13, KD-61, PD-81).
 *
 * <p>Parity is a pure function of the date and the semester start anchor. The week
 * index is the number of whole weeks between the Monday of the semester start and the
 * Monday of the target date; an even index is {@link WeekGroup#WEEK_A}, an odd index is
 * {@link WeekGroup#WEEK_B}. The first teaching week is therefore WEEK_A (PD-81).</p>
 *
 * <p>Using the calendar-week index (not a count of teaching days) means a holiday that
 * removes a single occurrence does not renumber subsequent weeks, so the A/B alternation
 * between paired lab groups is preserved.</p>
 */
@Component
public class WeekParityResolver {

    /**
     * Resolves the week group for {@code date} relative to {@code semesterStart}.
     *
     * @param date          any date within the semester (any day of week)
     * @param semesterStart the semester start date (anchor), from the academic calendar
     * @return WEEK_A for an even week index from the anchor, WEEK_B for odd
     */
    public WeekGroup resolve(LocalDate date, LocalDate semesterStart) {
        LocalDate anchorMonday = semesterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate dateMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weekIndex = ChronoUnit.WEEKS.between(anchorMonday, dateMonday);
        // Math.floorMod keeps parity correct for dates before the anchor (negative index).
        return Math.floorMod(weekIndex, 2) == 0 ? WeekGroup.WEEK_A : WeekGroup.WEEK_B;
    }
}
