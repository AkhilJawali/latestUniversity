# Design Document — Real-Time Conflict Detection Engine

## 1. Overview

This design specifies the real-time conflict detection engine (A4-16). It evaluates
a proposed session placement (or a whole draft) against a timetable draft's current
occupancy and returns a structured list of conflicts within a < 2s SLA, over an
interactive channel for the drag-and-drop editor (A4-15).

It **reuses** the generation-time hard-constraint rule logic from A4-11
(`HardConstraintValidator` / `CSPState` semantics) rather than redefining it, and
introduces a draft-scoped occupancy index hydrated from persisted
`ScheduledSession` rows (the engine's `CSPState` is coupled to a generation run and
cannot be reused directly).

**Source Requirement:** `docs/requirements/A4-16-real-time-conflict-detection-requirements.md` (Approved — A4-72).

**Requirement carried 9 Open Questions; the lead approved without resolving them.**
This design therefore either makes a **Provisional Decision (pending stakeholder
ratification)** or **explicitly defers** each — see §4. Nothing is silently hardcoded.

**Not in scope:** alternative-slot suggestions and conflict rendering / drag-drop UX
(A4-15); conflict-log persistence (separate story); saving placements (A4-14/A4-15);
new travel-time master data (A4-35).

## 2. Architecture

```
Delivery Layer
    ├── ConflictWsHandler (NEW — STOMP @MessageMapping "/conflict-check")   ── FR-6
    └── ConflictController (NEW — REST fallback)                            ── FR-1/FR-2
         │  POST /api/v1/drafts/{draftId}/conflict-check   (single placement)
         │  GET  /api/v1/drafts/{draftId}/conflicts        (full-draft check)
         │
Service Layer
    └── ConflictDetectionService (NEW)                                     ── FR-1/2/7
         ├── checkPlacement(draftId, ProposedPlacement) -> List<ConflictDto>
         └── checkDraft(draftId) -> List<ConflictDto>
         │
Rule Layer (REUSED from A4-11)
    ├── PlacementRuleChecker (NEW thin adapter over the engine rules)
    └── (reuses HardConstraintValidator rule semantics: double-booking,
         capacity, workload, consecutive, room-hard-block)
         │
Occupancy Layer
    ├── DraftOccupancyIndex (NEW — BitSet occupancy built from persisted sessions) ── OQ#7
    └── DraftOccupancyLoader (NEW — hydrates the index via ScheduledSessionRepository)
         │
Data (EXISTING — read only, no schema change)
    ├── ScheduledSessionRepository.findByDraftIdAndDeletedAtIsNull(draftId)
    ├── ScheduledSession (facultyId, roomId, batchId, sectionId, dayOfWeek, slotDefinitionId, recurrence)
    └── Room master data (capacity, building/floor)
         │
DTO / Enum Layer (NEW)
    ├── ConflictType (enum — the owned catalogue)
    ├── ConflictDto (type, involved ids, day, slot, description)
    └── ProposedPlacementRequest (draftId, faculty/room/batch/section, day, slot, durationMinutes)
```

**No Flyway migration** (latest is V13). Conflicts are transient by default (PD-97);
no new table is introduced by this story.

## 3. Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-61 | Introduce a **draft-scoped `DraftOccupancyIndex`** (BitSet maps for faculty/room/batch, keyed by day×slot) built from persisted `ScheduledSession` rows, separate from the generation `CSPState`. | `CSPState` is constructed for a generation run (variables + `SchedulingInput`); it has no loader from persisted sessions. A parallel read-only index gives O(1) occupancy lookups for the < 2s SLA without dragging in the solver. | Reuses the same bit-index scheme (`day*maxSlots+slot`) as `CSPState` (KD-48) so rule semantics match exactly. |
| KD-62 | **Reuse the A4-11 rule definitions** via a thin `PlacementRuleChecker` that applies the same predicates (faculty/room/batch occupancy, capacity ≥ strength, daily/weekly/consecutive hours, room hard-block) against the `DraftOccupancyIndex`. | Single-ownership: A4-11 owns the generation-time hard-constraint definitions; A4-16 must not contradict them. Sharing the predicate logic keeps them consistent (req §7). | If A4-11 rules change, the checker follows. Consecutive-hours reported as a **distinct** ConflictType from daily/weekly (AC5). |
| KD-63 | **`ConflictType` enum** (owned catalogue) with values: FACULTY_DOUBLE_BOOKING, ROOM_DOUBLE_BOOKING, BATCH_CLASH, ROOM_CAPACITY, FACULTY_DAILY_HOURS, FACULTY_WEEKLY_HOURS, FACULTY_CONSECUTIVE_HOURS, ROOM_HARD_BLOCK, FACULTY_HARD_BLOCK, TRAVEL_TIME, PREREQUISITE_SEQUENCE. | FR-3. Enum is the canonical machine-readable catalogue consumed by A4-15. `@Enumerated(STRING)` convention (though not persisted here). | TRAVEL_TIME and PREREQUISITE_SEQUENCE are DEFINED in the enum but **not detected yet** (PD-98/PD-96) — the value exists so consumers have a stable contract when detection lands. |
| KD-64 | **Recurrence-aware overlap:** two sessions in the same (day, slot) only conflict if their occurrence weeks overlap. Reuse A4-13 `RecurrenceOverlapEvaluator`. | OQ#8. A fortnightly Group-A vs Group-B pair in the same slot is NOT a conflict. Ignoring this would produce false positives for split labs. | The occupancy index stores recurrence/weekGroup per occupancy bit; the checker consults `RecurrenceOverlapEvaluator` before flagging. |
| KD-65 | **Delivery: Spring WebSocket + STOMP** (`spring-boot-starter-websocket`, to be added) with a REST fallback (`ConflictController`) for the same checks. | Org steering is Java/Spring; the story is labelled `websocket`. REST fallback keeps the check testable and usable without a live socket. | Requires adding the starter dependency (see §10 Dependencies). WsHandler delegates to the same `ConflictDetectionService` as REST — one code path. |

