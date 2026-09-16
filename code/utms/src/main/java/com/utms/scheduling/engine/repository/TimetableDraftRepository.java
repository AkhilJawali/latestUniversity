package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.TimetableDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableDraftRepository extends JpaRepository<TimetableDraft, Long> {

    Optional<TimetableDraft> findByIdAndDeletedAtIsNull(Long id);

    /** All drafts of a department, newest first — backs the approvals screen's draft list. */
    List<TimetableDraft> findByDepartmentIdAndDeletedAtIsNullOrderByIdDesc(Long departmentId);

    /** A4-21 — the current draft in a given status for a (department, semester, year) scope.
     *  Used to find the active PUBLISHED draft to supersede on a new publication. */
    Optional<TimetableDraft> findByDepartmentIdAndSemesterAndAcademicYearAndStatusAndDeletedAtIsNull(
            Long departmentId, String semester, String academicYear,
            com.utms.scheduling.engine.enums.DraftStatus status);

    @Modifying
    @Query("UPDATE TimetableDraft d SET d.status = 'SUPERSEDED' " +
           "WHERE d.departmentId = :deptId AND d.semester = :semester " +
           "AND d.status = 'DRAFT' AND d.deletedAt IS NULL")
    void supersedePreviousDrafts(@Param("deptId") Long deptId, @Param("semester") String semester);

    @Query("SELECT COALESCE(MAX(d.version), 0) FROM TimetableDraft d " +
           "WHERE d.departmentId = :deptId AND d.semester = :semester AND d.deletedAt IS NULL")
    int getMaxVersion(@Param("deptId") Long deptId, @Param("semester") String semester);
}
