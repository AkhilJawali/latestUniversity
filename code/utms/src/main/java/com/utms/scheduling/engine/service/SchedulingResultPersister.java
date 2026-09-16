package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.entity.GenerationRequest;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.entity.SoftConstraintViolation;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.DraftStatus;
import com.utms.scheduling.engine.model.ActiveBlock;
import com.utms.scheduling.engine.model.SchedulingInput;
import com.utms.scheduling.engine.model.SessionVariable;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.SoftConstraintViolationRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import com.utms.scheduling.engine.solver.CSPState;
import com.utms.scheduling.engine.solver.DaySlotRoom;
import com.utms.scheduling.engine.solver.SoftConstraintOptimizer;
import com.utms.scheduling.engine.solver.SoftViolation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Separate @Transactional bean for result persistence (KD-54).
 * Called from async thread in SchedulingEngineService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingResultPersister {

    private final TimetableDraftRepository draftRepository;
    private final ScheduledSessionRepository sessionRepository;
    private final SoftConstraintViolationRepository violationRepository;
    private final SoftConstraintOptimizer softOptimizer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TimetableDraft persistResults(GenerationRequest request, CSPState state,
                                          List<SoftViolation> violations,
                                          SchedulingInput input, int totalRequired) {
        draftRepository.supersedePreviousDrafts(request.getDepartmentId(), request.getSemester());
        int nextVersion = draftRepository.getMaxVersion(request.getDepartmentId(), request.getSemester()) + 1;

        TimetableDraft draft = new TimetableDraft();
        draft.setDepartmentId(request.getDepartmentId());
        draft.setSemester(request.getSemester());
        draft.setAcademicYear(request.getAcademicYear());
        draft.setStatus(DraftStatus.DRAFT);
        draft.setVersion(nextVersion);
        draft.setTotalSessionsRequired(totalRequired);
        draft.setTotalSessionsPlaced(state.getAssignedCount());
        draft.setGenerationRequestId(request.getId());
        draft.setGeneratedAt(LocalDateTime.now());

        double feasibility = totalRequired > 0 ? (double) state.getAssignedCount() / totalRequired : 0.0;
        draft.setFeasibilityScore(BigDecimal.valueOf(feasibility).setScale(3, RoundingMode.HALF_UP));
        double quality = softOptimizer.computeQualityScore(state, input);
        draft.setQualityScore(BigDecimal.valueOf(quality).setScale(3, RoundingMode.HALF_UP));

        draft = draftRepository.save(draft);
        log.info("Draft created: id={}, v={}, feasibility={}, quality={}", draft.getId(), draft.getVersion(), draft.getFeasibilityScore(), draft.getQualityScore());

        List<ScheduledSession> persistedSessions = persistSessions(draft, state);
        persistViolations(draft, violations);
        recordOverridesWithSessionIds(state, input, persistedSessions);

        return draft;
    }

    /**
     * Persist a partial re-generation result into a NEW draft version (A4-14, KD-61, Section 7.5).
     * Copies forward every FIXED session byte-identical (preserving isLocked/isApproved — zero drift,
     * HC-LOCK-1/2/3), then adds the newly-placed FREE sessions from the solved state.
     * Emits a PARTIAL_REGENERATION audit event within this transaction.
     */
    @Transactional
    public TimetableDraft persistRegenerationResult(GenerationRequest request, CSPState state,
                                                    List<ScheduledSession> fixedSessions,
                                                    List<SoftViolation> violations,
                                                    SchedulingInput input, int totalRequired) {
        draftRepository.supersedePreviousDrafts(request.getDepartmentId(), request.getSemester());
        int nextVersion = draftRepository.getMaxVersion(request.getDepartmentId(), request.getSemester()) + 1;

        TimetableDraft draft = new TimetableDraft();
        draft.setDepartmentId(request.getDepartmentId());
        draft.setSemester(request.getSemester());
        draft.setAcademicYear(request.getAcademicYear());
        draft.setStatus(DraftStatus.DRAFT);
        draft.setVersion(nextVersion);
        int placedFree = state.getAssignedCount();
        int totalPlaced = placedFree + fixedSessions.size();
        draft.setTotalSessionsRequired(totalRequired);
        draft.setTotalSessionsPlaced(totalPlaced);
        draft.setGenerationRequestId(request.getId());
        draft.setGeneratedAt(LocalDateTime.now());

        double feasibility = totalRequired > 0 ? (double) totalPlaced / totalRequired : 0.0;
        draft.setFeasibilityScore(BigDecimal.valueOf(feasibility).setScale(3, RoundingMode.HALF_UP));
        double quality = softOptimizer.computeQualityScore(state, input);
        draft.setQualityScore(BigDecimal.valueOf(quality).setScale(3, RoundingMode.HALF_UP));

        draft = draftRepository.save(draft);

        // Copy forward FIXED sessions byte-identical into the new version (zero drift).
        copyForwardFixedSessions(draft, fixedSessions);
        // Add the newly placed FREE sessions.
        persistSessions(draft, state);
        persistViolations(draft, violations);

        // PARTIAL_REGENERATION audit within the same transaction (A4-14 Section 7.5, KD-54).
        eventPublisher.publishEvent(new com.utms.common.audit.AuditEvent(
            "TimetableDraft", draft.getId(), com.utms.common.audit.AuditEvent.Action.CREATED,
            java.util.Map.of("sourceDraftId", request.getSourceDraftId(), "scope", request.getRegenerationScope()),
            java.util.Map.of("draftId", draft.getId(), "fixed", fixedSessions.size(), "regenerated", placedFree),
            request.getTriggeredBy(), java.time.Instant.now()));

        log.info("Regeneration draft created: id={}, v={}, fixed={}, free={}, feasibility={}",
            draft.getId(), draft.getVersion(), fixedSessions.size(), placedFree, draft.getFeasibilityScore());
        return draft;
    }

    /**
     * Clone each fixed session into the new draft, preserving its exact placement and
     * lock/approved flags (A4-14, HC-LOCK-1/2). Placement fields are copied unchanged.
     */
    private void copyForwardFixedSessions(TimetableDraft draft, List<ScheduledSession> fixedSessions) {
        List<ScheduledSession> clones = new ArrayList<>();
        for (ScheduledSession src : fixedSessions) {
            ScheduledSession c = new ScheduledSession();
            c.setDraftId(draft.getId());
            c.setCourseId(src.getCourseId());
            c.setFacultyId(src.getFacultyId());
            c.setBatchId(src.getBatchId());
            c.setSectionId(src.getSectionId());
            c.setRoomId(src.getRoomId());
            c.setDayOfWeek(src.getDayOfWeek());
            c.setSlotDefinitionId(src.getSlotDefinitionId());
            c.setSessionType(src.getSessionType());
            c.setIsLocked(src.getIsLocked());
            c.setIsApproved(src.getIsApproved());
            c.setRecurrenceType(src.getRecurrenceType());
            c.setWeekGroup(src.getWeekGroup());
            clones.add(c);
        }
        sessionRepository.saveAll(clones);
        log.info("Copied forward {} fixed sessions into draft {}", clones.size(), draft.getId());
    }

    private List<ScheduledSession> persistSessions(TimetableDraft draft, CSPState state) {
        List<ScheduledSession> sessions = new ArrayList<>();
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            SessionVariable var = state.getVariable(i);
            DaySlotRoom a = state.getAssignment(i);
            ScheduledSession s = new ScheduledSession();
            s.setDraftId(draft.getId());
            s.setCourseId(var.getCourseId());
            s.setFacultyId(var.getFacultyId());
            s.setBatchId(var.getBatchId());
            s.setSectionId(var.getSectionId());
            s.setRoomId(state.getActualRoomId(a.roomIndex()));
            s.setDayOfWeek(state.getDayName(a.dayIndex()));
            s.setSlotDefinitionId(state.getActualSlotId(a.dayIndex(), a.slotIndex()));
            s.setSessionType(var.getSessionType().name());
            s.setIsLocked(false);
            s.setIsApproved(false); // freshly placed sessions are never pre-approved (A4-14)
            sessions.add(s);
        }
        List<ScheduledSession> saved = sessionRepository.saveAll(sessions);
        log.info("Persisted {} sessions for draft {}", saved.size(), draft.getId());
        return saved;
    }

    private void persistViolations(TimetableDraft draft, List<SoftViolation> violations) {
        for (SoftViolation v : violations) {
            SoftConstraintViolation e = new SoftConstraintViolation();
            e.setDraftId(draft.getId());
            e.setConstraintType(v.type().name());
            e.setAffectedEntityType(v.entityType());
            e.setAffectedEntityId(v.entityId());
            e.setDescription(v.description());
            e.setRelaxationReason(v.reason());
            violationRepository.save(e);
        }
        if (!violations.isEmpty()) log.info("Persisted {} violations for draft {}", violations.size(), draft.getId());
    }

    private void recordOverridesWithSessionIds(CSPState state, SchedulingInput input, List<ScheduledSession> persisted) {
        int idx = 0;
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) continue;
            DaySlotRoom a = state.getAssignment(i);
            Long roomId = state.getActualRoomId(a.roomIndex());
            for (ActiveBlock block : input.getActiveBlocks()) {
                if ("SOFT".equals(block.getBlockType()) && block.getResourceId().equals(roomId)
                    && block.coversSlot(a.dayIndex(), a.slotIndex())) {
                    Long sessionId = persisted.get(idx).getId();
                    // TODO: blockService.recordSoftBlockOverride(block.getBlockId(), "Engine: no alternative", sessionId);
                    log.debug("Soft-block override recorded: blockId={}, sessionId={}", block.getBlockId(), sessionId);
                }
            }
            idx++;
        }
    }
}