## 4. Provisional Decisions (pending stakeholder ratification)

These resolve requirement Open Questions that the lead did not answer. They are
**provisional** — flagged for ratification, not treated as confirmed.

| # | Decision | Resolves | Rationale / Status |
|---|---|---|---|
| PD-95 | **Student-level elective clash is DEFERRED.** Detection is batch/section only. When elective registration (Phase 14) lands, a student-level check can extend the service. | OQ#1 | No student/elective model exists. AC3 is served at batch/section granularity now. **Confirm scope.** |
| PD-96 | **PREREQUISITE_SEQUENCE detection is DEFERRED.** The enum value exists; no detection logic is built. | OQ#2 | Story self-flagged `[STAKEHOLDER CONFIRMATION NEEDED]`; interpretation (within-semester vs curriculum-level) unresolved. **Needs Registrar/Academic Affairs.** |
| PD-97 | **Conflicts are transient** — returned in the response, not persisted. No new table/migration. | OQ#3 | Conflict-log persistence is a separate story. If adopted later, it adds storage without changing this API. **Confirm.** |
| PD-98 | **TRAVEL_TIME detection is DEFERRED.** Enum value exists; no travel-buffer rule or building-distance data implemented. | OQ#4 | No travel-time/distance model exists; depends on multi-campus (A4-35). **Confirm sequencing.** |
| PD-99 | **FACULTY_HARD_BLOCK depends on a prerequisite fix.** The engine's `CSPState.isFacultyHardBlocked` is a stub returning false. This story will wire the check, but correctness requires that stub to be implemented (faculty hard-availability windows loaded). | OQ#5 | Flagged as a dependency; FACULTY_HARD_BLOCK will under-report until the stub is fixed. **Confirm whether the fix is in scope here or a prerequisite story.** |
| PD-100 | **Occupancy caching:** an in-memory per-draft occupancy index, rebuilt on demand and invalidated when the draft's sessions change (using `TimetableDraft.version`). Redis is NOT introduced for Phase 1 (50 concurrent coordinators do not require it). | OQ#9 | Matches the "match infrastructure to scale" guardrail. A single JVM in-memory index meets the < 2s SLA for the stated load. **Confirm load target.** |

## 5. Service Logic

### 5.1 `ConflictDetectionService.checkPlacement(draftId, placement)`
1. Validate `draftId` resolves to a non-deleted draft (else 400 — no internals).
2. Obtain the `DraftOccupancyIndex` for the draft (cached; build via loader on miss).
3. **Exclude** the session being moved (if `placement.sessionId` present) from occupancy so a move onto its own slot isn't self-conflicting.
4. Run `PlacementRuleChecker` predicates; for each violated rule, add a `ConflictDto` (type, involved ids, day, slot, description).
5. Recurrence guard (KD-64): a same-slot occupant only yields a conflict if occurrence weeks overlap.
6. Return the list (empty = valid). Target < 2s (occupancy lookups are O(1)).

### 5.2 `ConflictDetectionService.checkDraft(draftId)`
Iterate placed sessions, evaluate each against the rest via the index, aggregate all conflicts (FR-2 / AC9).

### 5.3 `DraftOccupancyLoader`
Loads `findByDraftIdAndDeletedAtIsNull(draftId)`, resolves room capacity/building from room master data, and populates the `DraftOccupancyIndex` BitSets + workload accumulators (mirrors `CSPState.assign` semantics).

## 6. API Design

| Method | Path | Description | Auth | Traces To |
|---|---|---|---|---|
| POST | `/api/v1/drafts/{draftId}/conflict-check` | Check a single proposed placement | authenticated | FR-1, AC1–AC8 |
| GET | `/api/v1/drafts/{draftId}/conflicts` | Full-draft conflict list | authenticated | FR-2, AC9 |
| STOMP | `/app/drafts/{draftId}/conflict-check` → `/topic/...` | Same single-placement check over WebSocket | authenticated | FR-6 |

