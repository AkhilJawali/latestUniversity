package com.utms.scheduling.engine.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * A course-faculty-batch assignment loaded for scheduling.
 * Represents one "thing to schedule" before session derivation.
 */
@Getter
@Builder
public class CourseAssignment {
    private final Long courseId;
    private final String courseCode;
    private final Long facultyId;
    private final Long batchId;
    private final Long sectionId;
    private final int lectureHours;
    private final int tutorialHours;
    private final int practicalHours;
    private final int batchStrength;
    private final List<String> equipmentTags;
}
