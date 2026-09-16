package com.utms.masterdata.faculty;

/**
 * Domain event emitted when a faculty member's designation is changed.
 * Downstream consumers may need to recalculate workload norms per cadre.
 */
public record FacultyDesignationChangedEvent(
        Long facultyId,
        String previousDesignation,
        String newDesignation
) {
}
