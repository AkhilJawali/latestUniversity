package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyCompetencyRepository extends JpaRepository<FacultyCompetency, Long> {

    List<FacultyCompetency> findByFacultyIdAndDeletedAtIsNull(Long facultyId);

    /** A4-fix (scheduling data loader) — competencies for a course (which faculty can teach it). */
    List<FacultyCompetency> findByCourseIdAndDeletedAtIsNull(Long courseId);

    @Query("SELECT fc.course.id FROM FacultyCompetency fc WHERE fc.faculty.id = :facultyId AND fc.deletedAt IS NULL")
    List<Long> findCourseIdsByFacultyId(@Param("facultyId") Long facultyId);

    Optional<FacultyCompetency> findByFacultyIdAndCourseId(Long facultyId, Long courseId);

    boolean existsByFacultyIdAndCourseIdAndDeletedAtIsNull(Long facultyId, Long courseId);

    long countByCourseIdAndDeletedAtIsNull(Long courseId);
}
