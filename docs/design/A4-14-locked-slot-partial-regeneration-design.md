# Design Document — Locked Slot Preservation and Partial Re-Generation

**Jira Reference:** A4-14
**Source Requirements:** docs/requirements/A4-14-locked-slot-partial-regeneration-requirements.md (Approved by lead, A4-64)
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x · Maven
**Generated:** 2026-09-01

## 1. Overview

This design extends the A4-11 timetable generation engine with two capabilities: (1) **locked slot preservation** — specific sessions can be marked immovable so the engine never moves them, and (2) **partial re-generation** — the engine can re-run on a selected subset of a draft while preserving locked sessions, approved sessions, and all out-of-scope sessions.

The design reuses two existing mechanisms rather than inventing new ones:
- The **common-slot pre-placement** mechanism from A4-11 (KD-52, `CSPState.prePlaceCommonSlots`) — generalized to pre-place any fixed session (locked / approved / out-of-scope).
- The **infeasibility detection + reporting** path from A4-12 (InfeasibilityCollector, `infeasibility_reports`/`infeasibility_conflicts` tables) — consumed unchanged when fixed sessions make the free subset unplaceable.

It modifies the existing `ScheduledSession` entity, `CSPState`, `SchedulingDataLoader`, `SchedulingEngineService`, and `SchedulingController` — it does not replace them, and it introduces no new solver algorithm.

**Not in scope:** The generation engine core (A4-11), timeout/infeasibility mechanics (A4-12, consumed here), fortnightly patterns (A4-13), drag-drop editing UI (A4-15), the approval workflow that produces "approved" status (A4-18, integration contract only).

## 2. Architecture

```
Controller Layer
    └── SchedulingController (extended)
         │  POST   /timetables/{draftId}/sessions/{sessionId}/lock     (NEW — FR-1)
         │  POST   /timetables/{draftId}/sessions/{sessionId}/unlock   (NEW — FR-1.2)
         │  POST   /timetables/{draftId}/regenerate                    (NEW — FR-3 partial re-gen)
         │
Service Layer
    ├── SessionLockService (NEW — lock/unlock, validation, audit)
    ├── SchedulingEngineService (MODIFIED — accept fixed-session set + regen scope)
    ├── SchedulingDataLoader (MODIFIED — load existing draft sessions as fixed inputs)
    └── SchedulingResultPersister (MODIFIED — copy-forward fixed sessions into new draft version)
         │
Solver Layer
    ├── CSPState (MODIFIED — prePlaceFixedSessions generalizes prePlaceCommonSlots)
    └── ConstraintSolver (UNCHANGED — fixed sessions are just pre-placed occupancy)
         │
Entity Layer
    └── ScheduledSession (MODIFIED — activate is_locked; add is_approved)
         │
Reused from A4-12 (UNCHANGED)
    ├── InfeasibilityCollector / InfeasibilityReport / InfeasibilityConflict
    └── GenerationStatus.INFEASIBLE flow
```

