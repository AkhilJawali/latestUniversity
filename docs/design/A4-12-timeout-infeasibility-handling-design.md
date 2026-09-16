# Design Document — Timetable Generation: Timeout and Infeasibility Handling

## 1. Overview

This design extends the A4-11 timetable generation engine with three capabilities: (1) timeout enforcement with partial solution delivery, (2) infeasibility detection with actionable reporting, and (3) enhanced progress reporting including cancellation support. It modifies the existing `ConstraintSolver`, `SchedulingEngineService`, and `GenerationRequest` entity — not replacing them.

**Source Requirement:** `docs/requirements/A4-12-timeout-infeasibility-handling-requirements.md` (v2, Approved)

**Not in scope:** The generation engine itself (A4-11), fortnightly patterns (A4-13), locked slots (A4-14), drag-drop (A4-15), conflict detection (A4-16).

## 2. Architecture

```
Controller Layer
    └── SchedulingController (extended)
         │  POST /generate/{requestId}/cancel   (NEW — FR-6)
         │  GET  /generate/{requestId}/status   (EXTENDED — FR-4 progress)
         │  GET  /generate/{requestId}/infeasibility  (NEW — FR-3)
         │
Service Layer
    ├── SchedulingEngineService (MODIFIED — timeout handling, cancellation, infeasibility flow)
    ├── InfeasibilityCollector (NEW — collects empty-domain reasons during propagation/backtracking)
    └── SchedulingResultPersister (MODIFIED — persist partial on timeout/infeasibility)
         │
Solver Layer
    ├── ConstraintSolver (MODIFIED — cancel flag, timeout return type, infeasibility collection)
    └── SolverResult (EXTENDED — reason enum: COMPLETE, TIMED_OUT, INFEASIBLE, CANCELLED)
         │
Entity Layer
    ├── GenerationRequest (MODIFIED — new status values, bestSoFarCount, totalSessions)
    ├── InfeasibilityReport (NEW entity)
    └── InfeasibilityConflict (NEW entity)
         │
Repository Layer
    ├── InfeasibilityReportRepository (NEW)
    └── InfeasibilityConflictRepository (NEW)
         │
DTO Layer
    ├── GenerationStatusDto (MODIFIED — bestSoFarCount, cancellable flag)
    ├── InfeasibilityReportDto (NEW)
    └── InfeasibilityConflictDto (NEW)
```

## 3. Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-55 | Add `TIMED_OUT` and `INFEASIBLE` to `GenerationStatus` enum. Existing `FAILED` (unexpected errors) and `CANCELLED` remain unchanged. | FR-5.1: distinguish timeout (partial available) from failure (crash) and infeasibility (proven impossible). | Existing `failRequest()` method for FAILED unchanged. New `timeoutRequest()` and `infeasibleRequest()` methods added. |
| KD-56 | Cancellation via `volatile boolean cancelFlag` on a per-request `SolverContext` object (not a shared field). Solver checks flag at each backtracking iteration. | Thread-safe. Per-request isolation. Volatile guarantees visibility across threads without locks. `Thread.interrupt()` would complicate checkpoint persistence. | `cancelFlag` checked alongside timeout check in backtrack loop. |
| KD-57 | Progress reports `bestSoFarCount` (monotonically non-decreasing) not current assignment count. Stored in `GenerationRequest.bestSoFarCount` column, updated when checkpoint improves. | FR-4.2: backtracking count rises/falls during unwinding. bestSoFar is meaningful and monotonic — prevents confusing progress bar. | Checkpoint update in backtrack also writes to DB via lightweight repo call. |
| KD-58 | Infeasibility stored in separate `infeasibility_reports` + `infeasibility_conflicts` tables (not JSONB on GenerationRequest). | Queryable, indexable, extensible. Individual conflicts can be addressed/resolved independently. JSONB would be opaque to SQL queries. | One report per GenerationRequest (1:1). Multiple conflicts per report (1:N). |

