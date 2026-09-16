package com.utms.scheduling.engine.model;

import com.utms.scheduling.engine.entity.ScheduledSession;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Selector for partial re-generation (A4-14, KD-62 / PD-84).
 * A session is "selected" (movable/free) if it matches ANY of the id-sets AND is
 * neither locked nor approved (fixed always wins — that precedence is applied by the
 * caller, not here). An empty scope selects nothing.
 */
@Getter
@Builder
public class RegenerationScope {

    private final List<Long> batchIds;
    private final List<Long> sectionIds;
    private final List<Long> courseIds;

    /** True when no id-set names anything to regenerate. */
    public boolean isEmpty() {
        return isBlank(batchIds) && isBlank(sectionIds) && isBlank(courseIds);
    }

    /**
     * Whether this session falls within the requested scope. Locked/approved
     * precedence (fixed beats selected) is enforced by the partitioning caller.
     */
    public boolean matches(ScheduledSession session) {
        return contains(batchIds, session.getBatchId())
            || contains(sectionIds, session.getSectionId())
            || contains(courseIds, session.getCourseId());
    }

    private static boolean isBlank(List<Long> ids) {
        return ids == null || ids.isEmpty();
    }

    private static boolean contains(List<Long> ids, Long id) {
        return id != null && ids != null && ids.contains(id);
    }
}