## 3. Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-59 | Generalize A4-11's common-slot pre-placement (KD-52) into `CSPState.prePlaceFixedSessions(List<FixedSessionInfo>)`. Locked, approved, and out-of-scope sessions are all pre-placed as immovable occupancy before solving; they are NOT added as free variables. | One proven mechanism handles all "fixed" categories. Reuses BitSet occupancy so free variables are validated against fixed placements with zero new solver logic (HC-LOCK-4 is automatic). | `prePlaceCommonSlots` becomes a thin caller of `prePlaceFixedSessions`. HardConstraintValidator unchanged. |
| KD-60 | A session's lock state persists on the existing `ScheduledSession.is_locked` column (present since A4-11, previously always false). Approved state persists on a NEW `is_approved` column. Both are "fixed" inputs to generation. | `is_locked` already exists; activating it avoids schema churn. Approved is a distinct concept (owned by A4-18) so it gets its own column. | FR-2 reads is_locked; FR-4 reads is_approved. A4-18 will set is_approved later (integration contract, Section 9). |
| KD-61 | Partial re-generation produces a NEW draft version (consistent with A4-11 PD-72), not an in-place mutation. The new version is built by COPYING FORWARD every fixed session (locked + approved + out-of-scope) unchanged, then adding freshly-placed sessions for the selected subset only. | Preserves history and satisfies zero-drift (FR-3.3, NFR correctness) — copied sessions are byte-identical on placement fields. In-place mutation would risk partial writes and lose history. | SchedulingResultPersister copy-forward step. Locked/approved flags carry to the new version. |
| KD-62 | Re-generation scope is expressed as a `RegenerationScope` selector supporting batch IDs, section IDs, and course IDs (any combination). A session is "selected" (free) if it matches the scope AND is neither locked nor approved. Everything else is fixed. | The story ACs are inconsistent (AC#2 "section", AC#3 "batches"); a flexible selector satisfies both without guessing. Locked/approved always win over scope (never regenerated even if in-scope). | Resolves requirement OQ#3. Precedence rule prevents ambiguity: fixed beats selected. |
| KD-63 | Infeasibility for locked/approved conflicts reuses A4-12's InfeasibilityCollector with NO changes. Because fixed sessions occupy faculty/room/batch slots via pre-placement, a free variable whose domain empties produces the normal A4-12 infeasibility entry; the pre-placed occupancy is the cause. | FR-5 requires locks be surfaced as contributing constraints. Since occupancy already reflects locks, the existing collector's constraint attribution (FACULTY_DOUBLE_BOOKING, etc.) naturally points at them. No new report type needed. | A4-12 tables/flow unchanged. Section 7.4 explains the surfacing. |

## 4. Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-82 | Lock/unlock API lives in THIS story's backend (SessionLockService + endpoints). The drag-drop UI affordance that calls it is A4-15. | Req OQ#1 | Backend here, UI in A4-15 | The backend contract is needed by any UI; building it here unblocks A4-15. Low cost. |
| PD-83 | Introduce `is_approved` column NOW (default false). A4-14 treats approved sessions as fixed. The approval workflow (A4-18) becomes the writer of this column later. Until A4-18 exists, `is_approved` stays false and FR-4 is exercised via test fixtures. | Req OQ#2 | Add column now, A4-18 writes it later | Avoids a future migration; lets FR-4 be designed and tested now. Integration contract in Section 9. |
| PD-84 | Re-generation subset granularity = flexible selector (batchIds / sectionIds / courseIds), default = by batch. Fixed (locked/approved) sessions are never regenerated regardless of scope. | Req OQ#3 | Flexible, default batch | Satisfies both AC#2 (section) and AC#3 (batch) without locking one in. |
| PD-85 | Partial re-generation creates a new draft version and supersedes the prior one (A4-11 PD-72). Prior version retained for history. | Req OQ#4 | New version | Consistent with full-generation versioning. Enables A4-19 history/diff later. |
| PD-86 | Unlock IS in scope for this story (symmetric with lock, one endpoint + one field write). | Req OQ#5 | Included | Trivial cost; AC#6 depends on it. |
| PD-87 | Pre-existing conflicts among locked/approved sessions are NOT validated at lock time (cross-session validation is expensive and locks are added incrementally). They surface at generation time as infeasibility (KD-63). | Req OQ#6 | Surface at generation | Lock-time cross-validation would require a full conflict scan per lock. Generation-time surfacing reuses A4-12 and matches AC#4. |

## 5. API Design

### New Endpoints

| Method | Path | Description | Auth | Traces To |
|---|---|---|---|---|
| POST | `/api/v1/timetables/{draftId}/sessions/{sessionId}/lock` | Lock a session at its current placement | COORDINATOR (own dept) / HOD / REGISTRAR | FR-1.1, FR-1.3 |
| POST | `/api/v1/timetables/{draftId}/sessions/{sessionId}/unlock` | Unlock a session | COORDINATOR (own dept) / HOD / REGISTRAR | FR-1.2 |
| POST | `/api/v1/timetables/{draftId}/regenerate` | Trigger partial re-generation scoped to a subset | COORDINATOR (own dept) / HOD / REGISTRAR | FR-3 |

### Request/Response Examples

**POST /timetables/{draftId}/regenerate — request body:**
```json
{
  "scope": {
    "batchIds": [12, 13],
    "sectionIds": [],
    "courseIds": []
  },
  "seed": null
}
```

**POST .../regenerate — 202 Accepted:**
```json
{
  "requestId": 87,
  "status": "IN_PROGRESS",
  "sourceDraftId": 17,
  "scope": { "batchIds": [12, 13], "sectionIds": [], "courseIds": [] },
  "fixedSessionCount": 34,
  "regeneratingSessionCount": 14,
  "statusUrl": "/api/v1/timetables/generate/87/status"
}
```

**POST .../lock — 200 OK:**
```json
{ "sessionId": 501, "draftId": 17, "isLocked": true }
```

**POST .../regenerate — 422 when scope selects nothing movable:**
```json
{
  "timestamp": "2026-09-01T11:40:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Regeneration scope selects no movable sessions (all matches are locked or approved).",
  "path": "/api/v1/timetables/17/regenerate"
}
```

Infeasibility during partial re-gen is reported via the **existing A4-12 endpoint** `GET /api/v1/timetables/generate/{requestId}/infeasibility` — no new endpoint (KD-63).

## 6. Data Model

### Migration V12

```sql
-- V12__locked_slot_partial_regeneration.sql

-- Activate lock semantics (is_locked already exists from V10; ensure default/index).
-- Add approved flag (PD-83) — written by A4-18 later, read by generation now.
ALTER TABLE scheduled_sessions ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT false;

-- Track regeneration provenance on the request (which draft + scope it came from).
ALTER TABLE generation_requests ADD COLUMN source_draft_id BIGINT NULL
    REFERENCES timetable_drafts(id);
ALTER TABLE generation_requests ADD COLUMN regeneration_scope TEXT NULL;

-- Index to quickly load fixed sessions for a draft during regeneration.
CREATE INDEX idx_session_draft_fixed ON scheduled_sessions(draft_id)
    WHERE deleted_at IS NULL AND (is_locked = true OR is_approved = true);

COMMENT ON COLUMN scheduled_sessions.is_approved IS
    'Set by approval workflow (A4-18). Treated as immovable/fixed by the scheduling engine (A4-14).';
COMMENT ON COLUMN generation_requests.regeneration_scope IS
    'JSON selector {batchIds,sectionIds,courseIds} for partial re-generation (A4-14). NULL for full generation.';
```

**Note:** `regeneration_scope` stored as JSON text (selector is small and read as a whole). `is_locked` reuses the existing V10 column — no re-add.

### Entity Changes

**ScheduledSession (modified):** add `isApproved` (Boolean, default false) alongside the existing `isLocked`.

**GenerationRequest (modified):** add `sourceDraftId` (Long, nullable) and `regenerationScope` (String JSON, nullable). NULL for a full generation; populated for a partial re-generation.

### New Model (in-memory, not persisted)

**FixedSessionInfo** — the pre-placement input for the solver (parallels `CommonSlotInfo`):
```java
public class FixedSessionInfo {
    private final Long sessionId;
    private final Long facultyId;
    private final Long batchId;
    private final Long roomId;
    private final String dayOfWeek;
    private final Long slotDefinitionId;
    // origin flag for audit/reporting: LOCKED | APPROVED | OUT_OF_SCOPE
    private final FixedReason reason;
}
```

**RegenerationScope** — the selector (deserialized from the request / regeneration_scope):
```java
public class RegenerationScope {
    private final List<Long> batchIds;
    private final List<Long> sectionIds;
    private final List<Long> courseIds;
    public boolean isEmpty() { /* all lists empty */ }
    public boolean matches(ScheduledSession s) { /* any id-set contains s's id */ }
}
```

## 7. Service Logic

### 7.1 SessionLockService (NEW)

```
lock(draftId, sessionId, userId):
  1. Load session; verify it belongs to draftId (404 if not) — enforces "lock target exists" AND "subset belongs to draft" validation rules.
  2. Verify session has a complete placement (day, slot, room, faculty) — else 422 (validation rule "lock captures full placement").
  3. Set isLocked = true; save.
  4. Publish audit event LOCK_SESSION (who, when, sessionId).

unlock(draftId, sessionId, userId):
  1. Load session; verify belongs to draft (404 if not).
  2. Set isLocked = false; save.
  3. Publish audit event UNLOCK_SESSION.
```
Both run in a @Transactional method so the audit event is within the same transaction (KD-54 pattern).

### 7.2 Partial Re-Generation Flow (SchedulingEngineService, MODIFIED)

```
triggerRegeneration(sourceDraftId, scope, userId):
  1. Load source draft + its department/campus/semester (404 if draft not found).
  2. Dept-ownership check (same as A4-11 trigger).
  3. Validate scope references belong to the source draft: every batchId/sectionId/courseId
     in the scope must correspond to at least one session in the draft — else 422
     (validation rule "subset belongs to draft"). Prevents regenerating a scope that
     names entities not present in this draft.
  4. Load ALL sessions of the source draft.
  5. Partition:
       - FIXED  = locked OR approved OR (NOT matching scope)
       - FREE   = matches scope AND NOT locked AND NOT approved
  6. If FREE is empty → 422 (KD-62 / validation "subset non-empty" after precedence).
  7. In-progress guard: reject if a generation is already running for this dept+semester (409, reuse A4-11 PD-68).
  8. Create GenerationRequest with sourceDraftId + regenerationScope (IN_PROGRESS).
  9. Async executeRegeneration(requestId, fixedSessions, freeVariables).
```

```
executeRegeneration:
  1. Build SchedulingInput (same loaders as A4-11).
  2. Derive SessionVariables ONLY for the FREE sessions (sessionDeriver over the free subset).
  3. Convert FIXED sessions → List<FixedSessionInfo>.
  4. CSPState.prePlaceFixedSessions(fixedSessions)  // KD-59: occupies faculty/room/batch
  5. Solve/optimize the free variables (ConstraintSolver UNCHANGED — schedules around fixed occupancy).
  6. Outcome handling identical to A4-11/A4-12 (COMPLETE / TIMED_OUT / INFEASIBLE / CANCELLED).
  7. Persist NEW draft version (KD-61): copy-forward all FIXED sessions unchanged (preserving isLocked/isApproved),
     then add the newly-placed FREE sessions. Supersede the source draft (PD-72/PD-85).
```

### 7.3 CSPState.prePlaceFixedSessions (KD-59)

Generalizes the existing `prePlaceCommonSlots`. For each `FixedSessionInfo`, resolve day/slot/room indices and set the faculty, room, and batch occupancy bits — exactly as common slots do today. The existing `prePlaceCommonSlots(commonSlots)` is refactored to build `FixedSessionInfo` entries (reason = OUT_OF_SCOPE/common) and delegate. No change to `HardConstraintValidator`: free variables are validated against this occupancy automatically (HC-LOCK-4).

### 7.4 Infeasibility Surfacing (KD-63)

When a free variable's domain empties because fixed sessions occupy all viable slots, the existing `InfeasibilityCollector.identifyBlockingConstraints`/`recordDeadEnd` path fires unchanged (A4-12). The resulting `infeasibility_conflicts.explanation` names the occupied faculty/room/batch — which are the locked/approved placements. FR-5.2 is satisfied because the occupancy that caused the empty domain IS the fixed set. No new report structure; the A4-12 `GET .../infeasibility` endpoint serves it.

### 7.5 Copy-Forward Persistence (SchedulingResultPersister, MODIFIED)

When persisting a regeneration result into the new draft version:
```
persistRegenerationResult(request, solvedState, fixedSessions, input):
  1. Create new TimetableDraft (version+1, supersede source).
  2. For each FIXED session: clone into new draft, preserving day/slot/room/faculty/batch,
     isLocked, isApproved (byte-identical placement — zero drift, NFR).
  3. For each solved FREE variable: create a new ScheduledSession in the new draft.
  4. Persist soft-constraint violations (free subset only).
  5. Audit PARTIAL_REGENERATION (sourceDraftId, newDraftId, scope, outcome) within the transaction.
```

## 8. Cross-Cutting Concerns

| Concern | Design |
|---|---|
| Audit | LOCK_SESSION, UNLOCK_SESSION, PARTIAL_REGENERATION events via AuditEventPublisher (A4-2 contract), within the same @Transactional as the mutation (KD-54 pattern). |
| Security | Lock/unlock/regenerate: COORDINATOR restricted to own department (same check as A4-11 trigger); HOD/REGISTRAR any. Verified server-side against the draft's department. |
| Error handling | 404 (session/draft not found, or session not in the given draft), 422 (session lacks full placement; scope references entities not in the draft; scope selects no movable session), 409 (regeneration already in progress for dept+semester — reuse A4-11 PD-68). Same GlobalExceptionHandler envelope. |
| Concurrency | Regeneration reuses A4-11's in-progress guard (partial unique index / PD-68) keyed by dept+semester, and the AbortPolicy thread pool (KD-46). |
| Isolation (NFR) | Only FREE sessions are re-placed; FIXED sessions are copied byte-identical → sessions outside scope are provably unchanged. |
| Performance (NFR) | Fewer free variables than a full run → generally faster; falls back to A4-12 timeout handling if it exceeds the bound. |

## 9. Integration Contracts

| Service/Component | Method/Interface | Contract | Direction |
|---|---|---|---|
| CSPState | prePlaceFixedSessions(List<FixedSessionInfo>) | Sets occupancy for immovable sessions; free variables validate against it. Supersedes/backs prePlaceCommonSlots. | Internal |
| InfeasibilityCollector (A4-12) | record / recordDeadEnd / buildReport | Reused unchanged; fixed-session occupancy is the conflict cause. | Internal (reuse) |
| Approval Workflow (A4-18) | writes ScheduledSession.is_approved | A4-18 sets is_approved=true when a section is approved. A4-14 reads it as "fixed". Until A4-18 exists, column stays false. | Inbound (future) |
| Drag-drop editor (A4-15) | calls lock/unlock/regenerate endpoints | This story owns the API; A4-15 owns the UI affordance. | Inbound (future) |
| AuditEventPublisher (A4-2) | publish(AuditEvent) | Events: LOCK_SESSION, UNLOCK_SESSION, PARTIAL_REGENERATION. | Outbound |
| SchedulingResultPersister | persistRegenerationResult(...) | New draft version with copied-forward fixed sessions + new free placements. | Internal |

## 10. Testing Strategy

| Scenario | Type | Approach |
|---|---|---|
| Locked sessions never move (AC#1) | Integration | Lock 3 sessions, run generation, assert the 3 are byte-identical (day/slot/room/faculty) in output. |
| Approved section untouched during partial re-gen (AC#2) | Integration | Mark a section's sessions is_approved, regenerate a different section, assert approved sessions unchanged in the new version. |
| Only selected subset regenerated (AC#3) | Integration | Regenerate scope=batch B; assert non-B sessions copied forward identically, B sessions re-placed. |
| Locks cause infeasibility (AC#4) | Integration | Lock sessions that saturate a faculty's slots, regenerate the rest; assert status=INFEASIBLE and the A4-12 report names the locked faculty/room. |
| Locked + approved both preserved (AC#5) | Integration | Combine locked and approved; regenerate; assert both categories unchanged. |
| Unlock frees a session (AC#6) | Integration | Lock, unlock, regenerate; assert the session may move. |
| Zero-drift copy-forward | Property test | For any regeneration, every fixed session's placement fields are identical pre/post. |
| Scope selects nothing movable → 422 | Unit | Scope matches only locked sessions; assert 422. |
| Scope references entity not in draft → 422 | Unit | Scope names a batchId with no sessions in the draft; assert 422. |
| Lock without full placement → 422 | Unit | Session missing room; assert lock rejected. |
| Audit events recorded | Integration | Lock + regenerate; assert audit_events has LOCK_SESSION and PARTIAL_REGENERATION. |

## 11. Provisional Decisions Summary

PD-82 through PD-87 (Section 4). All pending stakeholder ratification; each resolves a requirement Open Question.

## 12. Open Questions (Design-Level)

| # | Question | Owner | Status |
|---|---|---|---|
| OQ-D6 | Should locking be permitted only on drafts in a specific state (e.g., not on already-published timetables)? Requirements don't constrain this. | System Design | Provisional: allow lock on any non-published draft; published handling deferred to A4-19. |
| OQ-D7 | When a new draft version is created by regeneration, should the source version be soft-deleted or retained as an accessible prior version? | System Design | Provisional: retained (superseded flag), consistent with A4-11 PD-72; history surfacing is A4-19. |

## 13. Consistency Notes

- Migration V12 follows V11 (A4-12). KD-59–63. PD-82–87. No numbering collisions with A4-11 (KD-45–54, PD-67–74) or A4-12 (KD-55–58, PD-75–81).
- Reuses A4-11 KD-52 pre-placement (generalized) and A4-11 PD-72 versioning; reuses A4-12 InfeasibilityCollector + `infeasibility_*` tables + `GET .../infeasibility` endpoint — none redefined.
- `GenerationStatus` enum unchanged (6 values). Partial re-gen uses the same statuses; INFEASIBLE/TIMED_OUT/CANCELLED behave as in A4-12.
- `ScheduledSession.is_locked` reuses the existing V10 column; only `is_approved` is added.
- Error envelope, audit contract (A4-2), dept-scoping, and thread pool (KD-46) match A4-11/A4-12.
- HardConstraintValidator and ConstraintSolver are UNCHANGED — fixed sessions enter purely as pre-placed occupancy.

## 14. Traceability

| Requirement | Design Element |
|---|---|
| FR-1.1 / FR-1.3 (lock, full placement) | SessionLockService.lock + POST .../lock + placement validation |
| FR-1.2 (unlock) | SessionLockService.unlock + POST .../unlock (PD-86) |
| FR-1.4 (audit lock/unlock) | AuditEventPublisher LOCK_SESSION/UNLOCK_SESSION (Section 8) |
| FR-2.1–2.4 (preserve locked in generation) | KD-59 prePlaceFixedSessions; free-variable exclusion; copy-forward (KD-61) |
| FR-3.1–3.4 (partial re-gen scope) | KD-62 RegenerationScope; Section 7.2 partition; POST .../regenerate |
| FR-4.1–4.3 (approved preserved) | KD-60 is_approved; treated as FIXED (PD-83); combined with locked in partition |
| FR-5.1–5.2 (infeasibility surfaces locks) | KD-63 reuse A4-12 InfeasibilityCollector; Section 7.4 |
| FR-6.1–6.3 (output/versioning/audit) | KD-61 new version + copy-forward; PARTIAL_REGENERATION audit |
| HC-LOCK-1 (locked immovable) | Pre-placed, excluded from free variables |
| HC-LOCK-2 (approved immovable) | Pre-placed as FIXED |
| HC-LOCK-3 (only subset regenerated) | Partition (Section 7.2) + copy-forward |
| HC-LOCK-4 (fixed consume occupancy) | prePlaceFixedSessions sets BitSet occupancy; validator checks automatically |
| Validation: lock target exists / subset belongs to draft | 7.1 step 1; 7.2 step 3 |
| Validation: subset non-empty | 7.2 step 6 (422) |
| Validation: lock captures full placement | 7.1 step 2 (422) |
| NFR correctness (zero drift) | Copy-forward byte-identical (Section 7.5) + property test |
| NFR audit | Events within @Transactional (KD-54 pattern) |
| Req OQ#1–6 | Resolved by PD-82–87 respectively |
