package com.utms.scheduling.engine.repository;

import com.utms.scheduling.engine.entity.InfeasibilityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InfeasibilityReportRepository extends JpaRepository<InfeasibilityReport, Long> {

    Optional<InfeasibilityReport> findByGenerationRequestIdAndDeletedAtIsNull(Long generationRequestId);

    /**
     * A4-fix — eager-fetch the report with its conflicts so the controller can map them
     * outside a transaction without a LazyInitializationException.
     */
    @Query("SELECT r FROM InfeasibilityReport r LEFT JOIN FETCH r.conflicts "
            + "WHERE r.generationRequestId = :requestId AND r.deletedAt IS NULL")
    Optional<InfeasibilityReport> findWithConflictsByGenerationRequestId(@Param("requestId") Long requestId);
}
