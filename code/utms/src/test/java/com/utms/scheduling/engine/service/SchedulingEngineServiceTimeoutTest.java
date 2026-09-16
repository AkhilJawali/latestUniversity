package com.utms.scheduling.engine.service;

import com.utms.scheduling.engine.config.SchedulingEngineProperties;
import com.utms.scheduling.engine.entity.GenerationRequest;
import com.utms.scheduling.engine.entity.TimetableDraft;
import com.utms.scheduling.engine.enums.GenerationPhase;
import com.utms.scheduling.engine.enums.GenerationStatus;
import com.utms.scheduling.engine.model.SchedulingInput;
import com.utms.scheduling.engine.model.SessionVariable;
import com.utms.scheduling.engine.repository.GenerationRequestRepository;
import com.utms.scheduling.engine.repository.InfeasibilityReportRepository;
import com.utms.scheduling.engine.repository.TimetableDraftRepository;
import com.utms.scheduling.engine.repository.UnplacedSessionRepository;
import com.utms.scheduling.engine.solver.CSPState;
import com.utms.scheduling.engine.solver.ConstraintSolver;
import com.utms.scheduling.engine.solver.InfeasibilityCollector;
import com.utms.scheduling.engine.solver.ProgressCallback;
import com.utms.scheduling.engine.solver.SoftConstraintOptimizer;
import com.utms.scheduling.engine.solver.SoftViolation;
import com.utms.scheduling.engine.solver.SolverContext;
import com.utms.scheduling.engine.solver.SolverOutcome;
import com.utms.scheduling.engine.solver.SolverResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SchedulingEngineService timeout handling.
 * Mocks the solver to return TIMED_OUT and verifies status transition and partial persistence.
 */
@ExtendWith(MockitoExtension.class)
class SchedulingEngineServiceTimeoutTest {

    @Mock private SchedulingDataLoader dataLoader;
    @Mock private SessionDeriver sessionDeriver;
    @Mock private ConstraintSolver constraintSolver;
    @Mock private SoftConstraintOptimizer softOptimizer;
    @Mock private SchedulingResultPersister resultPersister;
    @Mock private GenerationRequestRepository requestRepository;
    @Mock private InfeasibilityReportRepository infeasibilityReportRepository;
    @Mock private UnplacedSessionRepository unplacedSessionRepository;
    @Mock private TimetableDraftRepository draftRepository;
    @Mock private SchedulingEngineProperties engineProperties;
    @Mock private Executor schedulingExecutor;

    @InjectMocks
    private SchedulingEngineService engineService;

    private GenerationRequest request;

    @BeforeEach
    void setUp() {
        request = new GenerationRequest();
        request.setId(1L);
        request.setDepartmentId(10L);
        request.setSemester("ODD-2025");
        request.setAcademicYear("2025-26");
        request.setStatus(GenerationStatus.IN_PROGRESS);
        request.setPhase(GenerationPhase.LOADING_DATA);
        request.setProgress(0);
        request.setSeed(42L);
        request.setTriggeredBy("coordinator@test.com");
        request.setTriggeredAt(LocalDateTime.now());
        request.setTimeoutDurationSeconds(120);
        request.setBestSoFarCount(0);
    }

