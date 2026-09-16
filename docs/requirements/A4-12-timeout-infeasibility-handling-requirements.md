# Requirement Document — Timetable Generation: Timeout and Infeasibility Handling

## 1. Introduction

This document captures the requirements for handling two failure modes of the timetable generation engine: (1) when the engine exceeds the time limit without finding a complete solution, and (2) when the engine determines that no valid solution exists given the current constraints. It also covers progress reporting during generation so coordinators are not left waiting in a "black box."

This story EXTENDS A4-11 (Timetable Generation Engine) — it does not redefine the engine itself, but specifies the behaviors surrounding timeout, infeasibility, and progress visibility.

## 2. User Story

**A4-12:** As a Department Coordinator, I want the system to handle generation timeouts by returning the best partial solution found so far, and to clearly report when the problem is infeasible with an explanation of which constraints conflict, so that I am never stuck waiting indefinitely and always have actionable information to adjust inputs.

**Story Points:** 5

**BRD Requirements:** Section 8 — "Auto-generate a full department timetable within 2 minutes" (implies: what happens when it CAN'T complete in 2 minutes); 6.10 — (implied) engine must handle cases where no valid solution exists.

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Monitors generation progress, receives timeout/infeasibility results, adjusts inputs |
| Scheduling Engine (system) | Enforces time limit, detects infeasibility, reports progress, delivers partial solutions |
| GenerationRequest (entity from A4-11) | Extended with new terminal states (TIMED_OUT, INFEASIBLE) |
| Audit Trail (common) | Records timeout/infeasibility outcomes |

## 4. User Journeys

### Journey 1: Generation Times Out — Partial Solution Returned

**Before:**
- Coordinator has triggered generation (A4-11). Engine is running asynchronously.
- Engine is maintaining a best-partial-state checkpoint as it searches (A4-11 design, KD-45).

**During:**
1. Coordinator polls the status endpoint and sees progress information (best-so-far session count, time elapsed).
2. Engine reaches the time limit without placing all sessions.
3. Engine stops the search process.
4. Engine captures the best partial solution found (the checkpoint with the most sessions placed).
5. Engine persists the partial draft with a feasibility score < 1.0.
6. GenerationRequest status transitions to TIMED_OUT.
7. Coordinator is informed that generation timed out.
8. Coordinator views the partial draft and sees: which sessions were successfully placed, which sessions remain unplaced, and the feasibility/quality scores.

**After:**
- Coordinator can manually place remaining sessions via the editor (A4-15).
- Coordinator can adjust constraints and re-run generation (A4-11).
- Audit trail records the timeout event with context (sessions placed/total, time elapsed).

### Journey 2: Engine Detects Infeasibility

**Before:**
- Coordinator has triggered generation. Engine is running.
- During constraint propagation or exhaustive backtracking, the engine determines that no valid placement exists for one or more sessions.

**During:**
1. Engine detects infeasibility (a session's domain becomes permanently empty — no valid slot+room combination satisfies all hard constraints).
2. Engine identifies the conflicting constraints that caused infeasibility [see OQ#1 for detail level].
3. Engine generates an infeasibility report listing the affected sessions and the constraints that cannot be simultaneously satisfied.
4. GenerationRequest status transitions to INFEASIBLE.
5. No draft is produced (or a partial draft of successfully placed sessions is produced — [see OQ#3]).
6. Coordinator views the infeasibility report.

**After:**
- Coordinator uses the report to adjust inputs: add a room, relax a faculty constraint, split a batch, etc.
- Coordinator re-triggers generation after adjustments.
- Audit trail records infeasibility with constraint summary.

### Journey 3: Coordinator Monitors Progress

**Before:**
- Generation is in progress. Coordinator wants to know how things are going.

**During:**
1. Coordinator polls the status endpoint.
2. System returns progress information: best-so-far session count, total sessions required, time elapsed, current phase of the engine [see OQ#4 for detail level].
3. Coordinator sees generation is making progress (not stuck).

**After:**
- Coordinator either waits for completion, or [if cancellation is supported — see OQ#5] cancels the run.

### Journey 4: Coordinator Cancels In-Progress Generation

**Before:**
- Generation is running. Coordinator realizes they made a data mistake and wants to stop.

**During:**
1. Coordinator requests cancellation of the running generation.
2. Engine receives cancellation signal.
3. Engine stops and returns whatever partial result exists at that moment.
4. GenerationRequest status transitions to CANCELLED.

**After:**
- Coordinator fixes the data issue and re-triggers.
- Audit trail records cancellation.

*Note: This journey is subject to OQ#5 — cancellation support may be deferred.*

## 5. Functional Requirements

### FR-1: Time Limit Enforcement

- FR-1.1: The system shall enforce a time limit on timetable generation. When the time limit is reached without a complete solution, the engine shall stop and return the best partial result found so far.
- FR-1.2: The time limit shall be the value specified in BRD Section 8: 2 minutes [see OQ#2 for configurability].
- FR-1.3: The time limit measurement start point is subject to OQ#7. A4-11's design starts the clock before data loading (total elapsed from request start); this document must align with that, OR the exclusion of data-loading time must be explicitly justified as a provisional decision. Note: excluding loading is a relaxation of the BRD's literal "within 2 minutes" phrasing [see OQ#7].
- FR-1.4: On timeout, the system shall persist the best partial solution as a TimetableDraft with status indicating it is incomplete.

### FR-2: Partial Solution Delivery

- FR-2.1: When generation times out, the system shall deliver the best partial solution found — the state with the maximum number of sessions successfully placed without hard constraint violations.
- FR-2.2: The partial draft shall clearly indicate which sessions were placed and which remain unplaced.
- FR-2.3: The system shall compute and return a feasibility score for the partial draft: number of sessions placed / total sessions required (consistent with A4-11 FR-6.2).
- FR-2.4: The system shall compute and return a quality score for the placed sessions (soft constraint satisfaction among the sessions that were placed).
- FR-2.5: The partial draft shall include the list of unplaced sessions with their course, faculty, and batch context so the coordinator knows what remains.

### FR-3: Infeasibility Detection and Reporting

- FR-3.1: The system shall detect when the constraint set is infeasible — when no valid placement exists for one or more sessions given all hard constraints.
- FR-3.2: When infeasibility is detected, the system shall generate an infeasibility report that identifies: (a) which session(s) could not be placed, and (b) which hard constraints are in conflict for those sessions [level of detail subject to OQ#1].
- FR-3.3: The infeasibility report shall be actionable — it shall describe the conflict in terms the coordinator can act on (e.g., "Faculty X has no available slot on Monday-Wednesday that doesn't conflict with Room Y's capacity or Faculty Z's existing assignment") rather than internal algorithm terminology.
- FR-3.4: On infeasibility, the system shall transition the GenerationRequest to INFEASIBLE status with the report attached.
- FR-3.5: On infeasibility, the system shall still persist any sessions that WERE successfully placed before infeasibility was detected [see OQ#3 — partial result on infeasibility].

### FR-4: Progress Reporting

- FR-4.1: While generation is in progress, the system shall make progress information available to the coordinator via the status polling endpoint (A4-11 design: GET /generate/{id}/status).
- FR-4.2: Progress information shall include at minimum: (a) best-so-far session count (the highest number of sessions simultaneously placed without violation — monotonically non-decreasing, from the checkpoint), (b) total sessions to place, (c) elapsed time since generation started [see OQ#4 for additional detail]. Note: the "best-so-far" count is used rather than "currently placed" because during backtracking the current count rises and falls as assignments unwind — reporting that would produce a confusing progress indicator.
- FR-4.3: Progress information shall be updated at a reasonable interval (not stale for more than a few seconds) so the coordinator sees movement.
- FR-4.4: The system shall indicate which phase the engine is currently in (e.g., "propagation", "searching", "optimizing") to give the coordinator qualitative context.

### FR-5: Generation Status States

- FR-5.1: A4-11 already defines these terminal statuses: COMPLETED, FAILED (unexpected error), and CANCELLED. This document adds two new terminal statuses: **TIMED_OUT** and **INFEASIBLE**.
- FR-5.2: Status TIMED_OUT indicates the engine hit the time limit; a partial draft is available.
- FR-5.3: Status INFEASIBLE indicates no valid complete solution exists; an infeasibility report is available.
- FR-5.4: Status CANCELLED (already defined in A4-11) indicates the coordinator manually stopped the generation [subject to OQ#5]; a partial draft may be available.
- FR-5.5: Status FAILED (already defined in A4-11) indicates an unexpected system error (out of memory, unhandled exception). This document does not redefine FAILED behavior but acknowledges it in the state model for completeness.
- FR-5.6: Each terminal status shall include a timestamp of when the outcome was determined.

### FR-6: Cancellation

- FR-6.1: The system shall allow a coordinator to cancel an in-progress generation [subject to OQ#5].
- FR-6.2: On cancellation, the engine shall stop as soon as practical and return whatever partial result exists.
- FR-6.3: The GenerationRequest shall transition to CANCELLED status.
- FR-6.4: Cancellation shall be idempotent — cancelling an already-terminal request shall have no effect.

### FR-7: Audit Trail

- FR-7.1: The system shall record an audit event for each non-COMPLETED terminal outcome: TIMED_OUT, INFEASIBLE, CANCELLED, and FAILED.
- FR-7.2: The audit event shall include: generation request ID, department ID, outcome, sessions placed / total, time elapsed, and the user who triggered the original generation.
- FR-7.3: For infeasibility, the audit event shall include a summary of the conflicting constraints (not the full report — summary only for audit log readability).

## 6. Constraints Owned by This Document

| ID | Constraint | Type | Description |
|---|---|---|---|
| TIMEOUT-1 | Time limit enforcement | Hard (system) | Engine MUST stop at the configured time limit and return partial — never run indefinitely |
| TIMEOUT-2 | Partial solution validity | Hard (system) | Any partial solution returned MUST have zero hard constraint violations among the placed sessions |
| TIMEOUT-3 | Progress visibility | Hard (system) | Progress information MUST be available while generation is in progress — no "black box" period |

## 7. Constraints Referenced from Other Documents

| Constraint | Source | How Referenced |
|---|---|---|
| HC-ENG-1 through HC-ENG-12 (all engine hard constraints) | A4-11 | Infeasibility is reported in terms of these constraints |
| SC-ENG-1 through SC-ENG-6 (soft constraints) | A4-11 | Quality score for partial solutions uses same scoring |
| bestPartialState checkpoint mechanism | A4-11 design (KD-45) | Timeout delivers this checkpoint |
| Async with polling (PD-67) | A4-11 design | Progress reporting extends the existing poll response |
| Feasibility score = sessions_placed / total_required | A4-11 (Assumption 4) | Partial solution uses same formula |

## 8. Validation Rules

| Field/Output | Rule |
|---|---|
| Time limit | Must be > 0. BRD default: 120 seconds [see OQ#2 for configurability]. |
| Partial draft feasibility score | 0.0 to 1.0. < 1.0 for timeout (some sessions unplaced). |
| Partial draft quality score | 0.0 to 1.0. Computed only over placed sessions. |
| Infeasibility report | Must reference at least one session and at least one constraint. |
| Progress: bestSoFarCount | Integer >= 0, <= totalSessions. Monotonically non-decreasing. |
| Progress: totalSessions | Integer > 0. |
| Progress: elapsedSeconds | Number >= 0, <= time limit. |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Time limit accuracy | Engine must stop within a reasonable margin of the time limit (not 30 seconds over). Acceptable margin: [TBD — confirm with stakeholder, suggest <= 5 seconds overshoot]. |
| Progress update frequency | Progress should update at least every 5 seconds so the UI feels responsive. |
| Infeasibility detection speed | If infeasibility is detectable early (during propagation), it should be reported fast — not wait the full 2 minutes before declaring infeasible. |
| Partial solution persistence | Persisting the partial draft on timeout should complete within a few seconds (not add significant delay after the time limit). |
| Audit | All terminal outcomes audited within the same transaction as result persistence (consistent with A4-11 design, KD-54). |

## 10. Acceptance Criteria

1. Given a generation request, When the engine exceeds the time limit (2 minutes), Then it returns the best partial solution found so far with a clear indication that it is incomplete (status = TIMED_OUT).
2. Given an infeasible constraint set (no valid solution exists), When the engine detects this, Then it reports infeasibility with an explanation of which constraints are in conflict (status = INFEASIBLE).
3. Given a partial solution returned on timeout, When the coordinator views it, Then they can see which sessions were successfully placed and which remain unplaced.
4. Given infeasibility, When conflicting constraints are reported, Then the coordinator can understand what to adjust (report uses domain terms, not algorithm jargon).
5. Given a generation in progress, When the coordinator polls for status, Then progress information is available (best-so-far session count, total, elapsed time, current phase).
6. Given a timeout result, When the partial draft is inspected, Then zero hard constraint violations exist among the placed sessions.
7. Given infeasibility detected early (during propagation), When the engine identifies empty domains, Then it reports infeasibility immediately — does not wait for the full 2-minute timeout.
8. Given a partial draft from timeout, When scores are computed, Then feasibility score = placed/total (< 1.0) and quality score reflects only the placed sessions.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| GenerationRequest (extended) | id, status (IN_PROGRESS, COMPLETED, TIMED_OUT, INFEASIBLE, CANCELLED, FAILED), startedAt, completedAt, timeoutDurationSeconds, sessionsPlacedAtCompletion, totalSessionsRequired | Existing entity from A4-11, extended with TIMED_OUT and INFEASIBLE (COMPLETED, FAILED, CANCELLED already exist in A4-11) |
| InfeasibilityReport | id, generationRequestId, detectedAt, summary | Belongs to GenerationRequest |
| InfeasibilityConflict | id, reportId, affectedSessionDescription, conflictingConstraints (list), explanation | Belongs to InfeasibilityReport |
| UnplacedSession (view/output) | sessionDescription, courseCode, courseName, facultyName, batchName, reason | Part of timeout result (may be a computed view, not necessarily a separate table) |

*Note: Whether InfeasibilityReport is a separate table or a JSONB column on GenerationRequest is a design decision.*

## 12. Dependencies

| Dependency | Blocking? | Nature |
|---|---|---|
| A4-11 (Timetable Generation Engine) | Yes | A4-12 extends A4-11's engine with timeout/infeasibility behaviors |
| A4-11 design: bestPartialState checkpoint (KD-45) | Yes | Timeout relies on this mechanism existing |
| A4-11 design: async with polling (PD-67) | Yes | Progress reporting extends the poll response |
| A4-11 design: ConstraintSolver internals | Yes | Infeasibility detection happens within the solver |
| A4-15 (Drag-Drop Editor) | No | Consumes partial drafts for manual completion |
| A4-18 (Approval Workflow) | No | May or may not accept partial drafts [see OQ#6] |
| Common: AuditEventPublisher | No | Used for recording outcomes |

## 13. Assumptions

1. The engine already maintains a best-partial-state checkpoint (confirmed in A4-11 design, KD-45: "bestPartialState checkpoint. On timeout/failure, returns partial.").
2. The engine runs asynchronously and is already polled for status (A4-11 PD-67). A4-12 extends the poll response with richer progress data.
3. "Infeasibility" means the engine has definitively determined no valid solution exists for at least one session — not merely that it couldn't find one within the time limit (timeout != infeasibility).
4. The time limit measurement start point is an open question (OQ#7). A4-11's design starts the clock before data loading. Whether loading is included or excluded from the 2-minute budget needs resolution.
5. A partial draft from timeout is a valid TimetableDraft entity — it can be viewed, edited (A4-15), and potentially submitted (subject to OQ#6).

## 14. Consistency Notes

- **Timeout vs Infeasibility distinction:** These are mutually exclusive terminal states. TIMED_OUT means "the engine ran out of time but didn't prove infeasibility — a better result might exist with more time." INFEASIBLE means "the engine proved no valid complete solution exists regardless of time." This distinction matters for user messaging and next actions.
- **Feasibility score consistency:** A4-11 Assumption 4 defines feasibility = sessions_placed / total_required. A4-12 uses the identical formula for partial results. This is consistent.
- **Progress vs final result:** Progress reporting (FR-4) shows in-flight state; final result (FR-2, FR-3) shows the terminal state. These use the same underlying counters but serve different purposes.
- **Constraint count:** A4-12 does not own any scheduling constraints (HC or SC). It references all 12 hard constraints from A4-11 for infeasibility reporting. The constraint table in Section 6 contains only SYSTEM-level operational constraints (3 total: timeout enforcement, partial validity, progress visibility).
- **Audit consistency:** A4-11 design KD-54 establishes that audit events are within the persistence transaction. A4-12's audit events for timeout/infeasibility follow the same pattern.

## 15. Out of Scope

- The generation engine itself (constraint solving, backtracking, optimization) — owned by A4-11.
- Locked slots / partial re-generation — A4-14.
- Drag-drop manual editing of partial drafts — A4-15.
- Real-time conflict detection — A4-16.
- Approval workflow for partial/complete drafts — A4-18.
- Automatic retry of generation after timeout — not in BRD.
- Suggesting specific constraint relaxations to resolve infeasibility (beyond reporting what conflicts) — could be a future enhancement.

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | What level of detail should the infeasibility report provide? Options: (a) constraint types that conflict (e.g., "faculty double-booking + room capacity"); (b) specific entities involved (e.g., "Faculty X, Room Y, Batch Z on Tuesday 10:00"); (c) minimal unsatisfiable subset (MUS) — computationally expensive. | FR-3.2, FR-3.3, design complexity | System Design / UX |
| 2 | Should the timeout duration be configurable (per institution/department) or fixed at 2 minutes? BRD says "within 2 minutes" but larger departments might benefit from more time. | FR-1.2, configuration model | Academic Affairs / Product Owner |
| 3 | On infeasibility, should the system also persist a partial draft of sessions that WERE placed before infeasibility was detected? Or only the infeasibility report? | FR-3.5, data model | System Design / UX |
| 4 | What progress detail should be reported beyond sessions placed/total? Options: (a) just count + time; (b) current engine phase (propagation/search/optimization); (c) estimated time remaining. | FR-4.2, FR-4.4, UX | UX / Product Owner |
| 5 | Should cancellation be supported in this story? Or deferred? It adds API complexity (interrupt signal to async thread) but improves UX when coordinator realizes they made a data mistake. | FR-6, API design | Product Owner |
| 6 | Can a partial draft (from timeout) go through the approval workflow (A4-18), or must it be completed (manually or via re-run) first? | Workflow integration, downstream | Academic Affairs / System Design |
| 7 | When does the 2-minute clock start? A4-11's design starts timing before data loading (total elapsed). Excluding loading time is defensible (loading is I/O, not computation) but relaxes the BRD's literal "within 2 minutes." Should the timer include or exclude data loading and session derivation? | FR-1.3, alignment with A4-11, BRD compliance | System Design / Product Owner |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| Section 8 — "Auto-generate a full department timetable within 2 minutes" (implied: what if it can't?) | FR-1.1, FR-1.2, FR-2 |
| 6.10 — (implied) engine must handle cases where no valid solution exists | FR-3.1, FR-3.2, FR-3.3, FR-3.4 |
| 6.10 — "feasibility/quality score and violations list" | FR-2.3, FR-2.4 (for partial results) |
| Section 8 — "concurrent use by 50+ coordinators" (implies: coordinators need visibility into long-running ops) | FR-4 (progress reporting) |
| 6.10 — "Coordinators/HODs shall be able to review, override, and manually re-arrange the proposed draft" (implies: partial drafts are reviewable too) | FR-2.2, FR-2.5 |