## 4. Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-75 | Infeasibility detail: entity-level (option b) — names the specific session, faculty, room, batch, and constraint types that conflict. NOT MUS (too expensive). | OQ#1 | Entity-level | MUS is NP-hard for general CSPs. Entity-level is achievable by recording WHY a domain became empty (which constraints pruned all values). Actionable without requiring algorithm expertise. |
| PD-76 | Timeout duration configurable via `scheduling.engine.timeout-seconds` property (application.yml). Default: 120. Min: 30. Max: 600. | OQ#2 | Configurable, default 120 | BRD says "within 2 minutes" — we default to that. Larger departments may legitimately need more. Config-driven, not per-request (avoids abuse). |
| PD-77 | On infeasibility, persist a partial draft of sessions placed before detection + the infeasibility report. | OQ#3 | Persist partial | Coordinator can work with placed sessions, manually handle the infeasible ones. Better than losing all work. Draft status = TIMED_OUT-equivalent (incomplete). |
| PD-78 | Progress reporting: bestSoFarCount + totalSessions + elapsedSeconds + current phase. No estimated time remaining (unreliable for CSP). | OQ#4 | Count + phase + time | Estimate would be misleading (CSP progress is non-linear). Four fields give enough visibility. |
| PD-79 | Cancellation supported in this story. Implemented via volatile flag + new endpoint. | OQ#5 | Supported | Low implementation cost (one volatile + one endpoint). High UX value when data mistake discovered mid-generation. |
| PD-80 | Partial drafts (from timeout) CAN enter approval workflow. Coordinator explicitly acknowledges incompleteness before submission. | OQ#6 | Allow with acknowledgment | Sometimes 90% is good enough. Blocking approval forces manual completion which may not be needed. Approval UI can show feasibility score. |
| PD-81 | Timer starts when `executeGeneration()` begins (before data loading). Aligns with A4-11's existing `startTime = System.currentTimeMillis()`. Includes loading time in the 2-minute budget. | OQ#7 | Include loading | A4-11 already does this. BRD says "within 2 minutes" without exclusion. Loading typically takes < 5 seconds — not a practical concern. Changing now would require modifying A4-11. |

## 5. API Design

### New Endpoints

| Method | Path | Description | Auth | Traces To |
|---|---|---|---|---|
| POST | `/api/v1/timetables/generate/{requestId}/cancel` | Cancel in-progress generation | COORDINATOR (own dept) / HOD / REGISTRAR | FR-6 |
| GET | `/api/v1/timetables/generate/{requestId}/infeasibility` | Get infeasibility report | COORDINATOR+ | FR-3 |
| GET | `/api/v1/timetables/{draftId}/unplaced` | Get unplaced sessions list (only for partial drafts) | COORDINATOR+ | FR-2.5 |

### Modified Endpoints

| Method | Path | Change | Traces To |
|---|---|---|---|
| GET | `/api/v1/timetables/generate/{requestId}/status` | Response now includes `bestSoFarCount`, `totalSessions`, `cancellable` flag. Status can be TIMED_OUT / INFEASIBLE / CANCELLED. | FR-4, FR-5 |

### Response Examples

**GET /generate/{requestId}/status — IN_PROGRESS:**
```json
{
  "requestId": 42,
  "status": "IN_PROGRESS",
  "progress": 55,
  "phase": "SOLVING",
  "bestSoFarCount": 28,
  "totalSessions": 48,
  "elapsedSeconds": 67,
  "cancellable": true,
  "departmentId": 3,
  "semester": "odd-2025"
}
```

**GET /generate/{requestId}/status — TIMED_OUT:**
```json
{
  "requestId": 42,
  "status": "TIMED_OUT",
  "progress": 100,
  "phase": "STORING_RESULTS",
  "bestSoFarCount": 35,
  "totalSessions": 48,
  "elapsedSeconds": 121,
  "cancellable": false,
  "departmentId": 3,
  "semester": "odd-2025",
  "draftId": 17,
  "feasibilityScore": 0.729,
  "qualityScore": 0.85,
  "placedSessions": 35,
  "draftUrl": "/api/v1/timetables/17"
}
```

