package com.utms.masterdata.course;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final DepartmentRepository departmentRepository;
    private final CoursePrerequisiteService coursePrerequisiteService;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public CourseDto create(CreateCourseRequest request) {
        // Validate department exists
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department", request.getDepartmentId()));

        // Validate code uniqueness within department
        if (courseRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull(
                request.getCode(), request.getDepartmentId())) {
            throw new ConflictException(
                    "Course with code '" + request.getCode() + "' already exists in department " + request.getDepartmentId());
        }

        // Validate LTP sum > 0
        validateLtpSum(request.getLectureHours(), request.getTutorialHours(), request.getPracticalHours());

        Course course = courseMapper.toEntity(request);
        course.setDepartment(department);
        course.setIsActive(true);
        course.setIsCrossListed(false);
        course = courseRepository.save(course);

        // Handle prerequisite course IDs if provided
        if (request.getPrerequisiteCourseIds() != null && !request.getPrerequisiteCourseIds().isEmpty()) {
            for (Long prereqId : request.getPrerequisiteCourseIds()) {
                coursePrerequisiteService.addPrerequisite(course.getId(), prereqId);
            }
        }

        log.info("Course created: id={}, code={}, departmentId={}", course.getId(), course.getCode(), department.getId());
        return courseMapper.toDto(course);
    }

    @Transactional(readOnly = true)
    public CourseDto findById(Long id) {
        Course course = findActiveByIdOrThrow(id);
        return courseMapper.toDto(course);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> findAll(Pageable pageable) {
        Specification<Course> spec = notDeleted();
        return courseRepository.findAll(spec, pageable)
                .map(courseMapper::toDto);
    }

    @Transactional
    public CourseDto update(Long id, UpdateCourseRequest request) {
        Course course = findActiveByIdOrThrow(id);

        // Validate LTP sum > 0
        validateLtpSum(request.getLectureHours(), request.getTutorialHours(), request.getPracticalHours());

        // Detect LTP change and emit event
        boolean ltpChanged = !course.getLectureHours().equals(request.getLectureHours())
                || !course.getTutorialHours().equals(request.getTutorialHours())
                || !course.getPracticalHours().equals(request.getPracticalHours());

        Integer oldL = course.getLectureHours();
        Integer oldT = course.getTutorialHours();
        Integer oldP = course.getPracticalHours();

        // Detect type change and emit event
        boolean typeChanged = !course.getCourseType().equals(request.getCourseType());
        String previousType = course.getCourseType();

        courseMapper.updateEntity(request, course);
        course = courseRepository.save(course);

        if (ltpChanged) {
            applicationEventPublisher.publishEvent(new CourseLtpChangedEvent(
                    course.getId(), oldL, oldT, oldP,
                    request.getLectureHours(), request.getTutorialHours(), request.getPracticalHours()));
            log.info("Course LTP changed: id={}, old=[{},{},{}], new=[{},{},{}]",
                    course.getId(), oldL, oldT, oldP,
                    request.getLectureHours(), request.getTutorialHours(), request.getPracticalHours());
        }

        if (typeChanged) {
            applicationEventPublisher.publishEvent(new CourseTypeChangedEvent(
                    course.getId(), previousType, request.getCourseType()));
            log.info("Course type changed: id={}, from={}, to={}",
                    course.getId(), previousType, request.getCourseType());
        }

        log.info("Course updated: id={}", course.getId());
        return courseMapper.toDto(course);
    }

    @Transactional
    public void delete(Long id) {
        Course course = findActiveByIdOrThrow(id);

        // PD-9: Only active prerequisite dependents block deletion
        List<Map<String, Object>> references = new ArrayList<>();
        long activePrereqDependents = coursePrerequisiteRepository
                .countByPrerequisiteCourseIdAndCourseDeletedAtIsNull(id);
        if (activePrereqDependents > 0) {
            references.add(Map.of("referenceType", "PrerequisiteDependent", "count", activePrereqDependents));
        }

        if (!references.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Cannot delete course: has active prerequisite dependents", references);
        }

        course.setDeletedAt(LocalDateTime.now());
        course.setIsActive(false);
        courseRepository.save(course);

        log.info("Course soft-deleted: id={}, code={}", course.getId(), course.getCode());
    }

    private Course findActiveByIdOrThrow(Long id) {
        return courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
    }

    private void validateLtpSum(Integer lectureHours, Integer tutorialHours, Integer practicalHours) {
        if (lectureHours + tutorialHours + practicalHours <= 0) {
            throw new BusinessRuleViolationException(
                    "At least one of Lecture, Tutorial, or Practical hours must be greater than zero");
        }
    }

    private static Specification<Course> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
