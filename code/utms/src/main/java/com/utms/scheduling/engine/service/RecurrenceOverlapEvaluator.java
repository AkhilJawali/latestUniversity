package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.enums.RecurrenceType;
import com.utms.scheduling.engine.enums.WeekGroup;
import org.springframework.stereotype.Component;

/**
 * Owns the alternate-week non-conflict rule (A4-13, KD-63; constraints HC-FN-1 / HC-FN-2).
 *
 * <p>The rule is pure week-group algebra decided from the two recurrence patterns alone —
 * it needs no calendar and no enumerated dates, so it is O(1). Conflict detection (A4-16)
 * calls {@link #everCoOccur} as a gate before running any slot/resource comparison: if two
 * sessions can never occur in the same week, there is no clash to report.</p>
 */
@Component
public class RecurrenceOverlapEvaluator {

    /**
     * Immutable recurrence pattern of a session: its type and (for fortnightly) week group.
     * {@code weekGroup} is {@code null} when {@code type} is WEEKLY.
     */
    public record SessionRecurrence(RecurrenceType type, WeekGroup weekGroup) {
        public static SessionRecurrence weekly() {
            return new SessionRecurrence(RecurrenceType.WEEKLY, null);
        }

        public static SessionRecurrence fortnightly(WeekGroup group) {
            return new SessionRecurrence(RecurrenceType.FORTNIGHTLY, group);
        }
    }

    /**
     * Returns whether two sessions can ever occur in the same teaching week.
     *
     * <ul>
     *   <li>If either session is WEEKLY, it occurs every week, so the two always co-occur
     *       (HC-FN-2 — e.g. weekly vs. fortnightly overlaps).</li>
     *   <li>If both are FORTNIGHTLY, they co-occur only when they share the same week group;
     *       opposite groups never overlap (HC-FN-1 — the alternate-week non-conflict case).</li>
     * </ul>
     *
     * @return true if the sessions' occurring weeks overlap in at least one teaching week
     */
    public boolean everCoOccur(SessionRecurrence a, SessionRecurrence b) {
        if (a.type() == RecurrenceType.WEEKLY || b.type() == RecurrenceType.WEEKLY) {
            return true;
        }
        // Both FORTNIGHTLY: same group co-occurs, opposite groups never do.
        return a.weekGroup() == b.weekGroup();
    }
}
