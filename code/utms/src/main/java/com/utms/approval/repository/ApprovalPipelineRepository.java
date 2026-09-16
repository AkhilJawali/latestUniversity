package com.utms.approval.repository;

import com.utms.approval.entity.ApprovalPipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalPipelineRepository extends JpaRepository<ApprovalPipeline, Long> {

    /** A4-19 FR-6.3 / PD-102 — the single active global pipeline. */
    Optional<ApprovalPipeline> findFirstByIsActiveTrueAndDeletedAtIsNullOrderByIdAsc();

    List<ApprovalPipeline> findByDeletedAtIsNullOrderByIdAsc();

    Optional<ApprovalPipeline> findByIdAndDeletedAtIsNull(Long id);
}
