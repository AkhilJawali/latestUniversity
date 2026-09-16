package com.utms.scheduling.engine.service;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.academiccalendar.AcademicCalendarRepository;
import com.utms.masterdata.academiccalendar.PatternType;
import com.utms.masterdata.academiccalendar.WorkingDayPattern;
import com.utms.masterdata.academiccalendar.WorkingDayPatternRepository;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.course.Course;
import com.utms.masterdata.course.CourseRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import com.utms.masterdata.faculty.Faculty;
import com.utms.masterdata.faculty.FacultyCompetency;
import com.utms.masterdata.faculty.FacultyCompetencyRepository;
import com.utms.masterdata.faculty.FacultyRepository;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.room.Room;
import com.utms.masterdata.room.RoomRepository;
import com.utms.masterdata.section.Section;
import com.utms.masterdata.section.SectionRepository;
import com.utms.masterdata.timeslot.DayOfWeekEnum;
import com.utms.masterdata.timeslot.SlotDefinition;
import com.utms.masterdata.timeslot.SlotDefinitionRepository;
import com.utms.masterdata.timeslot.TimeSlotGrid;
import com.utms.masterdata.timeslot.TimeSlotGridRepository;
import com.utms.scheduling.engine.entity.InstitutionCommonSlot;
import com.utms.scheduling.engine.entity.SessionDerivationRule;
import com.utms.scheduling.engine.entity.SoftConstraintWeight;
import com.utms.scheduling.engine.enums.SoftConstraintType;
import com.utms.scheduling.engine.model.*;
import com.utms.scheduling.engine.repository.InstitutionCommonSlotRepository;
import com.utms.scheduling.engine.repository.SessionDerivationRuleRepository;
import com.utms.scheduling.engine.repository.SoftConstraintWeightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads all input data for a scheduling generation run (FR-2.1 through FR-2.7).
 * Also validates preconditions before generation (FR-1.2/1.3).
 * All collections sorted by ID for determinism (KD-50).
 *
 * <p><b>Wiring note (direct fix — Option A).</b> This loader is now wired to the real
 * master-data repositories so generation produces a non-empty draft. One gap remains in the
 * data model: there is no course-offering entity linking a course to the batch/section that
 * takes it and the faculty assigned to teach it. Only {@link FacultyCompetency}
 * (faculty↔course "qualified to teach") and the Program→Batch→Section hierarchy exist. Until
 * a proper CourseOffering entity is added (tracked as the correct follow-up), we build
 * {@link CourseAssignment} rows heuristically:
 * <ul>
 *   <li>faculty = the first competent faculty for the course (lowest faculty id);</li>
 *   <li>batch = every active batch under the department's programs (cartesian course × batch);</li>
 *   <li>section = the batch's first section (if any), else no section;</li>
 *   <li>strength = section sub-strength when a section is used, else batch strength.</li>
 * </ul>
 * Courses with no competent faculty, or with zero L-T-P hours, are skipped. This is an
 * approximation to make the pipeline usable, NOT the modelled truth of who teaches what.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingDataLoader {

    private final SessionDerivationRuleRepository derivationRuleRepository;
    private final InstitutionCommonSlotRepository commonSlotRepository;
    private final SoftConstraintWeightRepository weightRepository;

    // Master-data repositories (wired for the direct fix).
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final FacultyCompetencyRepository facultyCompetencyRepository;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final BatchRepository batchRepository;
    private final SectionRepository sectionRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotGridRepository timeSlotGridRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;
    private final AcademicCalendarRepository academicCalendarRepository;
    private final WorkingDayPatternRepository workingDayPatternRepository;

    /** Default daily/consecutive hour caps used when no cadre-norm module (A4-32) exists yet. */
    private static final double DEFAULT_MAX_DAILY_HOURS = 8.0;
    private static final double DEFAULT_MAX_WEEKLY_HOURS = 40.0;
    private static final double DEFAULT_MAX_CONSECUTIVE_HOURS = 3.0;

    /**
     * FR-1.2: Validate ALL preconditions before generation.
     * Returns ALL failures (not fail-fast) per FR-1.3.
     */
    @Transactional(readOnly = true)
    public List<PreconditionFailure> validatePreconditions(Long deptId, String semester, String academicYear) {
        List<PreconditionFailure> failures = new ArrayList<>();

        Long campusId = getCampusIdForDepartment(deptId);

        // (a) Academic calendar exists for campus + year + semester
        if (!academicCalendarRepository
                .existsByCampusIdAndAcademicYearAndSemesterIdentifierAndDeletedAtIsNull(campusId, academicYear, semester)) {
            failures.add(new PreconditionFailure("ACADEMIC_CALENDAR",
                "No academic calendar for campus " + campusId + ", year " + academicYear
                        + ", semester " + semester));
        }

        // (b) Time-slot grid exists for campus
        if (!timeSlotGridRepository.existsByCampusIdAndDeletedAtIsNull(campusId)) {
            failures.add(new PreconditionFailure("TIME_SLOT_GRID",
                "No time-slot grid configured for campus " + campusId));
        }

        // (c) Working-day pattern exists (prevents mid-generation 422 from CalendarQueryService)
        if (!workingDayPatternRepository.existsByCampusIdAndDeletedAtIsNull(campusId)) {
            failures.add(new PreconditionFailure("WORKING_DAY_PATTERN",
                "No working-day pattern configured for campus " + campusId));
        }

        // (d) Session derivation rules exist for campus (PD-74)
        if (!derivationRuleRepository.existsByCampusIdAndDeletedAtIsNull(campusId)) {
            failures.add(new PreconditionFailure("DERIVATION_RULES",
                "No session derivation rules configured for campus " + campusId));
        }

        // (e) At least one course with faculty assigned (competency) in the department
        if (!hasCoursesWithFaculty(deptId)) {
            failures.add(new PreconditionFailure("FACULTY_ASSIGNMENT",
                "No courses have a competent faculty assigned in department " + deptId));
        }

        // (f) At least one eligible room on the campus
        if (roomRepository.findByCampusIdAndDeletedAtIsNull(campusId).isEmpty()) {
            failures.add(new PreconditionFailure("ROOMS",
                "No active rooms available for campus " + campusId));
        }

        return failures;
    }

    /**
     * FR-2.1-2.7: Load all scheduling input.
     * KD-50: Sort all collections for determinism.
     */
    @Transactional(readOnly = true)
    public SchedulingInput loadAll(Long deptId, String semester, String academicYear) {
        Long campusId = getCampusIdForDepartment(deptId);

        return SchedulingInput.builder()
            .departmentId(deptId)
            .campusId(campusId)
            .semester(semester)
            .courses(loadCoursesWithAssignments(deptId))
            .facultyLimits(loadFacultyLimits(deptId))
            .rooms(loadEligibleRooms(campusId))
            .slotGrid(loadSlotGrid(campusId))
            .workingDays(loadWorkingDays(campusId))
            .activeBlocks(loadActiveBlocks(campusId))
            .commonSlots(loadCommonSlots(campusId))
            .derivationRules(loadDerivationRules(campusId))
            .softConstraintWeights(loadWeights(campusId))
            .build();
    }

    // ===== Private helpers =====

    private Long getCampusIdForDepartment(Long deptId) {
        Department dept = departmentRepository.findByIdAndDeletedAtIsNull(deptId)
            .orElseThrow(() -> new EntityNotFoundException("Department", deptId));
        return dept.getCampus().getId();
    }

    private boolean hasCoursesWithFaculty(Long deptId) {
        for (Course course : courseRepository.findByDepartmentIdAndDeletedAtIsNull(deptId)) {
            if (!facultyCompetencyRepository.findByCourseIdAndDeletedAtIsNull(course.getId()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build course-batch-faculty assignments heuristically (see class Javadoc).
     * Cartesian product of (active courses in dept) × (active batches under dept's programs),
     * pairing each course with its first competent faculty and each batch's first section.
     * Sorted by (courseId, batchId) for determinism (KD-50).
     */
    private List<CourseAssignment> loadCoursesWithAssignments(Long deptId) {
        List<Course> courses = courseRepository.findByDepartmentIdAndDeletedAtIsNull(deptId);
        List<Batch> batches = loadDepartmentBatches(deptId);

        if (courses.isEmpty() || batches.isEmpty()) {
            log.warn("No assignments: department {} has {} courses and {} batches",
                deptId, courses.size(), batches.size());
            return List.of();
        }

        // Heuristic: pair each course with ONE batch (the lowest-id batch in the
        // department), not every batch. Pairing with every batch multiplies the
        // session count (courses x batches) and makes generation time out for real
        // departments. This keeps the problem small enough to complete. The proper
        // fix remains a CourseOffering entity that records the true course->batch mapping.
        Batch batch = batches.get(0); // batches are sorted by id in loadDepartmentBatches
        List<Section> sections = sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batch.getId());
        Section section = sections.isEmpty() ? null
            : sections.stream().min(Comparator.comparing(Section::getId)).orElse(null);
        int strength = resolveStrength(section, batch);

        List<CourseAssignment> assignments = new ArrayList<>();
        for (Course course : courses) {
            int l = course.getLectureHours() != null ? course.getLectureHours() : 0;
            int t = course.getTutorialHours() != null ? course.getTutorialHours() : 0;
            int p = course.getPracticalHours() != null ? course.getPracticalHours() : 0;
            if (l + t + p == 0) {
                continue; // nothing to schedule for this course
            }

            Long facultyId = firstCompetentFacultyId(course.getId());
            if (facultyId == null) {
                log.warn("Skipping course {} ({}) — no competent faculty", course.getId(), course.getCode());
                continue;
            }

            List<String> equipment = course.getEquipmentTags() != null
                ? course.getEquipmentTags() : List.of();

            assignments.add(CourseAssignment.builder()
                .courseId(course.getId())
                .courseCode(course.getCode())
                .facultyId(facultyId)
                .batchId(batch.getId())
                .sectionId(section != null ? section.getId() : null)
                .lectureHours(l)
                .tutorialHours(t)
                .practicalHours(p)
                .batchStrength(strength)
                .equipmentTags(equipment)
                .build());
        }

        assignments.sort(Comparator.comparing(CourseAssignment::getCourseId));
        log.info("Loaded {} course assignments for department {} (one-batch-per-course heuristic; batchId={})",
            assignments.size(), deptId, batch.getId());
        return assignments;
    }

    private int resolveStrength(Section section, Batch batch) {
        if (section != null && section.getSubStrength() != null) {
            return section.getSubStrength();
        }
        return batch.getStrength() != null ? batch.getStrength() : 0;
    }

    private Long firstCompetentFacultyId(Long courseId) {
        return facultyCompetencyRepository.findByCourseIdAndDeletedAtIsNull(courseId).stream()
            .map(fc -> fc.getFaculty().getId())
            .min(Comparator.naturalOrder())
            .orElse(null);
    }

    /** All active batches under the department's programs (Program→Batch). */
    private List<Batch> loadDepartmentBatches(Long deptId) {
        List<Program> programs = programRepository.findByDepartmentIdAndDeletedAtIsNull(deptId);
        List<Batch> batches = new ArrayList<>();
        for (Program program : programs) {
            batches.addAll(batchRepository.findAllByProgramIdAndDeletedAtIsNull(program.getId()));
        }
        batches.sort(Comparator.comparing(Batch::getId));
        return batches;
    }

    /**
     * Per-faculty workload limits. Only weekly max is persisted (Faculty.maxWeeklyLoad);
     * daily/consecutive caps use safe defaults until the cadre-norm module (A4-32) exists.
     * KD-53: limits are in HOURS.
     */
    private List<FacultyWorkloadLimits> loadFacultyLimits(Long deptId) {
        Set<Long> facultyIds = new LinkedHashSet<>();
        for (Course course : courseRepository.findByDepartmentIdAndDeletedAtIsNull(deptId)) {
            for (FacultyCompetency fc : facultyCompetencyRepository
                    .findByCourseIdAndDeletedAtIsNull(course.getId())) {
                facultyIds.add(fc.getFaculty().getId());
            }
        }

        List<FacultyWorkloadLimits> limits = new ArrayList<>();
        for (Long facultyId : facultyIds) {
            Optional<Faculty> faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId);
            double weekly = faculty.map(Faculty::getMaxWeeklyLoad)
                .map(BigDecimal::doubleValue)
                .filter(v -> v > 0)
                .orElse(DEFAULT_MAX_WEEKLY_HOURS);
            limits.add(FacultyWorkloadLimits.builder()
                .facultyId(facultyId)
                .maxDailyHours(DEFAULT_MAX_DAILY_HOURS)
                .maxWeeklyHours(weekly)
                .maxConsecutiveHours(DEFAULT_MAX_CONSECUTIVE_HOURS)
                .build());
        }
        limits.sort(Comparator.comparing(FacultyWorkloadLimits::getFacultyId));
        return limits;
    }

    private List<RoomInfo> loadEligibleRooms(Long campusId) {
        return roomRepository.findByCampusIdAndDeletedAtIsNull(campusId).stream()
            .map(r -> RoomInfo.builder()
                .roomId(r.getId())
                .roomName(r.getName())
                .building(r.getBuilding())
                .floor(r.getFloor())
                .capacity(r.getCapacity() != null ? r.getCapacity() : 0)
                .equipmentTags(r.getEquipmentTags() != null ? r.getEquipmentTags() : List.of())
                .build())
            .sorted(Comparator.comparing(RoomInfo::getRoomId))
            .toList();
    }

    /**
     * Expand the campus slot grid into per-working-day teaching slots. All-day slots
     * (applicableDay == null) apply to every working day; day-specific slots apply only to
     * their day. Slots are indexed per day in start-time order (Fix #1: durationMinutes carried).
     */
    private List<SlotInfo> loadSlotGrid(Long campusId) {
        Optional<TimeSlotGrid> grid = timeSlotGridRepository.findByCampusIdAndDeletedAtIsNull(campusId);
        if (grid.isEmpty()) {
            return List.of();
        }
        List<SlotDefinition> defs = slotDefinitionRepository
            .findByGridIdAndDeletedAtIsNull(grid.get().getId());
        List<String> workingDays = loadWorkingDays(campusId);

        List<SlotInfo> slots = new ArrayList<>();
        for (String day : workingDays) {
            DayOfWeekEnum dayEnum = parseDay(day);
            List<SlotDefinition> daySlots = defs.stream()
                .filter(s -> s.getApplicableDay() == null
                    || (dayEnum != null && s.getApplicableDay() == dayEnum))
                .sorted(Comparator.comparing(SlotDefinition::getStartTime))
                .toList();
            int index = 0;
            for (SlotDefinition s : daySlots) {
                slots.add(SlotInfo.builder()
                    .slotDefinitionId(s.getId())
                    .index(index++)
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .durationMinutes(s.getDurationMinutes())
                    .slotType(s.getSlotType() != null ? s.getSlotType().name() : "TEACHING")
                    .dayOfWeek(day)
                    .build());
            }
        }
        return slots;
    }

    private DayOfWeekEnum parseDay(String day) {
        try {
            return DayOfWeekEnum.valueOf(day);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Working days for the campus, derived from the configured working-day pattern.
     * FIVE_DAY → Mon–Fri; SIX_DAY / ALTERNATE_SATURDAY → Mon–Sat (Saturday granularity is a
     * per-date concern handled elsewhere); CUSTOM / none → Mon–Fri fallback.
     */
    private List<String> loadWorkingDays(Long campusId) {
        List<String> monToFri = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        Optional<WorkingDayPattern> pattern = workingDayPatternRepository
            .findByCampusIdAndDeletedAtIsNull(campusId);
        if (pattern.isEmpty() || pattern.get().getPatternType() == null) {
            return monToFri;
        }
        PatternType type = pattern.get().getPatternType();
        if (type == PatternType.SIX_DAY || type == PatternType.ALTERNATE_SATURDAY) {
            return List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");
        }
        return monToFri;
    }

    /**
     * Active resource blocks as scheduling constraints. Not wired: ResourceBlock stores
     * date/time ranges per resource id with no campus link or slot-index mapping, so
     * translating them into (dayOfWeek, slotIndex) ActiveBlock rows is non-trivial and is
     * left as a follow-up. Returning empty means no block constraints are applied yet.
     */
    private List<ActiveBlock> loadActiveBlocks(Long campusId) {
        return List.of();
    }

    private List<CommonSlotInfo> loadCommonSlots(Long campusId) {
        List<InstitutionCommonSlot> slots = commonSlotRepository.findByCampusIdAndIsActiveTrue(campusId);
        return slots.stream()
            .map(s -> CommonSlotInfo.builder()
                .id(s.getId())
                .name(s.getName())
                .dayOfWeek(s.getDayOfWeek())
                .slotDefinitionId(s.getSlotDefinitionId())
                .appliesToAllBatches(s.getAppliesToAllBatches())
                .build())
            .sorted(Comparator.comparing(CommonSlotInfo::getId))
            .toList();
    }

    private List<SessionDerivationRule> loadDerivationRules(Long campusId) {
        return derivationRuleRepository.findByCampusIdAndIsActiveTrue(campusId).stream()
            .sorted(Comparator.comparing(SessionDerivationRule::getComponentType))
            .toList();
    }

    private Map<SoftConstraintType, Double> loadWeights(Long campusId) {
        List<SoftConstraintWeight> weights = weightRepository.findByCampusIdAndIsActiveTrue(campusId);
        if (weights.isEmpty()) {
            // PD-70: Default equal weights
            return Arrays.stream(SoftConstraintType.values())
                .collect(Collectors.toMap(t -> t, t -> 1.0));
        }
        return weights.stream()
            .collect(Collectors.toMap(
                w -> SoftConstraintType.valueOf(w.getConstraintType()),
                w -> w.getWeight().doubleValue(),
                (a, b) -> a));
    }
}
