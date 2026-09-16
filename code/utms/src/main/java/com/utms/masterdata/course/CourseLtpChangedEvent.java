package com.utms.masterdata.course;

/**
 * Domain event emitted when a course's L-T-P (Lecture-Tutorial-Practical) hours are modified.
 */
public record CourseLtpChangedEvent(
        Long courseId,
        Integer oldLectureHours,
        Integer oldTutorialHours,
        Integer oldPracticalHours,
        Integer newLectureHours,
        Integer newTutorialHours,
        Integer newPracticalHours
) {
}
