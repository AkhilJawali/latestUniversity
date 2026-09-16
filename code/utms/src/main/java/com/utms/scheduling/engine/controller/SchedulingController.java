package com.utms.scheduling.engine.controller;

import com.utms.scheduling.engine.dto.*;
import com.utms.scheduling.engine.entity.GenerationRequest;
import com.utms.scheduling.engine.entity.InfeasibilityReport;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.entity.SoftConstraintViolation;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.entity.UnplacedSession;
import com.utms.scheduling.engine.enums.GenerationStatus;
import com.utms.scheduling.engine.model.RegenerationScope;
import com.utms.scheduling.engine.repository.InfeasibilityReportRepository;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.SoftConstraintViolationRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import com.utms.scheduling.engine.repository.UnplacedSessionRepository;
import com.utms.scheduling.engine.service.SchedulingEngineService;
import com.utms.scheduling.engine.service.SessionLockService;
import com.utms.approval.service.CurrentUserProvider;
import com.utms.publication.dto.PublicationResultDto;
import com.utms.publication.service.PublicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingEngineService engineService;
    private final SessionLockService sessionLockService;
    private final PublicationService publicationService;
    private final CurrentUserProvider currentUserProvider;
    private final TimetableDraftRepository draftRepository;
    private final ScheduledSessionRepository sessionRepository;
    private final SoftConstraintViolationRepository violationRepository;
    private final InfeasibilityReportRepository infeasibilityReportRepository;
    private final UnplacedSessionRepository unplacedSessionRepository;

    @PostMapping("/generate")
    public ResponseEntity<GenerationStatusDto> generate(@Valid @RequestBody GenerateRequest request) {
        try {
            GenerationRequest genReq = engineService.triggerGeneration(
                request.getDepartmentId(), request.getSemester(),
                request.getAcademicYear(), request.getSeed(), "system");

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(GenerationStatusDto.builder()
                .requestId(genReq.getId()).status(genReq.getStatus().name())
                .departmentId(genReq.getDepartmentId()).semester(genReq.getSemester())
                .statusUrl("/api/v1/timetables/generate/" + genReq.getId() + "/status").build());
        } catch (RejectedExecutionException ex) {
            throw new ServiceUnavailableException("Scheduling engine queue is full. Try again later.");
        }
    }

    @GetMapping("/generate/{requestId}/status")
    public ResponseEntity<GenerationStatusDto> getStatus(@PathVariable Long requestId) {
        GenerationRequest req = engineService.getStatus(requestId);
        Long elapsed = req.getTriggeredAt() != null
            ? Duration.between(req.getTriggeredAt(), req.getCompletedAt() != null ? req.getCompletedAt() : LocalDateTime.now()).getSeconds() : null;

        return ResponseEntity.ok(GenerationStatusDto.builder()
            .requestId(req.getId()).status(req.getStatus().name()).progress(req.getProgress())
            .phase(req.getPhase() != null ? req.getPhase().name() : null).elapsedSeconds(elapsed)
            .departmentId(req.getDepartmentId()).semester(req.getSemester()).draftId(req.getDraftId())
            .bestSoFarCount(req.getBestSoFarCount()).totalSessions(req.getTotalSessions())
            .cancellable(req.getStatus() == GenerationStatus.IN_PROGRESS)
            .draftUrl(req.getDraftId() != null ? "/api/v1/timetables/" + req.getDraftId() : null)
            .statusUrl("/api/v1/timetables/generate/" + req.getId() + "/status").build());
    }

    /**
     * Cancel an in-progress generation. Idempotent: returns 200 always.
     * If IN_PROGRESS: sets cancel flag. If terminal: returns current state.
     */
    @PostMapping("/generate/{requestId}/cancel")
    public ResponseEntity<CancelResponseDto> cancelGeneration(@PathVariable Long requestId) {
        GenerationRequest req = engineService.cancelGeneration(requestId);
        boolean wasCancellable = req.getStatus() == GenerationStatus.IN_PROGRESS;

        String message = wasCancellable
            ? "Cancellation requested. Generation will stop at next checkpoint."
            : "Generation already in terminal state: " + req.getStatus().name();

        return ResponseEntity.ok(CancelResponseDto.builder()
            .requestId(req.getId())
            .status(req.getStatus().name())
            .message(message)
            .cancellable(wasCancellable)
            .build());
    }

    /**
     * Get infeasibility report for a generation request.
     * Returns 404 if request is not INFEASIBLE.
     */
    @GetMapping("/generate/{requestId}/infeasibility")
    public ResponseEntity<InfeasibilityReportDto> getInfeasibilityReport(@PathVariable Long requestId) {
        GenerationRequest req = engineService.getStatus(requestId);
        if (req.getStatus() != GenerationStatus.INFEASIBLE) {
            throw new com.utms.common.exception.EntityNotFoundException("InfeasibilityReport", requestId);
        }

        InfeasibilityReport report = infeasibilityReportRepository
            .findWithConflictsByGenerationRequestId(requestId)
            .orElseThrow(() -> new com.utms.common.exception.EntityNotFoundException("InfeasibilityReport", requestId));

        List<InfeasibilityConflictDto> conflictDtos = report.getConflicts().stream()
            .map(c -> InfeasibilityConflictDto.builder()
                .id(c.getId())
                .affectedSessionDescription(c.getAffectedSessionDescription())
                .conflictingConstraints(c.getConflictingConstraints())
                .explanation(c.getExplanation())
                .build())
            .toList();

        return ResponseEntity.ok(InfeasibilityReportDto.builder()
            .id(report.getId())
            .generationRequestId(report.getGenerationRequestId())
            .detectedAt(report.getDetectedAt())
            .summary(report.getSummary())
            .conflicts(conflictDtos)
            .build());
    }

    /**
     * Get unplaced sessions for a draft. Returns empty list if draft is not partial.
     */
    @GetMapping("/{draftId}/unplaced")
    public ResponseEntity<List<UnplacedSessionDto>> getUnplacedSessions(@PathVariable Long draftId) {
        draftRepository.findByIdAndDeletedAtIsNull(draftId)
            .orElseThrow(() -> new com.utms.common.exception.EntityNotFoundException("TimetableDraft", draftId));

        List<UnplacedSessionDto> dtos = unplacedSessionRepository.findByDraftIdAndDeletedAtIsNull(draftId).stream()
            .map(u -> UnplacedSessionDto.builder()
                .id(u.getId())
                .draftId(u.getDraftId())
                .courseId(u.getCourseId())
                .courseCode(u.getCourseCode())
                .courseName(u.getCourseName())
                .facultyId(u.getFacultyId())
                .facultyName(u.getFacultyName())
                .batchId(u.getBatchId())
                .batchName(u.getBatchName())
                .sessionType(u.getSessionType())
                .requiredDurationMinutes(u.getRequiredDurationMinutes())
                .reason(u.getReason())
                .build())
            .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * All drafts of a department, newest first, so a draft can be picked for submission,
     * approval and publication. violationCount is not computed for the list (null).
     */
    @GetMapping
    public ResponseEntity<List<TimetableDraftDto>> listDrafts(@RequestParam Long departmentId) {
        List<TimetableDraftDto> dtos = draftRepository.findByDepartmentIdAndDeletedAtIsNullOrderByIdDesc(departmentId)
            .stream().map(d -> toDraftDto(d, null)).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<TimetableDraftDto> getDraft(@PathVariable Long draftId) {
        TimetableDraft draft = draftRepository.findByIdAndDeletedAtIsNull(draftId)
            .orElseThrow(() -> new com.utms.common.exception.EntityNotFoundException("TimetableDraft", draftId));
        int vcnt = violationRepository.findByDraftIdAndDeletedAtIsNull(draftId).size();
        return ResponseEntity.ok(toDraftDto(draft, vcnt));
    }

    private TimetableDraftDto toDraftDto(TimetableDraft draft, Integer violationCount) {
        return TimetableDraftDto.builder()
            .id(draft.getId()).departmentId(draft.getDepartmentId()).semester(draft.getSemester())
            .academicYear(draft.getAcademicYear()).status(draft.getStatus().name()).version(draft.getVersion())
            .feasibilityScore(draft.getFeasibilityScore()).qualityScore(draft.getQualityScore())
            .totalSessionsRequired(draft.getTotalSessionsRequired()).totalSessionsPlaced(draft.getTotalSessionsPlaced())
            .generationRequestId(draft.getGenerationRequestId()).generatedAt(draft.getGeneratedAt())
            .violationCount(violationCount).build();
    }

    /**
     * Publish an APPROVED draft (A4-21, FR-1/FR-4). Makes it the single active PUBLISHED
     * timetable for its (department, semester, year) scope, supersedes any prior published
     * draft, audits the transition, and emits a TimetablePublishedEvent for downstream
     * notification (A4-37) and calendar-feed refresh (A4-39).
     * Returns 404 if the draft is missing, 422 if it is not APPROVED or already published.
     */
    // TODO @PreAuthorize("hasRole('REGISTRAR')") — enable when the Auth/RBAC module lands.
    @PostMapping("/{draftId}/publish")
    public ResponseEntity<PublicationResultDto> publish(@PathVariable Long draftId) {
        return ResponseEntity.ok(publicationService.publish(draftId, currentUserProvider.currentUserId()));
    }

    @GetMapping("/{draftId}/sessions")
    public ResponseEntity<Page<ScheduledSessionDto>> getSessions(@PathVariable Long draftId, Pageable pageable) {
        Page<ScheduledSessionDto> dtos = sessionRepository.findByDraftIdAndDeletedAtIsNull(draftId, pageable)
            .map(s -> ScheduledSessionDto.builder().id(s.getId()).courseId(s.getCourseId())
                .facultyId(s.getFacultyId()).batchId(s.getBatchId()).sectionId(s.getSectionId())
                .roomId(s.getRoomId()).dayOfWeek(s.getDayOfWeek()).slotDefinitionId(s.getSlotDefinitionId())
                .sessionType(s.getSessionType()).isLocked(s.getIsLocked()).build());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{draftId}/violations")
    public ResponseEntity<List<SoftConstraintViolationDto>> getViolations(@PathVariable Long draftId) {
        List<SoftConstraintViolationDto> dtos = violationRepository.findByDraftIdAndDeletedAtIsNull(draftId).stream()
            .map(v -> SoftConstraintViolationDto.builder().id(v.getId()).constraintType(v.getConstraintType())
                .affectedEntityType(v.getAffectedEntityType()).affectedEntityId(v.getAffectedEntityId())
                .description(v.getDescription()).relaxationReason(v.getRelaxationReason()).build())
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /** Trigger a partial re-generation scoped to a subset of a draft (A4-14, FR-3). */
    @PostMapping("/{draftId}/regenerate")
    public ResponseEntity<RegenerationStatusDto> regenerate(@PathVariable Long draftId,
                                                            @Valid @RequestBody RegenerateRequest request) {
        RegenerationScope scope = RegenerationScope.builder()
            .batchIds(request.getScope().getBatchIds())
            .sectionIds(request.getScope().getSectionIds())
            .courseIds(request.getScope().getCourseIds())
            .build();

        GenerationRequest genReq = engineService.triggerRegeneration(draftId, scope, request.getSeed(), "system");

        // Report how many sessions are fixed vs being regenerated (design Section 5).
        List<ScheduledSession> all = sessionRepository.findByDraftIdAndDeletedAtIsNull(draftId);
        long regenerating = all.stream()
            .filter(s -> !Boolean.TRUE.equals(s.getIsLocked()) && !Boolean.TRUE.equals(s.getIsApproved()) && scope.matches(s))
            .count();
        int fixed = all.size() - (int) regenerating;

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RegenerationStatusDto.builder()
            .requestId(genReq.getId()).status(genReq.getStatus().name()).sourceDraftId(draftId)
            .batchIds(scope.getBatchIds()).sectionIds(scope.getSectionIds()).courseIds(scope.getCourseIds())
            .fixedSessionCount(fixed).regeneratingSessionCount((int) regenerating)
            .statusUrl("/api/v1/timetables/generate/" + genReq.getId() + "/status").build());
    }

    /** Lock a session at its current placement (A4-14, FR-1.1). */
    @PostMapping("/{draftId}/sessions/{sessionId}/lock")
    public ResponseEntity<SessionLockDto> lockSession(@PathVariable Long draftId, @PathVariable Long sessionId) {
        ScheduledSession s = sessionLockService.lock(draftId, sessionId, "system");
        return ResponseEntity.ok(SessionLockDto.builder()
            .sessionId(s.getId()).draftId(s.getDraftId()).isLocked(s.getIsLocked()).build());
    }

    /** Unlock a session (A4-14, FR-1.2). */
    @PostMapping("/{draftId}/sessions/{sessionId}/unlock")
    public ResponseEntity<SessionLockDto> unlockSession(@PathVariable Long draftId, @PathVariable Long sessionId) {
        ScheduledSession s = sessionLockService.unlock(draftId, sessionId, "system");
        return ResponseEntity.ok(SessionLockDto.builder()
            .sessionId(s.getId()).draftId(s.getDraftId()).isLocked(s.getIsLocked()).build());
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    static class ServiceUnavailableException extends RuntimeException {
        ServiceUnavailableException(String msg) { super(msg); }
    }
}
