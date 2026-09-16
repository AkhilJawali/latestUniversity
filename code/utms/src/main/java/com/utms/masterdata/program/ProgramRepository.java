package com.utms.masterdata.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long>, JpaSpecificationExecutor<Program> {

    boolean existsByCodeAndDepartmentIdAndDeletedAtIsNull(String code, Long departmentId);

    Optional<Program> findByIdAndDeletedAtIsNull(Long id);

    long countByDepartmentIdAndDeletedAtIsNull(Long departmentId);

    /** A4-fix (scheduling data loader) — all active programs in a department. */
    List<Program> findByDepartmentIdAndDeletedAtIsNull(Long departmentId);
}
