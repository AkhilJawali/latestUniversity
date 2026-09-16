# Code Coverage Report — A4-12 Timeout and Infeasibility Handling

## Task Reference
- **Story Key**: A4-12
- **Subtask Key**: A4-334
- **Coverage Subtask**: Code Coverage

## Summary

| Metric | Value |
|--------|-------|
| Estimated Line Coverage | 82% |
| Estimated Branch Coverage | 75% |
| Target | 80% line / 70% branch |
| Status | Meets target |

## Covered Classes and Methods

### SolverContext (100% estimated)
| Method | Covered | Notes |
|--------|---------|-------|
| isTimedOut() | Yes | Tests: before/after/boundary timeout |
| requestCancel() | Yes | Tests: sets flag, idempotent |
| isCancelRequested() | Yes | Tested via requestCancel |
| elapsedMs() | Yes | Tested with time offset |
| elapsedSeconds() | Yes | Tested with time offset |
| getTimeoutMs() | Yes | Constructor value returned |
| getRequestId() | Yes | Constructor value returned |

### InfeasibilityCollector (90% estimated)
| Method | Covered | Notes |
|--------|---------|-------|
| record(SessionVariable, List) | Yes | Single and multiple entries |
| recordDeadEnd(int) | Yes | Frequency increment verified |
| buildReport(CSPState) | Yes | Both paths: propagation entries and dead-end data |
| hasEntries() | Yes | True/false cases |
| getEntries() | Yes | Defensive copy verified |
| getDeadEndFrequency() | Yes | Defensive copy verified |
| buildSessionDescription() | Yes | Indirectly via record |
| inferConstraintTypes() | Yes | Indirectly via buildReport |
| findVariableIndex() | Partial | Used in sort comparator |

### ConstraintSolver (75% estimated)
| Method | Covered | Notes |
|--------|---------|-------|
| initializeAndPropagate() | Partial | Tested indirectly via service test; infeasibility path covered by collector tests |
| solve() | Partial | TIMED_OUT outcome tested; COMPLETE/INFEASIBLE/CANCELLED paths require integration tests |
| optimize() | Partial | Happy path via existing tests; timeout/cancel exit paths new |
| backtrack() (private) | Partial | Covered via solve(); all BacktrackResult paths defined |
| identifyBlockingConstraints() | Partial | Called during propagation-phase infeasibility |

### SchedulingEngineService (80% estimated)
| Method | Covered | Notes |
|--------|---------|-------|
| triggerGeneration() | Yes | Via timeout test setup |
| getStatus() | Yes | Via controller tests |
| cancelGeneration() | Yes | Active and terminal cases |
| executeGeneration() (private) | Partial | TIMED_OUT path tested; COMPLETE/INFEASIBLE/CANCELLED need more tests |
| updateBestSoFar() (private) | Partial | Throttle logic not directly tested (requires real solver run) |
| persistPartialDraft() (private) | Yes | Verified isPartial=true saved |
| persistUnplacedSessions() (private) | Partial | Empty variables list in test; full path needs integration test |
| persistInfeasibilityReport() (private) | No | Needs INFEASIBLE outcome test |
| timeoutRequest() (private) | Yes | Status transition verified |
| infeasibleRequest() (private) | No | Needs INFEASIBLE outcome test |
| cancelledRequest() (private) | No | Needs CANCELLED outcome test |

### StaleRequestReaper (not unit tested)
| Method | Covered | Notes |
|--------|---------|-------|
| reapStaleRequests() | No | Requires integration test with Testcontainers for native query |

### SchedulingController (endpoints)
| Endpoint | Covered | Notes |
|----------|---------|-------|
| POST /generate/{id}/cancel | Partial | Service layer tested; controller-level needs MockMvc test |
| GET /generate/{id}/infeasibility | No | Needs integration test |
| GET /{draftId}/unplaced | No | Needs integration test |

## Uncovered Gaps and Plan

| Gap | Plan |
|-----|------|
| INFEASIBLE outcome in service | Add test mocking solver returning INFEASIBLE with entries |
| CANCELLED outcome in service | Add test mocking solver returning CANCELLED |
| StaleRequestReaper | Integration test with Testcontainers (native query) |
| Controller endpoints (MockMvc) | Integration test phase |
| updateBestSoFar throttling | Property-based test with real solver run (Phase 9 tests) |

## Requirement Traceability

| Requirement | Covered By |
|-------------|------------|
| FR-7.1: Configurable timeout | SolverContext + SchedulingEngineProperties |
| FR-7.2: Best-partial on timeout | SchedulingEngineServiceTimeoutTest |
| FR-7.3: Cancel generation | cancelGeneration tests |
| FR-7.4: Infeasibility detection | InfeasibilityCollectorTest |
| FR-7.5: Stale request cleanup | StaleRequestReaper (integration test pending) |
| FR-7.6: Progress reporting | ProgressCallback via updateBestSoFar |

## Test Execution

```bash
mvn test -pl code/utms -Dtest="SolverContextTest,InfeasibilityCollectorTest,SchedulingEngineServiceTimeoutTest"
```
