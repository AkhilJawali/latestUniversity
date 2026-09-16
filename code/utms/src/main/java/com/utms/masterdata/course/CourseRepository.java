package com.utms.masterdata.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    boolean existsByCodeAndDepartmentIdAndDeletedAtIsNull(String code, Long departmentId);

    Optional<Course> findByIdAndDeletedAtIsNull(Long id);

    long countByDepartmentIdAndDeletedAtIsNull(Long departmentId);

    /** A4-fix (scheduling data loader) — all active courses in a department. */
    List<Course> findByDepartmentIdAndDeletedAtIsNull(Long departmentId);
}
