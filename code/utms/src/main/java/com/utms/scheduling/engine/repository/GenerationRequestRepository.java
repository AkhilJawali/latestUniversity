package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.GenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationRequestRepository extends JpaRepository<GenerationRequest, Long> {

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GenerationRequest g " +
           "WHERE g.departmentId = :deptId AND g.semester = :semester " +
           "AND g.status = 'IN_PROGRESS' AND g.deletedAt IS NULL")
    boolean existsInProgressForDeptSemester(@Param("deptId") Long deptId, @Param("semester") String semester);

    Optional<GenerationRequest> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Find stale requests using per-row timeout_duration_seconds + grace period.
     * A request is stale if triggered_at + timeout_duration_seconds + gracePeriodSeconds < NOW().
     * Used by StaleRequestReaper to detect orphaned requests (process died or JVM crash).
     */
    @Query(value = "SELECT * FROM utms.generation_requests g " +
           "WHERE g.status = 'IN_PROGRESS' AND g.deleted_at IS NULL " +
           "AND g.triggered_at < NOW() - INTERVAL '1 second' * (g.timeout_duration_seconds + :gracePeriodSeconds)",
           nativeQuery = true)
    List<GenerationRequest> findStaleInProgressRequests(@Param("gracePeriodSeconds") int gracePeriodSeconds);
}