**GET /generate/{requestId}/status — INFEASIBLE:**
```json
{
  "requestId": 42,
  "status": "INFEASIBLE",
  "progress": 100,
  "phase": "STORING_RESULTS",
  "bestSoFarCount": 22,
  "totalSessions": 48,
  "elapsedSeconds": 14,
  "cancellable": false,
  "departmentId": 3,
  "semester": "odd-2025",
  "draftId": 18,
  "feasibilityScore": 0.458,
  "infeasibilityUrl": "/api/v1/timetables/generate/42/infeasibility"
}
```

**GET /generate/{requestId}/infeasibility:**
```json
{
  "reportId": 5,
  "generationRequestId": 42,
  "detectedAt": "2026-08-27T10:02:14",
  "summary": "2 sessions could not be placed due to constraint conflicts",
  "conflicts": [
    {
      "id": 11,
      "affectedSession": "CS301 Lecture — Dr. Sharma — Batch 3A",
      "conflictingConstraints": ["FACULTY_DOUBLE_BOOKING", "ROOM_CAPACITY"],
      "explanation": "Dr. Sharma is unavailable Mon/Wed/Fri 9:00-12:00 (hard block). The only remaining slots (Tue/Thu afternoon) have no room with capacity >= 60 and projector equipment."
    },
    {
      "id": 12,
      "affectedSession": "CS301 Tutorial — Dr. Sharma — Batch 3A",
      "conflictingConstraints": ["FACULTY_DOUBLE_BOOKING"],
      "explanation": "All valid slots for this session conflict with Dr. Sharma's other assigned sessions (CS201 Batch 2A, CS401 Batch 4A) given the availability block above."
    }
  ]
}
```

**POST /generate/{requestId}/cancel — 200 OK:**
```json
{
  "requestId": 42,
  "status": "CANCELLED",
  "message": "Generation cancelled. Partial result with 28/48 sessions available.",
  "draftId": 19
}
```

**POST /generate/{requestId}/cancel — 200 (already terminal, idempotent):**
```json
{
  "requestId": 42,
  "status": "COMPLETED",
  "message": "Generation already in terminal state: COMPLETED. No action taken.",
  "draftId": 17
}
```

## 6. Data Model

### Migration V11

```sql
-- V11__timeout_infeasibility.sql

-- Add new columns to generation_requests
ALTER TABLE generation_requests ADD COLUMN best_so_far_count INTEGER DEFAULT 0;
ALTER TABLE generation_requests ADD COLUMN total_sessions INTEGER;
ALTER TABLE generation_requests ADD COLUMN timeout_duration_seconds INTEGER DEFAULT 120;

-- Mark partial drafts explicitly (FR-1.4, PD-80)
ALTER TABLE timetable_drafts ADD COLUMN is_partial BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE timetable_drafts ADD COLUMN partial_acknowledged BOOLEAN NOT NULL DEFAULT false;

-- Infeasibility reports
CREATE TABLE infeasibility_reports (
    id BIGSERIAL PRIMARY KEY,
    generation_request_id BIGINT NOT NULL REFERENCES generation_requests(id),
    detected_at TIMESTAMP NOT NULL,
    summary TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_infeasibility_report_request FOREIGN KEY (generation_request_id) REFERENCES generation_requests(id)
);

CREATE UNIQUE INDEX uk_infeasibility_report_request ON infeasibility_reports(generation_request_id) WHERE deleted_at IS NULL;

-- Infeasibility conflicts (one per unplaceable session)
CREATE TABLE infeasibility_conflicts (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES infeasibility_reports(id),
    affected_session_description VARCHAR(500) NOT NULL,
    conflicting_constraints TEXT NOT NULL,
    explanation TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_infeasibility_conflict_report FOREIGN KEY (report_id) REFERENCES infeasibility_reports(id)
);

CREATE INDEX idx_infeasibility_conflicts_report ON infeasibility_conflicts(report_id);

-- Unplaced sessions (FR-2.5: persisted on timeout/infeasibility for coordinator visibility)
CREATE TABLE unplaced_sessions (
    id BIGSERIAL PRIMARY KEY,
    draft_id BIGINT NOT NULL REFERENCES timetable_drafts(id),
    course_id BIGINT NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    faculty_id BIGINT NOT NULL,
    faculty_name VARCHAR(200) NOT NULL,
    batch_id BIGINT NOT NULL,
    batch_name VARCHAR(100) NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    required_duration_minutes INTEGER NOT NULL,
    reason VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_unplaced_sessions_draft FOREIGN KEY (draft_id) REFERENCES timetable_drafts(id)
);

CREATE INDEX idx_unplaced_sessions_draft ON unplaced_sessions(draft_id);
```

