package com.utms.approval.repository;

import com.utms.approval.entity.WorkflowInstance;
import com.utms.approval.enums.WorkflowState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Optional<WorkflowInstance> findByIdAndDeletedAtIsNull(Long id);

    /** HC-AW-1 — the single active (IN_REVIEW) workflow for a draft, if any. */
    Optional<WorkflowInstance> findByDraftIdAndStateAndDeletedAtIsNull(Long draftId, WorkflowState state);

    /** FR-5.2 — the latest workflow instance for a draft (any state). */
    Optional<WorkflowInstance> findFirstByDraftIdAndDeletedAtIsNullOrderByIdDesc(Long draftId);

    /** FR-5.3 — reviewer queue, filtered by state + level. */
    Page<WorkflowInstance> findByStateAndCurrentLevelIndexAndDeletedAtIsNull(
            WorkflowState state, Integer currentLevelIndex, Pageable pageable);

    Page<WorkflowInstance> findByStateAndDeletedAtIsNull(WorkflowState state, Pageable pageable);

    Page<WorkflowInstance> findByDeletedAtIsNull(Pageable pageable);
}
