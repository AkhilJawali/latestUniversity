package com.utms.scheduling.engine.model;

import com.utms.scheduling.engine.entity.SessionDerivationRule;
import com.utms.scheduling.engine.enums.SoftConstraintType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Complete input data for a scheduling generation run.
 * Loaded by SchedulingDataLoader.loadAll() (FR-2.1 through FR-2.7 + PD-70/74).
 * All collections sorted by ID for determinism (KD-50).
 */
@Getter
@Builder
public class SchedulingInput {
    private final Long departmentId;
    private final Long campusId;
    private final String semester;

    // FR-2.1: Courses with faculty assignments and L-T-P
    private final List<CourseAssignment> courses;

    // FR-2.2: Faculty workload limits (from A4-32 + A4-4 override)
    private final List<FacultyWorkloadLimits> facultyLimits;

    // FR-2.3: Eligible rooms (capacity, equipment, building/floor)
    private final List<RoomInfo> rooms;

    // FR-2.4: Time-slot grid (teaching slots with durations)
    private final List<SlotInfo> slotGrid;

    // FR-2.5: Working days (days the engine can place sessions)
    private final List<String> workingDays;

    // FR-2.6: Active resource blocks (hard and soft)
    private final List<ActiveBlock> activeBlocks;

    // FR-2.7: Institution common slots (CCC/UWE — KD-52)
    private final List<CommonSlotInfo> commonSlots;

    // PD-74: Session derivation rules per campus
    private final List<SessionDerivationRule> derivationRules;

    // PD-70: Soft constraint weight configuration
    private final Map<SoftConstraintType, Double> softConstraintWeights;

    /**
     * Course code for a course id (used when persisting unplaced sessions).
     * Falls back to "COURSE-{id}" if the course is not in the loaded set.
     */
    public String getCourseCode(Long courseId) {
        return courses.stream()
                .filter(c -> c.getCourseId().equals(courseId))
                .map(CourseAssignment::getCourseCode)
                .findFirst()
                .orElse("COURSE-" + courseId);
    }

    /**
     * Human-readable course name for a course id.
     * TODO: course/faculty/batch names are not yet loaded into SchedulingInput
     *       (CourseAssignment carries only courseCode). Until the master-data
     *       wiring loads display names, we fall back to the code/id so the
     *       unplaced-session report remains populated and stable.
     */
    public String getCourseName(Long courseId) {
        return getCourseCode(courseId);
    }

    /**
     * Human-readable faculty name for a faculty id. See getCourseName TODO.
     */
    public String getFacultyName(Long facultyId) {
        return "FACULTY-" + facultyId;
    }

    /**
     * Human-readable batch name for a batch id. See getCourseName TODO.
     */
    public String getBatchName(Long batchId) {
        return "BATCH-" + batchId;
    }
}
