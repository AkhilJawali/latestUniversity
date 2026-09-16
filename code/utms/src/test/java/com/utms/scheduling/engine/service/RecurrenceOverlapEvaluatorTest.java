package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.enums.WeekGroup;
import com.utms.scheduling.engine.service.RecurrenceOverlapEvaluator.SessionRecurrence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RecurrenceOverlapEvaluator} (A4-13, design section 11).
 * Verifies the alternate-week non-conflict rule: HC-FN-1 and HC-FN-2.
 */
class RecurrenceOverlapEvaluatorTest {

    private final RecurrenceOverlapEvaluator evaluator = new RecurrenceOverlapEvaluator();

    @Test
    void everCoOccur_bothWeekly_true() {
        assertTrue(evaluator.everCoOccur(SessionRecurrence.weekly(), SessionRecurrence.weekly()));
    }

    @Test
    void everCoOccur_weeklyVsFortnightly_true_HCFN2() {
        assertTrue(evaluator.everCoOccur(
                SessionRecurrence.weekly(),
                SessionRecurrence.fortnightly(WeekGroup.WEEK_A)));
        assertTrue(evaluator.everCoOccur(
                SessionRecurrence.fortnightly(WeekGroup.WEEK_B),
                SessionRecurrence.weekly()));
    }

    @Test
    void everCoOccur_fortnightlyOppositeGroups_false_HCFN1() {
        assertFalse(evaluator.everCoOccur(
                SessionRecurrence.fortnightly(WeekGroup.WEEK_A),
                SessionRecurrence.fortnightly(WeekGroup.WEEK_B)));
    }

    @Test
    void everCoOccur_fortnightlySameGroup_true_HCFN2() {
        assertTrue(evaluator.everCoOccur(
                SessionRecurrence.fortnightly(WeekGroup.WEEK_A),
                SessionRecurrence.fortnightly(WeekGroup.WEEK_A)));
    }
}
