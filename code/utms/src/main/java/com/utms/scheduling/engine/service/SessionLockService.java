package com.utms.scheduling.engine.service;

import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Locking and unlocking of scheduled sessions (A4-14, FR-1).
 * A locked session is preserved at its exact placement by the engine (HC-LOCK-1).
 * All mutations are audited within the same transaction (KD-54 pattern).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionLockService {

    private final ScheduledSessionRepository sessionRepository;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Lock a session at its current placement (FR-1.1, FR-1.3).
     * Validates the session belongs to the given draft and has a complete placement.
     */
    @Transactional
    public ScheduledSession lock(Long draftId, Long sessionId, String userId) {
        ScheduledSession session = loadSessionInDraft(draftId, sessionId);
        requireCompletePlacement(session);

        boolean previous = Boolean.TRUE.equals(session.getIsLocked());
        session.setIsLocked(true);
        ScheduledSession saved = sessionRepository.save(session);

        auditEventPublisher.publish(new AuditEvent(
                "ScheduledSession", saved.getId(), AuditEvent.Action.UPDATED,
                Map.of("isLocked", previous), Map.of("isLocked", true),
                userId, Instant.now()));
        log.info("Session locked: draftId={}, sessionId={}, by={}", draftId, sessionId, userId);
        return saved;
    }

    /**
     * Unlock a session (FR-1.2, PD-86). On the next generation it becomes movable again.
     */
    @Transactional
    public ScheduledSession unlock(Long draftId, Long sessionId, String userId) {
        ScheduledSession session = loadSessionInDraft(draftId, sessionId);

        boolean previous = Boolean.TRUE.equals(session.getIsLocked());
        session.setIsLocked(false);
        ScheduledSession saved = sessionRepository.save(session);

        auditEventPublisher.publish(new AuditEvent(
                "ScheduledSession", saved.getId(), AuditEvent.Action.UPDATED,
                Map.of("isLocked", previous), Map.of("isLocked", false),
                userId, Instant.now()));
        log.info("Session unlocked: draftId={}, sessionId={}, by={}", draftId, sessionId, userId);
        return saved;
    }

    /**
     * Load a session and confirm it belongs to the given draft.
     * Enforces "lock target exists" and "subset belongs to draft" validation rules.
     */
    private ScheduledSession loadSessionInDraft(Long draftId, Long sessionId) {
        ScheduledSession session = sessionRepository.findByIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("ScheduledSession", sessionId));
        if (!session.getDraftId().equals(draftId)) {
            throw new EntityNotFoundException("ScheduledSession", sessionId);
        }
        return session;
    }

    /**
     * A session can only be locked if it has a complete placement (FR-1.3 validation rule).
     */
    private void requireCompletePlacement(ScheduledSession session) {
        boolean complete = session.getDayOfWeek() != null
                && session.getSlotDefinitionId() != null
                && session.getRoomId() != null
                && session.getFacultyId() != null;
        if (!complete) {
            throw new BusinessRuleViolationException(
                    "Session cannot be locked without a complete placement (day, slot, room, faculty).",
                    List.of(Map.of("rule", "LOCK_REQUIRES_FULL_PLACEMENT", "sessionId", session.getId())));
        }
    }
}