**Success:** `{ "data": [ ConflictDto ] }` (empty array = no conflict).
**Error envelope:** reuse the existing `GlobalExceptionHandler` shape (status, error, message, path, details) — 400 for malformed/nonexistent draft, no stack traces (AC8).

**ConflictDto:** `{ type, involvedFacultyId?, involvedRoomId?, involvedBatchId?, involvedSectionId?, involvedSessionId?, dayOfWeek, slotDefinitionId, description }`.

## 7. Testing Strategy

- Unit: `ConflictDetectionService` per conflict type (AC1–AC7, AC9), malformed input (AC8), no-conflict happy path (AC6), multiple-conflict aggregation (AC7), recurrence non-overlap (KD-64).
- Reuse-consistency test: a placement rejected by the engine's `HardConstraintValidator` is also flagged by `PlacementRuleChecker` for the shared types (guards single-ownership).
- Integration (Testcontainers, when Docker available): load a persisted draft, check placements end-to-end via REST.
- Deferred types (TRAVEL_TIME, PREREQUISITE_SEQUENCE, FACULTY_HARD_BLOCK correctness, student-level) are NOT asserted — see §4.

## 8. Cross-Cutting Concerns

- **Performance (FR-5):** O(1) BitSet lookups + per-draft in-memory index (PD-100); no DB round-trip on the hot path after the index is built.
- **Security:** authenticated; error responses leak no internals (AC8).
- **Concurrency:** index invalidated on draft session change via `TimetableDraft.version`; consistent with optimistic concurrency used by the editor (A4-15).
- **Accessibility:** `ConflictDto.description` carries text so the editor can convey conflicts without relying on colour (req §9).

## 9. Consistency & Interaction Notes (P6 Step 3)

- **Transient conflicts (PD-97) + no audit:** acceptable — conflict-log persistence is a separate story; no audit gap is introduced because no conflict data is stored.
- **Occupancy cache (PD-100) + concurrent edits:** staleness mitigated by version-based invalidation; a stale read at worst produces a re-check on save (A4-15 owns the authoritative save-time check).
- **Reuse of engine rules (KD-62) + generation-coupled CSPState (KD-61):** resolved by the parallel `DraftOccupancyIndex` — the rule *predicates* are shared, the *occupancy source* is not.
- **FACULTY_HARD_BLOCK (PD-99):** the wire is designed, but the underlying engine stub means it under-reports until fixed — called out, not hidden.

## 10. Dependencies

- A4-11 — rule semantics, `ScheduledSession`/`TimetableDraft`, `ScheduledSessionRepository`, room master data.
- A4-13 — `RecurrenceOverlapEvaluator` (KD-64).
- **New dependency:** `spring-boot-starter-websocket` (KD-65) — must be added to `pom.xml` (pinned via Spring Boot BOM 3.3.2).
- Prerequisite fix (PD-99): faculty hard-availability loading (`CSPState.isFacultyHardBlocked` stub).
- Blocked-until-built: elective registration (Phase 14) for student-level (PD-95); A4-35 for travel-time (PD-98).

## 11. Traceability (FR/AC → design element)

| Requirement | Design element |
|---|---|
| FR-1 single check | `ConflictDetectionService.checkPlacement` + REST/STOMP endpoints |
| FR-2 full-draft check | `ConflictDetectionService.checkDraft` + GET endpoint |
| FR-3 catalogue | `ConflictType` enum (KD-63) |
| FR-4 structured result | `ConflictDto` |
| FR-5 < 2s SLA | `DraftOccupancyIndex` O(1) + in-memory cache (PD-100) |
| FR-6 channel | STOMP handler + REST fallback (KD-65) |
| FR-7 no false negatives | `PlacementRuleChecker` reusing engine predicates (KD-62); deferred types per §4 |
| AC1–AC7, AC9 | service unit tests §7 |
| AC8 malformed input | draftId/id validation + GlobalExceptionHandler |
| OQ#1/#2/#3/#4/#5/#9 | PD-95 / PD-96 / PD-97 / PD-98 / PD-99 / PD-100 |
| OQ#6 transport | KD-65 |
| OQ#7 occupancy source | KD-61 |
| OQ#8 recurrence | KD-64 |

## 12. Open Items Carried Forward

All requirement OQs are dispositioned above as Provisional Decisions (PD-95..PD-100)
or Key Decisions (KD-61/64/65). The **provisional** ones (PD-95, PD-96, PD-98, PD-99)
change scope and need stakeholder/lead ratification before the corresponding
detection is built. The 9 firm ACs from the requirement are fully designed and can
proceed to development regardless of the deferred types.

---

*Prepared for the Design Derivation subtask (A4-73) of A4-16. Migration: none (V13
is latest; conflicts transient). Provisional Decisions PD-95..PD-100 continue the
project-wide sequence (last used: PD-94).*