**Note:** `conflicting_constraints` stored as comma-separated constraint type names (e.g., "FACULTY_DOUBLE_BOOKING,ROOM_CAPACITY"). Returned as a list in the DTO via split.

### Entity Changes

**GenerationStatus enum (modified):**
```java
public enum GenerationStatus {
    IN_PROGRESS,
    COMPLETED,
    TIMED_OUT,    // NEW
    INFEASIBLE,   // NEW
    FAILED,
    CANCELLED
}
```

**GenerationRequest entity (extended columns):**
- `bestSoFarCount` (Integer) — monotonically non-decreasing best checkpoint count
- `totalSessions` (Integer) — set after session derivation
- `timeoutDurationSeconds` (Integer, default 120) — configurable per run

## 7. Service Logic

### 7.1 SolverContext (NEW)

A per-request context object passed into the solver, carrying timeout config and the cancel flag:

```java
public class SolverContext {
    private final long startTimeMs;
    private final long timeoutMs;
    private volatile boolean cancelRequested;
    private final Long requestId;

    public boolean isTimedOut() { return System.currentTimeMillis() - startTimeMs > timeoutMs; }
    public boolean isCancelRequested() { return cancelRequested; }
    public void requestCancel() { this.cancelRequested = true; }
    public long elapsedMs() { return System.currentTimeMillis() - startTimeMs; }
}
```

### 7.2 Modified SchedulingEngineService.executeGeneration()

```
1. Create SolverContext(startTime, timeoutMs from config)
2. Store SolverContext in a ConcurrentHashMap<Long, SolverContext> keyed by requestId
3. Load data (phase: LOADING_DATA)
4. Derive sessions → set totalSessions on GenerationRequest
5. Propagate (phase: PROPAGATING)
   - If propagation finds empty domain → collect infeasibility, go to step 8
6. Solve (phase: SOLVING)
   - Returns SolverResult with reason: COMPLETE / TIMED_OUT / INFEASIBLE / CANCELLED
7. If COMPLETE: optimize (phase: OPTIMIZING). If not: skip optimization.
8. Persist results (phase: STORING_RESULTS):
   - COMPLETE → persist full draft, status = COMPLETED
   - TIMED_OUT → persist partial draft (bestPartial), status = TIMED_OUT
   - INFEASIBLE → persist partial draft + infeasibility report, status = INFEASIBLE
   - CANCELLED → persist partial draft, status = CANCELLED
9. Audit event for non-COMPLETED outcomes
10. Remove SolverContext from map
```

### 7.3 Modified ConstraintSolver.solve()

The backtracking loop now checks two stop conditions (timeout and cancel). Infeasibility is NOT declared mid-search — an empty domain during forward-checking is a normal dead-end handled by backtracking (try sibling branches). Infeasibility is only proven in two places: (a) during initial AC-3 propagation (before search begins) if a domain is permanently pruned to empty, or (b) after root-level exhaustion (all branches explored, no solution found, and timeout not reached).

