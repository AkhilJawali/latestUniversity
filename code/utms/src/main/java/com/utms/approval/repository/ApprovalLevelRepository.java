package com.utms.approval.repository;

import com.utms.approval.entity.ApprovalLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalLevelRepository extends JpaRepository<ApprovalLevel, Long> {

    /** Ordered levels of a pipeline (A4-19 FR-6). */
    List<ApprovalLevel> findByPipelineIdAndDeletedAtIsNullOrderByLevelIndexAsc(Long pipelineId);
}
