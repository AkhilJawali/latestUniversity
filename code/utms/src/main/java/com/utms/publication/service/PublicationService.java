package com.utms.publication.service;

import com.utms.approval.service.CurrentUserProvider;
import com.utms.common.audit.AuditEvent;
import com.utms.common.audit.AuditEvent.Action;
import com.utms.common.audit.AuditEventPublisher;
import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.publication.dto.PublicationResultDto;
import com.utms.publication.event.TimetablePublishedEvent;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.DraftStatus;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A4-21 — Timetable publication.
 *
 * <p>Publishes an APPROVED draft: it becomes the single active PUBLISHED timetable for its
 * (department, semester, academic-year) scope, any previously PUBLISHED draft for the same
 * scope is superseded (KD-A21-2), the transition is audited (PD-110 — reuse AuditEvent, no
 * new table), and a {@link TimetablePublishedEvent} is emitted for downstream delivery
 * (notifications A4-37, PD-107) and feed refresh (A4-39, PD-108). This service does NOT
 * deliver notifications or refresh feeds — it identifies recipients and emits the event.
 *
 * <p>The single-active-published invariant is enforced here in-service (no DB partial-unique
 * index, since A4-21 ships no migration) — flagged as a hardening follow-up.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PublicationService {

    private final TimetableDraftRepository draftRepository;
    private final ScheduledSessionRepository sessionRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publish a draft. Idempotency is deliberately rejected: re-publishing an already
     * PUBLISHED draft is a 422 (PD-111), and only APPROVED drafts may be published (FR-1).
     *
     * @param draftId the draft to publish
     * @param actor   the user id performing the publish (manual endpoint) — may be null, in
     *                which case the current request user is used
     * @return summary of the publication (final status, superseded draft, recipient counts)
     *
     * <p>Always runs in its own transaction. The auto-publish path calls this from an
     * AFTER_COMMIT listener, where the approval transaction's resources are still bound: with
     * the default REQUIRED propagation the call would join that already-committed transaction
     * and the PUBLISHED status would never be flushed. REQUIRES_NEW commits the publication
     * independently (and a failure rolls back only the publication, never the approval).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PublicationResultDto publish(Long draftId, String actor) {
        TimetableDraft draft = draftRepository.findByIdAndDeletedAtIsNull(draftId)
                .orElseThrow(() -> new EntityNotFoundException("TimetableDraft", draftId));

        if (draft.getStatus() == DraftStatus.PUBLISHED) {
            throw new BusinessRuleViolationException(
                    "Timetable draft " + draftId + " is already published");
        }
        if (draft.getStatus() != DraftStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Only an APPROVED draft can be published; draft " + draftId
                            + " is in status " + draft.getStatus());
        }

        String publisher = (actor != null && !actor.isBlank())
                ? actor
                : currentUserProvider.currentUserId();

        // KD-A21-2: supersede the prior active PUBLISHED draft for the same scope (if any).
        Long supersededDraftId = null;
        Optional<TimetableDraft> priorPublished = draftRepository
                .findByDepartmentIdAndSemesterAndAcademicYearAndStatusAndDeletedAtIsNull(
                        draft.getDepartmentId(), draft.getSemester(), draft.getAcademicYear(),
                        DraftStatus.PUBLISHED);
        if (priorPublished.isPresent() && !priorPublished.get().getId().equals(draft.getId())) {
            TimetableDraft prior = priorPublished.get();
            prior.setStatus(DraftStatus.SUPERSEDED);
            supersededDraftId = prior.getId();
            auditEventPublisher.publish(new AuditEvent(
                    "TimetableDraft", prior.getId(), Action.UPDATED,
                    DraftStatus.PUBLISHED, DraftStatus.SUPERSEDED, publisher, Instant.now()));
            log.info("Superseded prior published draft {} for scope (dept={}, sem={}, year={})",
                    prior.getId(), draft.getDepartmentId(), draft.getSemester(),
                    draft.getAcademicYear());
        }

        // Transition the target draft to PUBLISHED.
        DraftStatus previousStatus = draft.getStatus();
        draft.setStatus(DraftStatus.PUBLISHED);

        // Identify affected recipients from the published draft's sessions (PD-109:
        // students addressed at batch/section granularity — no per-student entity yet).
        List<ScheduledSession> sessions = sessionRepository.findByDraftIdAndDeletedAtIsNull(draftId);
        Set<Long> facultyIds = sessions.stream()
                .map(ScheduledSession::getFacultyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> batchIds = sessions.stream()
                .map(ScheduledSession::getBatchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> sectionIds = sessions.stream()
                .map(ScheduledSession::getSectionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Audit the publish transition (PD-110).
        auditEventPublisher.publish(new AuditEvent(
                "TimetableDraft", draft.getId(), Action.UPDATED,
                previousStatus, DraftStatus.PUBLISHED, publisher, Instant.now()));

        // Emit the integration event (KD-A21-3: published in-tx; consumers listen AFTER_COMMIT).
        eventPublisher.publishEvent(new TimetablePublishedEvent(
                draft.getId(), draft.getDepartmentId(), draft.getSemester(),
                draft.getAcademicYear(), publisher, Instant.now(),
                facultyIds, batchIds, sectionIds));

        log.info("Published timetable draft {} (dept={}, sem={}, year={}) by {} — "
                        + "{} faculty, {} batches, {} sections affected",
                draft.getId(), draft.getDepartmentId(), draft.getSemester(),
                draft.getAcademicYear(), publisher,
                facultyIds.size(), batchIds.size(), sectionIds.size());

        return PublicationResultDto.builder()
                .draftId(draft.getId())
                .status(draft.getStatus().name())
                .supersededDraftId(supersededDraftId)
                .affectedFacultyCount(facultyIds.size())
                .affectedBatchCount(batchIds.size())
                .build();
    }
}