```java
// In backtrack loop — these cause early termination with partial:
if (context.isTimedOut()) return SolverResult(bestPartial, SolverOutcome.TIMED_OUT);
if (context.isCancelRequested()) return SolverResult(bestPartial, SolverOutcome.CANCELLED);

// Empty domain mid-search is NOT infeasibility — it's a normal dead-end:
if (varIndex < 0 || domain.isEmpty()) return false; // triggers backtrack to parent

// Infeasibility is declared ONLY at root-level exhaustion:
// (called from solve() wrapper when backtrack() returns false AND timeout not reached)
if (!solved && !context.isTimedOut() && !context.isCancelRequested()) {
    // All branches exhausted without solution = proven infeasible
    return SolverResult(bestPartial, SolverOutcome.INFEASIBLE, infeasibilityCollector.getEntries());
}
```

**When infeasibility is collected:** The InfeasibilityCollector records entries when:
1. **During propagation (pre-search):** A domain becomes permanently empty — records which constraints pruned all values for that variable. This is immediate and cheap.
2. **After root-level exhaustion:** The solver identifies the variable(s) with smallest domain that most frequently caused dead-ends during the search (tracked via a dead-end counter per variable). The top-N most problematic variables are reported with their constraint reasons.

### 7.4 InfeasibilityCollector (NEW)

Collects WHY a session's domain became empty:

```java
@Slf4j
public class InfeasibilityCollector {
    private final List<InfeasibilityEntry> entries = new ArrayList<>();

    public void record(SessionVariable variable, List<String> constraintsThatPruned) {
        entries.add(new InfeasibilityEntry(
            describeSession(variable),
            constraintsThatPruned,
            buildExplanation(variable, constraintsThatPruned)
        ));
    }

    public boolean hasEntries() { return !entries.isEmpty(); }
    public List<InfeasibilityEntry> getEntries() { return Collections.unmodifiableList(entries); }
}
```

The constraint types that pruned are collected during `forwardCheck()` and `propagateArcs()` — when a value is removed from a domain, the removing constraint is tagged.

### 7.5 Progress Update Enhancement

Existing `updateProgress()` enhanced to also write `bestSoFarCount`:

```java
private void updateBestSoFar(Long requestId, int bestCount) {
    requestRepository.findById(requestId).ifPresent(r -> {
        if (bestCount > r.getBestSoFarCount()) {
            r.setBestSoFarCount(bestCount);
            requestRepository.save(r);
        }
    });
}
```

Called from within the solver when the checkpoint improves (new callback interface):

```java
public interface ProgressCallback {
    void onCheckpointImproved(int newBestCount);
}
```

**Batching (OQ-D4):** To avoid excessive DB writes during fast early placement, updates are throttled — only written when improvement exceeds 5% of totalSessions (i.e., at most ~20 writes per generation).

### 7.6 Cancellation Flow

```
1. POST /generate/{requestId}/cancel received
2. Controller looks up SolverContext from ConcurrentHashMap
3. If found and status is IN_PROGRESS: context.requestCancel()
4. If not found or already terminal: return 200 with current terminal state (idempotent per FR-6.4)
5. Return 200 with current bestSoFarCount info
6. Solver detects cancelRequested → returns CANCELLED result
7. executeGeneration() persists partial + transitions status
```

**Idempotency (FR-6.4):** Cancelling an already-terminal request returns 200 with the current state — NOT 409. This makes the cancel endpoint a true no-op on repeated calls.

### 7.7 Timeout Flow

```
1. Solver checks isTimedOut() at each backtracking iteration
2. Also checked at optimization loop boundary
3. On timeout: return bestPartial state immediately
4. executeGeneration() receives TIMED_OUT result
5. Persist partial draft (same as COMPLETED path, but feasibility < 1.0)
6. Set status = TIMED_OUT, record elapsed, bestSoFarCount
7. Audit event
```

### 7.8 Infeasibility Flow

