package com.utms.approval.service;

import com.utms.approval.dto.ApprovalLevelDto;
import com.utms.approval.dto.ApprovalPipelineDto;
import com.utms.approval.dto.ApproveRequest;
import com.utms.approval.dto.RejectRequest;
import com.utms.approval.dto.SubmitDraftRequest;
import com.utms.approval.dto.WorkflowInstanceDto;
import com.utms.approval.entity.ApprovalPipeline;
import com.utms.approval.entity.WorkflowInstance;
import com.utms.approval.entity.WorkflowStep;
import com.utms.approval.enums.WorkflowAction;
import com.utms.approval.enums.WorkflowState;
import com.utms.approval.event.DraftApprovedEvent;
import com.utms.approval.mapper.ApprovalMapper;
import com.utms.approval.repository.ApprovalPipelineRepository;
import com.utms.approval.repository.WorkflowInstanceRepository;
import com.utms.approval.repository.WorkflowStepRepository;
import com.utms.approval.service.PipelineResolver.ResolvedPipeline;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.DraftStatus;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A4-19 — the multi-level approval workflow state machine. Drives the existing
 * {@link DraftStatus} transitions (DRAFT → UNDER_REVIEW → APPROVED, and back to DRAFT on a
 * first-level rejection), appends an immutable {@link WorkflowStep} for every action
 * (HC-AW-3/HC-AW-5), and emits a {@link DraftApprovedEvent} on final approval so the
 * publication story (A4-21) can publish (PD-104). Publication itself is out of scope here
 * (HC-AW-6).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowService {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final ApprovalPipelineRepository pipelineRepository;
    private final TimetableDraftRepository draftRepository;
    private final PipelineResolver pipelineResolver;
    private final ApprovalMapper mapper;
    private final CurrentUserProvider currentUser;
    private final ApplicationEventPublisher eventPublisher;

    // --- FR-1: submit ---
    public WorkflowInstanceDto submit(SubmitDraftRequest request) {
        String actor = currentUser.currentUserId();
        TimetableDraft draft = draftRepository.findByIdAndDeletedAtIsNull(request.getDraftId())
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", request.getDraftId()));

        if (draft.getStatus() != DraftStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                    "Only a draft in DRAFT status can be submitted for approval (current: " + draft.getStatus() + ")");
        }
        instanceRepository.findByDraftIdAndStateAndDeletedAtIsNull(draft.getId(), WorkflowState.IN_REVIEW)
                .ifPresent(existing -> {
                    throw new BusinessRuleViolationException(
                            "This draft already has an active approval workflow");
                });

        ResolvedPipeline pipeline = pipelineResolver.resolveActive();
        int firstReview = pipeline.firstReviewLevelIndex();

        WorkflowInstance instance = new WorkflowInstance();
        instance.setDraftId(draft.getId());
        instance.setPipelineId(pipeline.pipelineId());
        instance.setCurrentLevelIndex(firstReview);
        instance.setState(WorkflowState.IN_REVIEW);
        instance = instanceRepository.save(instance);

        draft.setStatus(DraftStatus.UNDER_REVIEW);
        draftRepository.save(draft);

        appendStep(instance, firstReview, pipeline.levelName(firstReview),
                WorkflowAction.SUBMITTED, actor, request.getComments(), null);

        return toDto(instance, draft, pipeline);
    }

    // --- FR-2: approve ---
    public WorkflowInstanceDto approve(Long instanceId, ApproveRequest request) {
        String actor = currentUser.currentUserId();
        WorkflowInstance instance = loadActive(instanceId);
        ResolvedPipeline pipeline = pipelineResolver.resolveById(instance.getPipelineId());
        int level = instance.getCurrentLevelIndex();

        appendStep(instance, level, pipeline.levelName(level),
                WorkflowAction.APPROVED, actor, request.getComments(), null);

        TimetableDraft draft = loadDraft(instance.getDraftId());

        if (pipeline.hasNextLevel(level)) {
            instance.setCurrentLevelIndex(level + 1);
            instanceRepository.save(instance); // draft stays UNDER_REVIEW
        } else {
            instance.setState(WorkflowState.APPROVED);
            instanceRepository.save(instance);
            draft.setStatus(DraftStatus.APPROVED);
            draftRepository.save(draft);
            // PD-104 / KD-A19-3: A4-21 publishes on this event, after commit.
            eventPublisher.publishEvent(new DraftApprovedEvent(
                    draft.getId(), instance.getId(), actor, Instant.now()));
        }
        return toDto(instance, draft, pipeline);
    }

    // --- FR-3: reject (return with reason) ---
    public WorkflowInstanceDto reject(Long instanceId, RejectRequest request) {
        String actor = currentUser.currentUserId();
        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new BusinessRuleViolationException("A rejection reason is required");
        }
        WorkflowInstance instance = loadActive(instanceId);
        ResolvedPipeline pipeline = pipelineResolver.resolveById(instance.getPipelineId());
        int level = instance.getCurrentLevelIndex();

        appendStep(instance, level, pipeline.levelName(level),
                WorkflowAction.REJECTED, actor, request.getComments(), request.getRejectionReason());

        TimetableDraft draft = loadDraft(instance.getDraftId());

        if (pipeline.hasPreviousReviewLevel(level)) {
            instance.setCurrentLevelIndex(level - 1);
            instanceRepository.save(instance); // draft stays UNDER_REVIEW
        } else {
            instance.setState(WorkflowState.REJECTED_RETURNED);
            instanceRepository.save(instance);
            draft.setStatus(DraftStatus.DRAFT); // returned to the submitter
            draftRepository.save(draft);
        }
        return toDto(instance, draft, pipeline);
    }

    // --- FR-5: reads ---
    @Transactional(readOnly = true)
    public WorkflowInstanceDto get(Long instanceId) {
        WorkflowInstance instance = instanceRepository.findByIdAndDeletedAtIsNull(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", instanceId));
        return toDto(instance, loadDraft(instance.getDraftId()),
                pipelineResolver.resolveById(instance.getPipelineId()));
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceDto getByDraft(Long draftId) {
        WorkflowInstance instance = instanceRepository
                .findFirstByDraftIdAndDeletedAtIsNullOrderByIdDesc(draftId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", "draftId", String.valueOf(draftId)));
        return toDto(instance, loadDraft(instance.getDraftId()),
                pipelineResolver.resolveById(instance.getPipelineId()));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowInstanceDto> list(WorkflowState state, Integer levelIndex, Pageable pageable) {
        Page<WorkflowInstance> page;
        if (state != null && levelIndex != null) {
            page = instanceRepository.findByStateAndCurrentLevelIndexAndDeletedAtIsNull(state, levelIndex, pageable);
        } else if (state != null) {
            page = instanceRepository.findByStateAndDeletedAtIsNull(state, pageable);
        } else {
            page = instanceRepository.findByDeletedAtIsNull(pageable);
        }
        return page.map(this::toDtoShallow);
    }

    // --- PD-101: read pipelines ---
    @Transactional(readOnly = true)
    public List<ApprovalPipelineDto> listPipelines() {
        return pipelineRepository.findByDeletedAtIsNullOrderByIdAsc().stream()
                .map(this::toPipelineDto)
                .toList();
    }

    // --- helpers ---

    private WorkflowInstance loadActive(Long instanceId) {
        WorkflowInstance instance = instanceRepository.findByIdAndDeletedAtIsNull(instanceId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowInstance", instanceId));
        if (instance.getState() != WorkflowState.IN_REVIEW) {
            throw new BusinessRuleViolationException(
                    "No action is allowed on a workflow in state " + instance.getState());
        }
        return instance;
    }

    private TimetableDraft loadDraft(Long draftId) {
        return draftRepository.findByIdAndDeletedAtIsNull(draftId)
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", draftId));
    }

    private void appendStep(WorkflowInstance instance, int levelIndex, String levelName,
                            WorkflowAction action, String actor, String comments, String rejectionReason) {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowInstanceId(instance.getId());
        step.setLevelIndex(levelIndex);
        step.setLevelName(levelName);
        step.setAction(action);
        step.setActorUserId(actor);
        step.setComments(comments);
        step.setRejectionReason(rejectionReason);
        step.setActedAt(LocalDateTime.now());
        stepRepository.save(step);
    }

    private WorkflowInstanceDto toDto(WorkflowInstance instance, TimetableDraft draft, ResolvedPipeline pipeline) {
        List<WorkflowStep> steps = stepRepository
                .findByWorkflowInstanceIdAndDeletedAtIsNullOrderByActedAtAscIdAsc(instance.getId());
        return WorkflowInstanceDto.builder()
                .id(instance.getId())
                .draftId(instance.getDraftId())
                .draftStatus(draft != null ? draft.getStatus().name() : null)
                .pipelineId(instance.getPipelineId())
                .currentLevelIndex(instance.getCurrentLevelIndex())
                .currentLevelName(pipeline.levelName(instance.getCurrentLevelIndex()))
                .state(instance.getState())
                .steps(mapper.toStepDtos(steps))
                .build();
    }

    // Lightweight DTO for list results (no per-row step fetch).
    private WorkflowInstanceDto toDtoShallow(WorkflowInstance instance) {
        return WorkflowInstanceDto.builder()
                .id(instance.getId())
                .draftId(instance.getDraftId())
                .pipelineId(instance.getPipelineId())
                .currentLevelIndex(instance.getCurrentLevelIndex())
                .state(instance.getState())
                .steps(List.of())
                .build();
    }

    private ApprovalPipelineDto toPipelineDto(ApprovalPipeline pipeline) {
        ResolvedPipeline resolved = pipelineResolver.resolveById(pipeline.getId());
        List<ApprovalLevelDto> levels = mapper.toLevelDtos(resolved.levels());
        return ApprovalPipelineDto.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .scope(pipeline.getScope())
                .isActive(pipeline.getIsActive())
                .levels(levels)
                .build();
    }
}
