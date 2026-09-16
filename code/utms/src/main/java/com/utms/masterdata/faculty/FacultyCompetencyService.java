package com.utms.masterdata.faculty;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.course.Course;
import com.utms.masterdata.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyCompetencyService {

    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final FacultyCompetencyRepository facultyCompetencyRepository;

    /**
     * Add competencies in bulk. Idempotent — silently skips already-active links.
     */
    @Transactional
    public void addCompetencies(Long facultyId, List<Long> courseIds) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        for (Long courseId : courseIds) {
            // Skip if already active
            if (facultyCompetencyRepository.existsByFacultyIdAndCourseIdAndDeletedAtIsNull(facultyId, courseId)) {
                log.debug("Competency already exists: facultyId={}, courseId={} — skipping", facultyId, courseId);
                continue;
            }

            Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                    .orElseThrow(() -> new EntityNotFoundException("Course", courseId));

            // Check if soft-deleted record exists (unique constraint is plain)
            Optional<FacultyCompetency> existing = facultyCompetencyRepository
                    .findByFacultyIdAndCourseId(facultyId, courseId);

            if (existing.isPresent()) {
                // Reactivate soft-deleted record
                FacultyCompetency fc = existing.get();
                fc.setDeletedAt(null);
                fc.setIsActive(true);
                facultyCompetencyRepository.save(fc);
                log.info("Competency reactivated: facultyId={}, courseId={}", facultyId, courseId);
            } else {
                FacultyCompetency fc = new FacultyCompetency();
                fc.setFaculty(faculty);
                fc.setCourse(course);
                fc.setIsActive(true);
                facultyCompetencyRepository.save(fc);
                log.info("Competency added: facultyId={}, courseId={}", facultyId, courseId);
            }
        }
    }

    /**
     * Remove a competency link (soft-delete).
     * TODO: Warn if faculty has active sessions for this course.
     */
    @Transactional
    public void removeCompetency(Long facultyId, Long courseId) {
        FacultyCompetency fc = facultyCompetencyRepository
                .findByFacultyIdAndCourseId(facultyId, courseId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("FacultyCompetency",
                        "facultyId=" + facultyId + ",courseId=" + courseId, "not found"));

        fc.setDeletedAt(LocalDateTime.now());
        fc.setIsActive(false);
        facultyCompetencyRepository.save(fc);

        log.info("Competency removed: facultyId={}, courseId={}", facultyId, courseId);
    }

    @Transactional(readOnly = true)
    public List<Long> getCompetencyCourseIds(Long facultyId) {
        return facultyCompetencyRepository.findCourseIdsByFacultyId(facultyId);
    }
}