```
1. During propagation: if a domain becomes empty, record which constraints caused it
2. During backtracking: if MRV variable has empty domain after all branches exhausted, record
3. Return INFEASIBLE result with collected entries
4. executeGeneration(): persist partial draft + InfeasibilityReport + InfeasibilityConflicts
5. Set status = INFEASIBLE
6. Audit event with constraint summary
```

**Early detection:** If infeasibility is detected during propagation (step 1), the engine returns immediately — does NOT wait for the full timeout. (NFR: infeasibility detection speed)

## 8. Cross-Cutting Concerns

| Concern | Design |
|---|---|
| Audit | Audit events for TIMED_OUT, INFEASIBLE, CANCELLED, and FAILED within SchedulingResultPersister @Transactional (same pattern as KD-54). Includes: requestId, departmentId, outcome, bestSoFarCount/totalSessions, elapsedMs, triggeredBy. |
| Security | Cancel endpoint: same auth as trigger (COORDINATOR own dept / HOD / REGISTRAR). Infeasibility endpoint: same as status (COORDINATOR+). |
| Error handling | Cancel on non-existent request → 404. Infeasibility on non-INFEASIBLE request → 404. Cancel on terminal request → 200 (idempotent, returns current state). Same GlobalExceptionHandler envelope for actual errors. |
| Configuration | `scheduling.engine.timeout-seconds` in application.yml (default: 120, min: 30, max: 600). Validated at startup via @ConfigurationProperties. |
| Thread safety | SolverContext per request in ConcurrentHashMap. volatile cancelFlag. No shared mutable state between requests. |
| Stale request reaper | A @Scheduled task runs every 60 seconds. Finds any `GenerationRequest` with status = IN_PROGRESS AND `triggered_at < NOW() - (timeout_duration_seconds + 120s margin)`. Transitions those to FAILED with error_message = "Generation orphaned (process died)". This unblocks the department's partial unique index (PD-68) so future generations are not permanently bricked by a JVM crash. |

## 9. Integration Contracts

| Service/Component | Method/Interface | Contract | Direction |
|---|---|---|---|
| SchedulingResultPersister | persistPartialResults(request, state, violations, input, totalSessions) | Same as full persist but with feasibility < 1.0. Also persists InfeasibilityReport if applicable. | Internal |
| AuditEventPublisher | publish(AuditEvent) | Same contract as A4-2. Events: GENERATION_TIMED_OUT, GENERATION_INFEASIBLE, GENERATION_CANCELLED, GENERATION_FAILED. | Outbound |
| GenerationStatusDto | Extended with bestSoFarCount, totalSessions, cancellable, infeasibilityUrl | Frontend polls this. | Response |
| ProgressCallback | onCheckpointImproved(int) | Called by solver when bestPartial improves. Implementor writes to DB. | Callback |

## 10. Testing Strategy

| Scenario | Type | Approach |
|---|---|---|
| Timeout returns partial after configured duration | Integration | Set timeout to 2s, provide complex input. Assert status = TIMED_OUT, feasibility < 1.0, draft exists. |
| Infeasibility detected and reported | Unit + Integration | Create input with impossible constraints (faculty with zero available slots). Assert status = INFEASIBLE, report contains correct session and constraints. |
| Early infeasibility (during propagation) | Unit | Domain pruned to empty during AC-3. Assert returns < timeout duration with INFEASIBLE. |
| Progress reporting bestSoFarCount | Unit | Mock solver progress. Assert bestSoFarCount monotonically non-decreasing in status response. |
| Cancellation stops solver | Integration | Trigger generation, immediately cancel. Assert status = CANCELLED within a few seconds. |
| Cancel on terminal request → 409 | Unit | Already-completed request. Assert 409 response. |
| Partial draft has zero hard violations | Property test | For any partial result, verify no HC-ENG-1..12 violations among placed sessions (same validator). |
| Audit events recorded | Integration | Trigger timeout scenario. Assert audit_events table has entry with correct type and context. |

