package com.utms.approval.repository;

import com.utms.approval.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Append-only audit trail (A4-19 HC-AW-3). Only reads and {@code save} of new rows are used
 * by the service — no update/delete of existing steps is exposed.
 */
@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    /** Ordered history for an instance (FR-5.1). */
    List<WorkflowStep> findByWorkflowInstanceIdAndDeletedAtIsNullOrderByActedAtAscIdAsc(Long workflowInstanceId);
}
