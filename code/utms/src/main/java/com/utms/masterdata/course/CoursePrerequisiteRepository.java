package com.utms.masterdata.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    @Query("SELECT cp.prerequisiteCourse.id FROM CoursePrerequisite cp WHERE cp.course.id = :courseId")
    List<Long> findPrerequisiteIdsByCourseId(@Param("courseId") Long courseId);

    long countByPrerequisiteCourseIdAndCourseDeletedAtIsNull(Long prerequisiteCourseId);

    Optional<CoursePrerequisite> findByCourseIdAndPrerequisiteCourseId(Long courseId, Long prerequisiteCourseId);

    List<CoursePrerequisite> findByCourseId(Long courseId);
}
