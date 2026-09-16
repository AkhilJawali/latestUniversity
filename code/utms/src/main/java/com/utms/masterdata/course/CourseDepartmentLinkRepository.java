package com.utms.masterdata.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseDepartmentLinkRepository extends JpaRepository<CourseDepartmentLink, Long> {

    boolean existsByCourseIdAndDepartmentId(Long courseId, Long departmentId);

    long countByCourseId(Long courseId);

    Optional<CourseDepartmentLink> findByCourseIdAndDepartmentId(Long courseId, Long departmentId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Course c " +
            "WHERE c.code = :code AND c.department.id = :departmentId " +
            "AND c.id != :excludeCourseId AND c.deletedAt IS NULL")
    boolean existsOtherCourseWithCodeInDepartment(@Param("code") String code,
                                                   @Param("departmentId") Long departmentId,
                                                   @Param("excludeCourseId") Long excludeCourseId);
}
