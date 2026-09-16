package com.utms.publication.event;

import java.time.Instant;
import java.util.Set;

/**
 * Published (within the publish transaction; consumed AFTER_COMMIT) when a timetable draft
 * is published (A4-21, FR-4 / KD-A21-1). This is the integration contract for the
 * notification engine (A4-37) and the calendar-feed service (A4-39), neither of which
 * exists yet. A4-21 identifies the recipients; the consumers perform delivery / feed
 * regeneration. Students are addressed at batch/section granularity (PD-109) — there is no
 * per-student entity yet.
 */
public record TimetablePublishedEvent(
        Long draftId,
        Long departmentId,
        String semester,
        String academicYear,
        String publishedByUserId,
        Instant publishedAt,
        Set<Long> affectedFacultyIds,
        Set<Long> affectedBatchIds,
        Set<Long> affectedSectionIds
) {
}
