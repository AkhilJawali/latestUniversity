package com.utms.masterdata.course;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseCrossListingService {

    private final CourseRepository courseRepository;
    private final CourseDepartmentLinkRepository courseDepartmentLinkRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public void addCrossListing(Long courseId, Long departmentId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department", departmentId));

        // Cannot cross-list to owning department
        if (course.getDepartment().getId().equals(departmentId)) {
            throw new BusinessRuleViolationException(
                    "Cannot cross-list a course to its owning department");
        }

        // Check if link already exists
        if (courseDepartmentLinkRepository.existsByCourseIdAndDepartmentId(courseId, departmentId)) {
            throw new BusinessRuleViolationException(
                    "Cross-listing already exists for course " + courseId + " in department " + departmentId);
        }

        // Code collision check (KD-8): ensure no other course in target dept has same code
        if (courseDepartmentLinkRepository.existsOtherCourseWithCodeInDepartment(
                course.getCode(), departmentId, courseId)) {
            throw new ConflictException(
                    "Another course with code '" + course.getCode() + "' already exists in department " + departmentId);
        }

        CourseDepartmentLink link = new CourseDepartmentLink();
        link.setCourse(course);
        link.setDepartment(department);
        link.setIsActive(true);
        courseDepartmentLinkRepository.save(link);

        // Sync is_cross_listed flag
        course.setIsCrossListed(true);
        courseRepository.save(course);

        log.info("Cross-listing added: course={} linked to department={}", courseId, departmentId);
    }

    @Transactional
    public void removeCrossListing(Long courseId, Long departmentId) {
        CourseDepartmentLink link = courseDepartmentLinkRepository
                .findByCourseIdAndDepartmentId(courseId, departmentId)
                .orElseThrow(() -> new EntityNotFoundException("CourseDepartmentLink",
                        "courseId=" + courseId + ",departmentId=" + departmentId, "not found"));

        courseDepartmentLinkRepository.delete(link);

        // Sync is_cross_listed flag: if no more links, set to false
        long remainingLinks = courseDepartmentLinkRepository.countByCourseId(courseId);
        if (remainingLinks == 0) {
            Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
            course.setIsCrossListed(false);
            courseRepository.save(course);
        }

        log.info("Cross-listing removed: course={} unlinked from department={}", courseId, departmentId);
    }
}