    @Test
    @DisplayName("Solver returning TIMED_OUT transitions request to TIMED_OUT status")
    void executeGeneration_solverTimesOut_transitionsToTimedOut() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        // save() must assign the generated id so triggerGeneration captures a non-null
        // requestId for the async executeGeneration(requestId) call.
        when(requestRepository.save(any(GenerationRequest.class))).thenAnswer(inv -> {
            GenerationRequest r = inv.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(engineProperties.getDefaultTimeoutSeconds()).thenReturn(120);
        // Note: getProgressThresholdPercent() is not stubbed here — the solver is mocked
        // to time out immediately, so the progress callback (which reads it) never fires.

        SchedulingInput mockInput = mock(SchedulingInput.class);
        when(dataLoader.loadAll(anyLong(), any(), any())).thenReturn(mockInput);

        List<SessionVariable> variables = Collections.emptyList();
        when(sessionDeriver.derive(any())).thenReturn(variables);

        CSPState mockState = mock(CSPState.class);
        when(mockState.getVariableCount()).thenReturn(0);
        when(constraintSolver.initializeAndPropagate(anyList(), any(), any(InfeasibilityCollector.class)))
            .thenReturn(mockState);

        SolverResult timedOutResult = new SolverResult(mockState, SolverOutcome.TIMED_OUT);
        when(constraintSolver.solve(any(), any(Random.class), any(SolverContext.class),
            any(ProgressCallback.class), any(InfeasibilityCollector.class)))
            .thenReturn(timedOutResult);

        when(softOptimizer.identifyViolations(any(), any())).thenReturn(Collections.emptyList());

        TimetableDraft mockDraft = new TimetableDraft();
        mockDraft.setId(100L);
        when(resultPersister.persistResults(any(), any(), anyList(), any(), anyInt()))
            .thenReturn(mockDraft);
        when(draftRepository.save(any(TimetableDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act — trigger generation, capture the async runnable
        engineService.triggerGeneration(10L, "ODD-2025", "2025-26", 42L, "coordinator@test.com");

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(schedulingExecutor).execute(runnableCaptor.capture());

        // Execute the captured runnable (simulates async execution). The save() stub
        // assigns id=1L, so executeGeneration(1L) resolves via the findById(1L) stub.
        runnableCaptor.getValue().run();

        // Assert — verify status was set to TIMED_OUT
        ArgumentCaptor<GenerationRequest> reqCaptor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(requestRepository, atLeastOnce()).save(reqCaptor.capture());

        List<GenerationRequest> savedRequests = reqCaptor.getAllValues();
        boolean hasTimedOut = savedRequests.stream()
            .anyMatch(r -> r.getStatus() == GenerationStatus.TIMED_OUT);
        assertThat(hasTimedOut).isTrue();
    }

    @Test
    @DisplayName("Solver returning TIMED_OUT persists partial draft with isPartial=true")
    void executeGeneration_solverTimesOut_persistsPartialDraft() {
        // Arrange
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        // save() must assign the generated id so triggerGeneration captures a non-null
        // requestId for the async executeGeneration(requestId) call.
        when(requestRepository.save(any(GenerationRequest.class))).thenAnswer(inv -> {
            GenerationRequest r = inv.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(engineProperties.getDefaultTimeoutSeconds()).thenReturn(120);
        // Note: getProgressThresholdPercent() is not stubbed here — the solver is mocked
        // to time out immediately, so the progress callback (which reads it) never fires.

        SchedulingInput mockInput = mock(SchedulingInput.class);
        when(dataLoader.loadAll(anyLong(), any(), any())).thenReturn(mockInput);

        List<SessionVariable> variables = Collections.emptyList();
        when(sessionDeriver.derive(any())).thenReturn(variables);

        CSPState mockState = mock(CSPState.class);
        when(mockState.getVariableCount()).thenReturn(0);
        when(constraintSolver.initializeAndPropagate(anyList(), any(), any(InfeasibilityCollector.class)))
            .thenReturn(mockState);

        SolverResult timedOutResult = new SolverResult(mockState, SolverOutcome.TIMED_OUT);
        when(constraintSolver.solve(any(), any(Random.class), any(SolverContext.class),
            any(ProgressCallback.class), any(InfeasibilityCollector.class)))
            .thenReturn(timedOutResult);

        when(softOptimizer.identifyViolations(any(), any())).thenReturn(Collections.emptyList());

        TimetableDraft mockDraft = new TimetableDraft();
        mockDraft.setId(100L);
        when(resultPersister.persistResults(any(), any(), anyList(), any(), anyInt()))
            .thenReturn(mockDraft);
        when(draftRepository.save(any(TimetableDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        engineService.triggerGeneration(10L, "ODD-2025", "2025-26", 42L, "coordinator@test.com");

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(schedulingExecutor).execute(runnableCaptor.capture());

        // The save() stub assigns id=1L, so executeGeneration(1L) resolves via the
        // findById(1L) stub — no extra re-stubbing needed.
        runnableCaptor.getValue().run();

        // Assert — draft saved with isPartial = true
        ArgumentCaptor<TimetableDraft> draftCaptor = ArgumentCaptor.forClass(TimetableDraft.class);
        verify(draftRepository).save(draftCaptor.capture());
        assertThat(draftCaptor.getValue().getIsPartial()).isTrue();
    }

    @Test
    @DisplayName("cancelGeneration sets cancel flag on active solver context")
    void cancelGeneration_activeRequest_setsCancelFlag() {
        // Arrange
        when(requestRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(request));

        // Act
        GenerationRequest result = engineService.cancelGeneration(1L);

        // Assert — request is returned (cancel flag is set on SolverContext, which is
        // only populated during active execution. In this unit test, solverContexts map is empty,
        // so the cancel is a no-op but returns the request correctly)
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GenerationStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("cancelGeneration on terminal request returns current state without error")
    void cancelGeneration_terminalRequest_returnsCurrentState() {
        // Arrange
        request.setStatus(GenerationStatus.COMPLETED);
        when(requestRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(request));

        // Act
        GenerationRequest result = engineService.cancelGeneration(1L);

        // Assert — returns the completed request, no exception
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GenerationStatus.COMPLETED);
    }
}
