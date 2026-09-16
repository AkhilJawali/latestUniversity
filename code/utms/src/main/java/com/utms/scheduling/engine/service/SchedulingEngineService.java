package com.utms.scheduling.engine.service;

import com.utms.common.exception.BusinessRuleViolationException;
import com.utms.common.exception.ConflictException;
import com.utms.scheduling.engine.config.SchedulingEngineProperties;
import com.utms.scheduling.engine.entity.GenerationRequest;
import com.utms.scheduling.engine.entity.InfeasibilityConflict;
import com.utms.scheduling.engine.entity.InfeasibilityReport;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.entity.UnplacedSession;
import com.utms.scheduling.engine.entity.ScheduledSession;
import com.utms.scheduling.engine.enums.FixedReason;
import com.utms.scheduling.engine.enums.GenerationPhase;
import com.utms.scheduling.engine.enums.GenerationStatus;
import com.utms.scheduling.engine.model.FixedSessionInfo;
import com.utms.scheduling.engine.model.PreconditionFailure;
import com.utms.scheduling.engine.model.RegenerationScope;
import com.utms.scheduling.engine.model.SchedulingInput;
import com.utms.scheduling.engine.model.SessionVariable;
import com.utms.scheduling.engine.repository.GenerationRequestRepository;
import com.utms.scheduling.engine.repository.InfeasibilityReportRepository;
import com.utms.scheduling.engine.repository.ScheduledSessionRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import com.utms.scheduling.engine.repository.UnplacedSessionRepository;
import com.utms.scheduling.engine.solver.CSPState;
import com.utms.scheduling.engine.solver.ConstraintSolver;
import com.utms.scheduling.engine.solver.InfeasibilityCollector;
import com.utms.scheduling.engine.solver.InfeasibilityEntry;
import com.utms.scheduling.engine.solver.SoftConstraintOptimizer;
import com.utms.scheduling.engine.solver.SoftViolation;
import com.utms.scheduling.engine.solver.SolverContext;
import com.utms.scheduling.engine.solver.SolverOutcome;
import com.utms.scheduling.engine.solver.SolverResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Orchestrates timetable generation: triggers async execution, manages solver contexts,
 * handles timeout/cancel/infeasibility outcomes, and persists results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingEngineService {

    private final SchedulingDataLoader dataLoader;
    private final SessionDeriver sessionDeriver;
    private final ConstraintSolver constraintSolver;
    private final SoftConstraintOptimizer softOptimizer;
    private final SchedulingResultPersister resultPersister;
    private final GenerationRequestRepository requestRepository;
    private final InfeasibilityReportRepository infeasibilityReportRepository;
    private final UnplacedSessionRepository unplacedSessionRepository;
    private final ScheduledSessionRepository sessionRepository;
    private final TimetableDraftRepository draftRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final SchedulingEngineProperties engineProperties;
    @Qualifier("schedulingEngineExecutor")
    private final Executor schedulingExecutor;

    /** Active solver contexts keyed by GenerationRequest ID. Used for cancel signaling. */
    private final ConcurrentHashMap<Long, SolverContext> solverContexts = new ConcurrentHashMap<>();

    @Transactional
    public GenerationRequest triggerGeneration(Long departmentId, String semester, String academicYear, Long seed, String userId) {
        if (requestRepository.existsInProgressForDeptSemester(departmentId, semester)) {
            throw new ConflictException("Generation already in progress for department " + departmentId + ", semester " + semester);
        }
        List<PreconditionFailure> failures = dataLoader.validatePreconditions(departmentId, semester, academicYear);
        if (!failures.isEmpty()) {
            throw new BusinessRuleViolationException("Generation preconditions not met",
                failures.stream().map(f -> Map.<String, Object>of("check", f.check(), "message", f.message())).toList());
        }

        GenerationRequest request = new GenerationRequest();
        request.setDepartmentId(departmentId);
        request.setSemester(semester);
        request.setAcademicYear(academicYear);
        request.setStatus(GenerationStatus.IN_PROGRESS);
        request.setPhase(GenerationPhase.LOADING_DATA);
        request.setProgress(0);
        request.setSeed(seed != null ? seed : computeDefaultSeed(departmentId, semester));
        request.setTriggeredBy(userId);
        request.setTriggeredAt(LocalDateTime.now());
        request.setTimeoutDurationSeconds(engineProperties.getDefaultTimeoutSeconds());
        request = requestRepository.save(request);

        final Long requestId = request.getId();
        CompletableFuture.runAsync(() -> executeGeneration(requestId), schedulingExecutor);
        log.info("Generation triggered: requestId={}, dept={}, semester={}", requestId, departmentId, semester);
        return request;
    }

    @Transactional(readOnly = true)
    public GenerationRequest getStatus(Long requestId) {
        return requestRepository.findByIdAndDeletedAtIsNull(requestId)
            .orElseThrow(() -> new com.utms.common.exception.EntityNotFoundException("GenerationRequest", requestId));
    }

    /**
     * Cancel an in-progress generation. Idempotent: if already terminal, returns current state.
     * Sets the cancel flag on the SolverContext so the solver exits gracefully.
     */
    @Transactional(readOnly = true)
    public GenerationRequest cancelGeneration(Long requestId) {
        GenerationRequest request = requestRepository.findByIdAndDeletedAtIsNull(requestId)
            .orElseThrow(() -> new com.utms.common.exception.EntityNotFoundException("GenerationRequest", requestId));

        if (request.getStatus() == GenerationStatus.IN_PROGRESS) {
            SolverContext context = solverContexts.get(requestId);
            if (context != null) {
                context.requestCancel();
                log.info("Cancel requested for generation: requestId={}", requestId);
            }
        }

        return request;
    }

    /**
     * Trigger a partial re-generation scoped to a subset of a source draft (A4-14, Section 7.2).
     * Partitions the draft's sessions into FIXED (locked / approved / out-of-scope) and FREE
     * (in-scope and movable), then regenerates only the FREE subset around the FIXED occupancy.
     */
    @Transactional
    public GenerationRequest triggerRegeneration(Long sourceDraftId, RegenerationScope scope, Long seed, String userId) {
        TimetableDraft source = draftRepository.findByIdAndDeletedAtIsNull(sourceDraftId)
            .orElseThrow(() -> new com.utms.common.exception.EntityNotFoundException("TimetableDraft", sourceDraftId));

        if (requestRepository.existsInProgressForDeptSemester(source.getDepartmentId(), source.getSemester())) {
            throw new ConflictException("Generation already in progress for department "
                + source.getDepartmentId() + ", semester " + source.getSemester());
        }

        List<ScheduledSession> allSessions = sessionRepository.findByDraftIdAndDeletedAtIsNull(sourceDraftId);

        // Validation: the scope must reference entities present in the draft (422).
        if (scope == null || scope.isEmpty()) {
            throw new BusinessRuleViolationException("Regeneration scope is empty.",
                List.of(Map.of("rule", "SCOPE_EMPTY")));
        }
        boolean scopeMatchesDraft = allSessions.stream().anyMatch(scope::matches);
        if (!scopeMatchesDraft) {
            throw new BusinessRuleViolationException(
                "Regeneration scope references no sessions in this draft.",
                List.of(Map.of("rule", "SCOPE_NOT_IN_DRAFT", "draftId", sourceDraftId)));
        }

        // Partition: FIXED wins over selected (locked/approved always fixed).
        long freeCount = allSessions.stream().filter(s -> isFree(s, scope)).count();
        if (freeCount == 0) {
            throw new BusinessRuleViolationException(
                "Regeneration scope selects no movable sessions (all matches are locked or approved).",
                List.of(Map.of("rule", "NO_MOVABLE_SESSIONS", "draftId", sourceDraftId)));
        }

        GenerationRequest request = new GenerationRequest();
        request.setDepartmentId(source.getDepartmentId());
        request.setSemester(source.getSemester());
        request.setAcademicYear(source.getAcademicYear());
        request.setStatus(GenerationStatus.IN_PROGRESS);
        request.setPhase(GenerationPhase.LOADING_DATA);
        request.setProgress(0);
        request.setSeed(seed != null ? seed : computeDefaultSeed(source.getDepartmentId(), source.getSemester()));
        request.setTriggeredBy(userId);
        request.setTriggeredAt(LocalDateTime.now());
        request.setTimeoutDurationSeconds(engineProperties.getDefaultTimeoutSeconds());
        request.setSourceDraftId(sourceDraftId);
        request.setRegenerationScope(serializeScope(scope));
        request = requestRepository.save(request);

        final Long requestId = request.getId();
        CompletableFuture.runAsync(() -> executeRegeneration(requestId, sourceDraftId, scope), schedulingExecutor);
        log.info("Partial re-generation triggered: requestId={}, sourceDraftId={}, free={}", requestId, sourceDraftId, freeCount);
        return request;
    }

    /** A session is FREE (movable) if it matches the scope AND is neither locked nor approved. */
    private boolean isFree(ScheduledSession s, RegenerationScope scope) {
        boolean fixed = Boolean.TRUE.equals(s.getIsLocked()) || Boolean.TRUE.equals(s.getIsApproved());
        return !fixed && scope.matches(s);
    }

    private void executeRegeneration(Long requestId, Long sourceDraftId, RegenerationScope scope) {
        long startTime = System.currentTimeMillis();
        try {
            GenerationRequest request = requestRepository.findById(requestId).orElseThrow();
            Random seededRandom = new Random(request.getSeed());
            long timeoutMs = request.getTimeoutDurationSeconds() * 1000L;
            SolverContext context = new SolverContext(startTime, timeoutMs, requestId);
            solverContexts.put(requestId, context);

            updateProgress(requestId, GenerationPhase.LOADING_DATA, 5);
            SchedulingInput input = dataLoader.loadAll(request.getDepartmentId(), request.getSemester(), request.getAcademicYear());

            // Partition the source draft's sessions into FIXED and FREE.
            List<ScheduledSession> allSessions = sessionRepository.findByDraftIdAndDeletedAtIsNull(sourceDraftId);
            List<ScheduledSession> fixedSessions = new ArrayList<>();
            for (ScheduledSession s : allSessions) {
                if (!isFree(s, scope)) fixedSessions.add(s);
            }

            updateProgress(requestId, GenerationPhase.DERIVING_SESSIONS, 15);
            // Derive the full required set, keep in-scope variables, then SUBTRACT the sessions
            // already fixed for the same (course, batch, section, type) group so a locked/approved
            // session is not regenerated as a duplicate free variable (fix: derive-vs-partition seam).
            List<SessionVariable> allVariables = sessionDeriver.derive(input);
            Map<String, Long> fixedCountByGroup = fixedSessions.stream()
                .collect(java.util.stream.Collectors.groupingBy(this::sessionGroupKey, java.util.stream.Collectors.counting()));
            Map<String, Integer> takenByGroup = new java.util.HashMap<>();
            List<SessionVariable> freeVariables = new ArrayList<>();
            for (SessionVariable v : allVariables) {
                if (!isVariableFree(v, scope)) continue;
                String key = variableGroupKey(v);
                long alreadyFixed = fixedCountByGroup.getOrDefault(key, 0L);
                int consumed = takenByGroup.getOrDefault(key, 0);
                // Skip as many derived variables as there are fixed sessions in this group.
                if (consumed < alreadyFixed) {
                    takenByGroup.put(key, consumed + 1);
                    continue;
                }
                freeVariables.add(v);
            }
            // Total required for this partial re-gen = fixed (copied forward) + free (being placed).
            int totalRequired = fixedSessions.size() + freeVariables.size();
            updateTotalSessions(requestId, totalRequired);

            updateProgress(requestId, GenerationPhase.PROPAGATING, 25);
            InfeasibilityCollector infeasibilityCollector = new InfeasibilityCollector(freeVariables);
            CSPState state = constraintSolver.initializeAndPropagate(freeVariables, input, infeasibilityCollector);
            // Pre-place FIXED sessions as immovable occupancy (KD-59, HC-LOCK-4).
            state.prePlaceFixedSessions(toFixedSessionInfos(fixedSessions));

            updateProgress(requestId, GenerationPhase.SOLVING, 40);
            SolverResult result = constraintSolver.solve(state, seededRandom, context,
                newBestCount -> updateBestSoFar(requestId, newBestCount, freeVariables.size()),
                infeasibilityCollector);

            SolverOutcome outcome = result.getOutcome();
            CSPState finalState = result.getState();
            if (outcome == SolverOutcome.COMPLETE) {
                updateProgress(requestId, GenerationPhase.OPTIMIZING, 70);
                finalState = constraintSolver.optimize(result.getState(), true, input, seededRandom, context);
            }
            List<SoftViolation> violations = softOptimizer.identifyViolations(finalState, input);

            updateProgress(requestId, GenerationPhase.STORING_RESULTS, 90);
            TimetableDraft draft = resultPersister.persistRegenerationResult(
                request, finalState, fixedSessions, violations, input, totalRequired);
            if (outcome != SolverOutcome.COMPLETE) {
                draft.setIsPartial(true);
                draftRepository.save(draft);
                persistUnplacedSessions(draft.getId(), finalState, freeVariables, input);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            switch (outcome) {
                case COMPLETE -> completeRequest(requestId, draft.getId(), elapsed);
                case TIMED_OUT -> timeoutRequest(requestId, draft.getId(), elapsed);
                case CANCELLED -> cancelledRequest(requestId, draft.getId(), elapsed);
                case INFEASIBLE -> {
                    persistInfeasibilityReport(requestId, result.getInfeasibilityEntries());
                    infeasibleRequest(requestId, draft.getId(), elapsed);
                }
            }
            log.info("Partial re-generation finished: requestId={}, outcome={}, draftId={}", requestId, outcome, draft.getId());
        } catch (Exception ex) {
            log.error("Partial re-generation failed: requestId={}", requestId, ex);
            failRequest(requestId, ex.getMessage());
        } finally {
            solverContexts.remove(requestId);
        }
    }

    /** A derived variable is in-scope if its (batch/section/course) matches the scope. This is only
     *  the scope filter; reconciliation against already-fixed sessions (so locked/approved sessions
     *  are not regenerated as duplicates) is done separately by the per-group subtraction in executeRegeneration. */
    private boolean isVariableFree(SessionVariable v, RegenerationScope scope) {
        return contains(scope.getBatchIds(), v.getBatchId())
            || contains(scope.getSectionIds(), v.getSectionId())
            || contains(scope.getCourseIds(), v.getCourseId());
    }

    private boolean contains(List<Long> ids, Long id) {
        return id != null && ids != null && ids.contains(id);
    }

    /** Group key for a persisted fixed session: (course, batch, section, type). */
    private String sessionGroupKey(ScheduledSession s) {
        return s.getCourseId() + "|" + s.getBatchId() + "|" + s.getSectionId() + "|" + s.getSessionType();
    }

    /** Group key for a derived variable — must align with {@link #sessionGroupKey}. */
    private String variableGroupKey(SessionVariable v) {
        return v.getCourseId() + "|" + v.getBatchId() + "|" + v.getSectionId() + "|" + v.getSessionType().name();
    }

    private List<FixedSessionInfo> toFixedSessionInfos(List<ScheduledSession> fixedSessions) {
        List<FixedSessionInfo> result = new ArrayList<>();
        for (ScheduledSession s : fixedSessions) {
            FixedReason reason = Boolean.TRUE.equals(s.getIsLocked()) ? FixedReason.LOCKED
                : Boolean.TRUE.equals(s.getIsApproved()) ? FixedReason.APPROVED
                : FixedReason.OUT_OF_SCOPE;
            result.add(FixedSessionInfo.builder()
                .sessionId(s.getId()).facultyId(s.getFacultyId()).batchId(s.getBatchId())
                .roomId(s.getRoomId()).dayOfWeek(s.getDayOfWeek()).slotDefinitionId(s.getSlotDefinitionId())
                .reason(reason).build());
        }
        return result;
    }

    private String serializeScope(RegenerationScope scope) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "batchIds", scope.getBatchIds() != null ? scope.getBatchIds() : List.of(),
                "sectionIds", scope.getSectionIds() != null ? scope.getSectionIds() : List.of(),
                "courseIds", scope.getCourseIds() != null ? scope.getCourseIds() : List.of()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Failed to serialize regeneration scope; storing empty selector", e);
            return "{\"batchIds\":[],\"sectionIds\":[],\"courseIds\":[]}";
        }
    }

    private void executeGeneration(Long requestId) {
        long startTime = System.currentTimeMillis();
        try {
            GenerationRequest request = requestRepository.findById(requestId).orElseThrow();
            Random seededRandom = new Random(request.getSeed());

            // Create SolverContext with configured timeout
            long timeoutMs = request.getTimeoutDurationSeconds() * 1000L;
            SolverContext context = new SolverContext(startTime, timeoutMs, requestId);
            solverContexts.put(requestId, context);

            updateProgress(requestId, GenerationPhase.LOADING_DATA, 5);
            SchedulingInput input = dataLoader.loadAll(request.getDepartmentId(), request.getSemester(), request.getAcademicYear());

            updateProgress(requestId, GenerationPhase.DERIVING_SESSIONS, 15);
            List<SessionVariable> variables = sessionDeriver.derive(input);

            // Set totalSessions on the request after derivation
            updateTotalSessions(requestId, variables.size());

            updateProgress(requestId, GenerationPhase.PROPAGATING, 25);
            InfeasibilityCollector infeasibilityCollector = new InfeasibilityCollector(variables);
            CSPState state = constraintSolver.initializeAndPropagate(variables, input, infeasibilityCollector);

            updateProgress(requestId, GenerationPhase.SOLVING, 40);
            SolverResult result = constraintSolver.solve(state, seededRandom, context,
                newBestCount -> updateBestSoFar(requestId, newBestCount, variables.size()),
                infeasibilityCollector);

            // Handle outcome based on SolverOutcome
            SolverOutcome outcome = result.getOutcome();

            if (outcome == SolverOutcome.COMPLETE) {
                updateProgress(requestId, GenerationPhase.OPTIMIZING, 70);
                CSPState optimizedState = constraintSolver.optimize(result.getState(), true, input, seededRandom, context);
                List<SoftViolation> violations = softOptimizer.identifyViolations(optimizedState, input);

                updateProgress(requestId, GenerationPhase.STORING_RESULTS, 90);
                TimetableDraft draft = resultPersister.persistResults(request, optimizedState, violations, input, variables.size());

                long elapsed = System.currentTimeMillis() - startTime;
                completeRequest(requestId, draft.getId(), elapsed);
                log.info("Generation completed: requestId={}, draftId={}, elapsed={}ms", requestId, draft.getId(), elapsed);

            } else if (outcome == SolverOutcome.TIMED_OUT) {
                updateProgress(requestId, GenerationPhase.STORING_RESULTS, 90);
                TimetableDraft draft = persistPartialDraft(request, result.getState(), input, variables.size());
                persistUnplacedSessions(draft.getId(), result.getState(), variables, input);
                long elapsed = System.currentTimeMillis() - startTime;
                timeoutRequest(requestId, draft.getId(), elapsed);
                log.warn("Generation timed out: requestId={}, draftId={}, elapsed={}ms", requestId, draft.getId(), elapsed);

            } else if (outcome == SolverOutcome.INFEASIBLE) {
                updateProgress(requestId, GenerationPhase.STORING_RESULTS, 90);
                TimetableDraft draft = persistPartialDraft(request, result.getState(), input, variables.size());
                persistUnplacedSessions(draft.getId(), result.getState(), variables, input);
                persistInfeasibilityReport(requestId, result.getInfeasibilityEntries());
                long elapsed = System.currentTimeMillis() - startTime;
                infeasibleRequest(requestId, draft.getId(), elapsed);
                log.warn("Generation infeasible: requestId={}, conflicts={}", requestId, result.getInfeasibilityEntries().size());

            } else if (outcome == SolverOutcome.CANCELLED) {
                updateProgress(requestId, GenerationPhase.STORING_RESULTS, 90);
                TimetableDraft draft = persistPartialDraft(request, result.getState(), input, variables.size());
                persistUnplacedSessions(draft.getId(), result.getState(), variables, input);
                long elapsed = System.currentTimeMillis() - startTime;
                cancelledRequest(requestId, draft.getId(), elapsed);
                log.info("Generation cancelled: requestId={}, draftId={}, elapsed={}ms", requestId, draft.getId(), elapsed);
            }

        } catch (Exception ex) {
            log.error("Generation failed: requestId={}", requestId, ex);
            failRequest(requestId, ex.getMessage());
        } finally {
            solverContexts.remove(requestId);
        }
    }

    /**
     * ProgressCallback implementation: update best-so-far count in the database.
     * Throttled: only persists when improvement exceeds configured threshold percentage of total.
     */
    private void updateBestSoFar(Long requestId, int newBestCount, int totalSessions) {
        if (totalSessions <= 0) return;

        int thresholdIncrement = Math.max(1, (totalSessions * engineProperties.getProgressThresholdPercent()) / 100);
        requestRepository.findById(requestId).ifPresent(r -> {
            int previousBest = r.getBestSoFarCount();
            if (newBestCount - previousBest >= thresholdIncrement) {
                r.setBestSoFarCount(newBestCount);
                int progressPct = 40 + (int) ((newBestCount * 30.0) / totalSessions);
                r.setProgress(Math.min(progressPct, 70));
                requestRepository.save(r);
                log.debug("Best-so-far updated: requestId={}, count={}/{}", requestId, newBestCount, totalSessions);
            }
        });
    }

    private TimetableDraft persistPartialDraft(GenerationRequest request, CSPState state,
                                               SchedulingInput input, int totalRequired) {
        List<SoftViolation> violations = softOptimizer.identifyViolations(state, input);
        TimetableDraft draft = resultPersister.persistResults(request, state, violations, input, totalRequired);
        draft.setIsPartial(true);
        draftRepository.save(draft);
        return draft;
    }

    private void persistUnplacedSessions(Long draftId, CSPState state, List<SessionVariable> variables, SchedulingInput input) {
        for (int i = 0; i < state.getVariableCount(); i++) {
            if (!state.isAssigned(i)) {
                SessionVariable var = variables.get(i);
                UnplacedSession unplaced = new UnplacedSession();
                unplaced.setDraftId(draftId);
                unplaced.setCourseId(var.getCourseId());
                unplaced.setCourseCode(input.getCourseCode(var.getCourseId()));
                unplaced.setCourseName(input.getCourseName(var.getCourseId()));
                unplaced.setFacultyId(var.getFacultyId());
                unplaced.setFacultyName(input.getFacultyName(var.getFacultyId()));
                unplaced.setBatchId(var.getBatchId());
                unplaced.setBatchName(input.getBatchName(var.getBatchId()));
                unplaced.setSessionType(var.getSessionType().name());
                unplaced.setRequiredDurationMinutes(var.getRequiredDurationMinutes());
                unplaced.setReason("Could not be placed during generation");
                unplacedSessionRepository.save(unplaced);
            }
        }
    }

    private void persistInfeasibilityReport(Long requestId, List<InfeasibilityEntry> entries) {
        InfeasibilityReport report = new InfeasibilityReport();
        report.setGenerationRequestId(requestId);
        report.setDetectedAt(LocalDateTime.now());
        report.setSummary(String.format("Infeasibility detected: %d session(s) cannot be placed", entries.size()));

        for (InfeasibilityEntry entry : entries) {
            InfeasibilityConflict conflict = new InfeasibilityConflict();
            conflict.setReport(report);
            conflict.setAffectedSessionDescription(entry.sessionDescription());
            conflict.setConflictingConstraints(String.join(", ", entry.constraintTypes()));
            conflict.setExplanation(entry.explanation());
            report.getConflicts().add(conflict);
        }

        infeasibilityReportRepository.save(report);
        log.info("Infeasibility report persisted: requestId={}, conflicts={}", requestId, entries.size());
    }

    private void updateProgress(Long requestId, GenerationPhase phase, int progress) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setPhase(phase);
            r.setProgress(progress);
            requestRepository.save(r);
        });
    }

    private void updateTotalSessions(Long requestId, int totalSessions) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setTotalSessions(totalSessions);
            requestRepository.save(r);
        });
    }

    private void completeRequest(Long requestId, Long draftId, long elapsedMs) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setStatus(GenerationStatus.COMPLETED);
            r.setDraftId(draftId);
            r.setElapsedMs(elapsedMs);
            r.setCompletedAt(LocalDateTime.now());
            r.setProgress(100);
            requestRepository.save(r);
        });
    }

    private void timeoutRequest(Long requestId, Long draftId, long elapsedMs) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setStatus(GenerationStatus.TIMED_OUT);
            r.setDraftId(draftId);
            r.setElapsedMs(elapsedMs);
            r.setCompletedAt(LocalDateTime.now());
            r.setProgress(100);
            r.setErrorMessage("Generation timed out after " + (elapsedMs / 1000) + "s. Partial solution saved.");
            requestRepository.save(r);
        });
    }

    private void infeasibleRequest(Long requestId, Long draftId, long elapsedMs) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setStatus(GenerationStatus.INFEASIBLE);
            r.setDraftId(draftId);
            r.setElapsedMs(elapsedMs);
            r.setCompletedAt(LocalDateTime.now());
            r.setProgress(100);
            r.setErrorMessage("No valid complete solution exists. See infeasibility report for details.");
            requestRepository.save(r);
        });
    }

    private void cancelledRequest(Long requestId, Long draftId, long elapsedMs) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setStatus(GenerationStatus.CANCELLED);
            r.setDraftId(draftId);
            r.setElapsedMs(elapsedMs);
            r.setCompletedAt(LocalDateTime.now());
            r.setProgress(100);
            r.setErrorMessage("Generation cancelled by user. Partial solution saved.");
            requestRepository.save(r);
        });
    }

    private void failRequest(Long requestId, String errorMessage) {
        requestRepository.findById(requestId).ifPresent(r -> {
            r.setStatus(GenerationStatus.FAILED);
            r.setErrorMessage(errorMessage != null ? errorMessage.substring(0, Math.min(errorMessage.length(), 2000)) : "Unknown error");
            r.setCompletedAt(LocalDateTime.now());
            requestRepository.save(r);
        });
    }

    private Long computeDefaultSeed(Long deptId, String semester) {
        long minuteTs = System.currentTimeMillis() / 60000;
        return (deptId * 31 + semester.hashCode()) * 37 + minuteTs;
    }
}
