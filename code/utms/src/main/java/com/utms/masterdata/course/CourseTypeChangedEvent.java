package com.utms.masterdata.course;

/**
 * Domain event emitted when a course's type (CORE, ELECTIVE, AUDIT) is changed.
 */
public record CourseTypeChangedEvent(
        Long courseId,
        String previousType,
        String newType
) {
}