## 11. Provisional Decisions Summary

PD-75 through PD-81 (see table in Section 4). All pending stakeholder ratification.

## 12. Open Questions (Design-Level)

| # | Question | Owner | Status |
|---|---|---|---|
| OQ-D4 | Should bestSoFarCount progress updates be batched (every N improvements) to reduce DB writes during fast early placement? Practical concern if 100+ sessions placed in first seconds. | System Design | Provisional: update only when improvement > 5% of total (reduces writes from O(N) to O(20)). |
| OQ-D5 | Should the ConcurrentHashMap of SolverContexts be bounded? If many departments generate simultaneously and some leak (thread dies without cleanup), the map grows. | System Design | Provisional: cleanup in a @Scheduled task every 5 minutes (remove entries older than timeout + 60s). |

## 13. Consistency Notes

- Migration V11 follows V10 (A4-11). PD-75-81. KD-55-58.
- `GenerationStatus` enum now has 6 values: IN_PROGRESS, COMPLETED, TIMED_OUT, INFEASIBLE, FAILED, CANCELLED.
- Existing `failRequest()` method unchanged — still handles unexpected exceptions.
- `TIMEOUT_MS` constant in ConstraintSolver replaced by `context.getTimeoutMs()` (from config).
- bestSoFarCount in entity parallels the in-memory `bestCount[0]` in solver — they are synced via ProgressCallback.
- Cancel endpoint returns 200 always (idempotent per FR-6.4). If in-progress: cancellation initiated. If already terminal: returns current state, no action. Final status visible via GET /status.
- InfeasibilityReport is 1:1 with GenerationRequest (enforced by unique index).

## 14. Traceability

| Requirement | Design Element |
|---|---|
| FR-1.1 (time limit enforcement) | SolverContext.isTimedOut() check in backtrack loop |
| FR-1.2 (2 min default, configurable) | PD-76, scheduling.engine.timeout-seconds config |
| FR-1.3 (clock start point) | PD-81, aligned with A4-11 startTime |
| FR-1.4 (persist partial on timeout) | Section 7.7, SchedulingResultPersister |
| FR-2.1 (best partial) | KD-57, bestPartial checkpoint |
| FR-2.2 (placed vs unplaced indication) | `is_partial` flag on TimetableDraft + feasibility < 1.0 + unplaced_sessions table |
| FR-2.3 (feasibility score) | Same formula as A4-11 |
| FR-2.4 (quality score for placed) | SoftConstraintOptimizer on partial state |
| FR-2.5 (unplaced session list) | `unplaced_sessions` table + GET /timetables/{draftId}/unplaced endpoint. Persisted during result storage with course/faculty/batch context denormalized. |
| FR-3.1 (detect infeasibility) | Section 7.8, empty domain detection |
| FR-3.2 (report with constraints) | InfeasibilityCollector, PD-75 entity-level |
| FR-3.3 (actionable report) | InfeasibilityConflict.explanation field |
| FR-3.4 (INFEASIBLE status) | KD-55 enum extension |
| FR-3.5 (persist partial on infeasibility) | PD-77 |
| FR-4.1 (progress via polling) | Extended GenerationStatusDto |
| FR-4.2 (bestSoFarCount) | KD-57, ProgressCallback |
| FR-4.3 (update frequency) | OQ-D4 batching |
| FR-4.4 (phase indication) | Existing GenerationPhase enum, already in status |
| FR-5.1-5.6 (status states) | KD-55 |
| FR-6.1-6.4 (cancellation) | KD-56, Section 7.6 |
| FR-7.1-7.3 (audit) | Section 8, AuditEventPublisher |
| TIMEOUT-1 (never run indefinitely) | SolverContext + check in every iteration |
| TIMEOUT-2 (partial has zero HC violations) | bestPartial only contains validated assignments |
| TIMEOUT-3 (progress visibility) | bestSoFarCount + phase + elapsed in status response |
