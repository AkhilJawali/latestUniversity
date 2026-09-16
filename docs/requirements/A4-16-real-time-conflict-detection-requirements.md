# Requirement Document — Real-Time Conflict Detection Engine

## 1. Introduction

This document captures the requirements for detecting scheduling conflicts in
real-time (target < 2 seconds) when a coordinator reviews or edits a timetable
draft. It defines the catalogue of conflict types the system recognises and the
channel by which conflicts are reported back to the editing client.

This story **OWNS the real-time conflict-type catalogue** — the canonical list of
detectable conflict types and their meaning for the editing experience. Other
stories consume it (A4-15 drag-and-drop editor, A4-14 partial re-generation, and
the multi-campus and add/drop stories). It does **not** re-define the
generation-time hard constraints already implemented in the engine (A4-11); where
the same rule applies, this story reuses the engine's rule logic (see §7).

## 2. User Story

**A4-16:** As a Department Coordinator, I want the system to detect all scheduling
conflicts in real-time — faculty double-booking, room double-booking,
student/batch clashes, capacity mismatches, daily/weekly hour limits exceeded,
max consecutive teaching hours, hard-block violations, and travel-time violations
— so that I am immediately aware of issues and can resolve them before submission.

**Story Points:** 8

**BRD Requirements:** 6.9 — real-time detection of faculty double-booking, room
double-booking, student/batch clashes, and exceeding max daily/weekly teaching
hours; Section 8 — "Conflict checks return in under 2 seconds"; 7.3 — room
capacity ≥ batch/group strength; 7.3 — location/transit buffer; 7.4 — max
consecutive teaching hours for faculty; 7.1 — prerequisite mapping (see OQ#2).

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Proposes a session placement/move and receives conflict feedback |
| Conflict Detection Engine (system) | Evaluates a proposed placement against a draft's current occupancy and returns conflicts |
| Timetable Editor (A4-15, consumer) | Sends placement checks, renders returned conflicts and indicators |
| ScheduledSession / TimetableDraft (A4-11 entities) | The persisted data the check operates on |
| Audit Trail (common) | Optional record of conflicts (see OQ#3) |

## 4. User Journeys

### Journey 1: Single placement check during editing (happy path)
**Before:** A draft exists with persisted sessions (A4-11 output). The editor has
loaded the draft.
**During:**
1. Coordinator drags a session to a candidate (day, slot, room), or edits an assignment.
2. The client sends a placement-check request (draftId + proposed placement).
3. The engine evaluates the placement against the draft's current occupancy.
4. If no conflict: the engine returns an empty conflict list (placement is valid).
5. The result returns within the < 2s SLA (BRD Section 8).

**After:** The editor allows the placement; the actual save is owned by A4-15/A4-14.

### Journey 2: Placement produces one or more conflicts
**During:**
1. Coordinator proposes a placement that clashes (e.g., the faculty is already booked in that slot).
2. The engine returns a structured conflict list: for each conflict, its **type**, the **involved entities** (faculty/room/batch/session ids), and a human-readable description.
3. The editor renders the conflicts (indicators/description). Alternative-slot suggestion is owned by A4-15 / BRD 6.9 — **out of scope here** (see §15).

**After:** Coordinator adjusts and re-checks. No conflict is silently dropped.

### Journey 3: Full-draft validation
**During:** Coordinator (or the editor on load) requests a check of the entire
current draft; the engine returns all conflicts present across all placed sessions.
**After:** The editor surfaces a conflict summary/count.

## 5. Functional Requirements

Organised by behaviour, not by algorithm.

### FR-1: Single-placement conflict check
The system shall evaluate a single proposed placement (draftId + faculty, room,
batch/section, day, slot, session duration) against the draft's current occupancy
and return the list of conflicts it would introduce, or an empty list if valid.

### FR-2: Full-draft conflict check
The system shall evaluate all currently placed sessions of a draft and return all
detected conflicts.

### FR-3: Conflict-type catalogue (OWNED by this story)
The system shall recognise and report the following real-time conflict types. Each
returned conflict carries: type, involved entity ids, and a description.

| Type | Meaning | Backing rule status (see §7) |
|---|---|---|
| FACULTY_DOUBLE_BOOKING | Faculty already occupied in that day/slot | Reuse engine rule |
| ROOM_DOUBLE_BOOKING | Room already occupied in that day/slot | Reuse engine rule |
| BATCH_CLASH | Batch/section already occupied in that day/slot | Reuse engine rule (batch/section level — see OQ#1) |
| ROOM_CAPACITY | Room capacity < batch/group strength | Reuse engine rule |
| FACULTY_DAILY_HOURS | Placement exceeds faculty max daily hours | Reuse engine rule |
| FACULTY_WEEKLY_HOURS | Placement exceeds faculty max weekly hours | Reuse engine rule |
| FACULTY_CONSECUTIVE_HOURS | Placement exceeds faculty max consecutive teaching hours (distinct from daily/weekly) | Reuse engine rule |
| ROOM_HARD_BLOCK | Room is hard-blocked in that window | Reuse engine rule |
| FACULTY_HARD_BLOCK | Faculty declared hard-unavailable in that window | **Engine rule is a stub today (see OQ#5)** |
| TRAVEL_TIME | Back-to-back sessions on distant buildings/campuses violate the travel buffer | **New — not implemented anywhere (see OQ#4)** |
| PREREQUISITE_SEQUENCE | Prerequisite/dependent course scheduling conflict | **New — interpretation unconfirmed (see OQ#2)** |

### FR-4: Structured conflict result
Each conflict shall include a stable machine-readable `type`, the involved entity
identifiers, and a description that never exposes internal stack traces or paths.

### FR-5: Real-time delivery within SLA
Conflict results for a single-placement check shall be returned within 2 seconds
(BRD Section 8) under normal load.

### FR-6: Real-time delivery channel
The system shall expose the check over a channel suitable for the interactive
editor (the story is labelled `websocket`). The concrete transport is a design
decision (see OQ#6); the requirement is sub-2s interactive feedback.

### FR-7: No false negatives (correctness)
For the conflict types this story detects against available data, the system shall
not miss a conflict that is actually present (a returned "no conflict" must be
trustworthy for those types). Types with missing backing data (travel-time,
prerequisite, faculty-hard-block, student-level) are governed by their OQs.

## 6. Constraints Owned by This Document

- **CT-1 … CT-11:** the real-time conflict-type catalogue in FR-3 (the canonical
  set of conflict types the editing experience recognises).
- **CT-SLA:** single-placement check completes < 2 seconds (BRD Section 8).

## 7. Constraints Referenced from Other Documents

The generation-time hard constraints are defined and implemented by A4-11
(`HardConstraintValidator` HC-ENG-1..HC-ENG-12). This story **reuses that rule
logic** for the overlapping types rather than re-defining it:

- HC-ENG-1 faculty double-booking, HC-ENG-2 room double-booking, HC-ENG-3 batch
  clash, HC-ENG-4 room capacity, HC-ENG-7 room hard-block, HC-ENG-11 daily/weekly
  load, HC-ENG-12 consecutive hours — all reused as the backing rule for the
  corresponding CT-* types.
- Occupancy semantics (O(1) BitSet maps) are defined in A4-11 `CSPState`. Note the
  reuse caveat in OQ#7: `CSPState` is coupled to a generation run; a real-time
  check needs occupancy hydrated from persisted `ScheduledSession` rows.

**Single-ownership note:** A4-11 owns the *generation-time* hard-constraint
definitions; A4-16 owns the *real-time conflict-type catalogue* presented to the
editor. These must stay consistent — the same clash must not be a hard constraint
in one and absent in the other. This document does not introduce a contradictory
definition of any shared rule.

## 8. Validation Rules

| Input | Rule |
|---|---|
| draftId | Must reference an existing, non-deleted draft |
| Proposed placement | day/slot/room/faculty/batch ids must resolve against the draft's master data; reject malformed input with 400, no internals leaked |
| Session duration | Resolved from the slot definition (multi-slot sessions per A4-13 recurrence honoured on occurring weeks — see OQ#8) |

## 9. Non-Functional Requirements

- **Performance:** single-placement check < 2s (BRD Section 8); target supports 50+ concurrent coordinators (BRD Section 8) — see OQ#9 for caching strategy.
- **Security:** authenticated access; no stack traces or internal paths in responses.
- **Auditability:** whether conflicts are persisted is deferred (OQ#3).
- **Accessibility:** conflict info must be conveyable by text (not colour alone) — rendering is owned by A4-15, but the payload must carry a text description (FR-4).

## 10. Acceptance Criteria (Given / When / Then)

1. **Faculty double-booking:** Given a draft where faculty F is booked at (Mon, Slot 3), When a placement books F again at (Mon, Slot 3), Then a FACULTY_DOUBLE_BOOKING conflict is returned within 2 seconds.
2. **Room double-booking:** Given room R occupied at (Tue, Slot 2), When another session is placed in R at (Tue, Slot 2), Then a ROOM_DOUBLE_BOOKING conflict is returned.
3. **Batch clash:** Given a batch B occupied at (Wed, Slot 1), When another session for B is placed at (Wed, Slot 1), Then a BATCH_CLASH conflict is returned (batch/section level — student-level is OQ#1).
4. **Capacity:** Given room R capacity 40, When a batch of strength 60 is placed in R, Then a ROOM_CAPACITY conflict is returned.
5. **Consecutive hours (distinct):** Given faculty F at the configured max consecutive limit, When a further back-to-back session is placed, Then a FACULTY_CONSECUTIVE_HOURS conflict is returned — reported as a distinct type from the daily/weekly cap.
6. **No conflict — happy path:** Given a free (day, slot, room) with no rule violation, When a placement is checked, Then an empty conflict list is returned within 2 seconds.
7. **Multiple conflicts:** Given a placement that violates two rules at once, When checked, Then all applicable conflicts are returned (not just the first).
8. **Malformed input:** Given a check request with an invalid/nonexistent draftId or ids, When submitted, Then a 400 validation error is returned with no internal details.
9. **Full-draft check:** Given a draft with an existing clash, When a full-draft check runs, Then that conflict appears in the returned list.

*(Travel-time, prerequisite, faculty-hard-block, and student-level acceptance
criteria are intentionally omitted here pending OQ#2/#4/#5/#1 resolution — see §16.
Adding them now would assert behaviour against data/rules that do not yet exist.)*

## 11. Data Model (conceptual)

- **Conflict (transient by default):** `{ type, involvedFacultyId?, involvedRoomId?, involvedBatchId?, involvedSessionId?, day, slot, description }`.
- **ConflictType (enum):** the CT catalogue (FR-3). New to this story.
- Persistence of conflicts is deferred (OQ#3) — the conflict-log persistence story
  (separate) owns storage if adopted.
- Operates on existing `ScheduledSession` (draftId, facultyId, roomId, batchId,
  sectionId, dayOfWeek, slotDefinitionId) and room master data (building/floor for
  travel-time).

## 12. Dependencies

- **A4-11** — engine hard-constraint rules and occupancy semantics to reuse; `ScheduledSession`/`TimetableDraft` entities.
- **Room master data** — building/floor for travel-time (data exists on `RoomInfo`; travel-time *logic* does not).
- **Elective registration (Phase 14, not built)** — required for student-level clash (OQ#1).
- **Faculty hard-availability** — the engine's check is a stub (OQ#5).
- **A4-15 (consumer)** — drag-drop editor; alternative-slot suggestions and rendering live there.

## 13. Assumptions

1. A "real-time check" operates against a single draft's currently persisted sessions.
2. The check is stateless per request except for reading the draft's occupancy.
3. Batch-level clash is the detectable granularity today (student-level pending OQ#1).
4. The < 2s SLA is measured under normal (not worst-case bulk) load.

## 14. Consistency Notes (contradiction check)

- **Story text vs system reality (AC3):** the story's AC3 says "student's registered
  elective" clash. **There is no student or elective-registration model in the
  system** (confirmed — Phase 14). This document therefore scopes AC3 to
  batch/section clash and raises student-level as OQ#1 — it does not claim
  student-level detection.
- **"OWNS all conflict-type definitions" vs A4-11:** reconciled in §7 — A4-16 owns
  the real-time catalogue; A4-11 owns generation-time hard constraints; the shared
  rules are reused, not contradicted.
- **AC9 prerequisite (from the story) is self-flagged** `[STAKEHOLDER CONFIRMATION
  NEEDED]` — carried forward as OQ#2, not asserted as a firm requirement.
- **Faculty hard-block:** listed as a conflict type but the engine's rule is a stub
  returning false — flagged (OQ#5) rather than assumed working.

## 15. Out of Scope

- Alternative-slot suggestion / ranked resolutions (BRD 6.9) — owned by A4-15.
- Rendering conflict indicators, drag-drop UX, optimistic concurrency — A4-15.
- Persisting a conflict log / conflict reports — separate conflict-log story (OQ#3).
- Applying/saving the placement — A4-14 / A4-15.
- Defining new travel-time master data — see OQ#4 / multi-campus A4-35.

## 16. Open Questions

| # | Question | Impact | Proposed disposition |
|---|---|---|---|
| OQ#1 | Student-level elective clash (AC3) — no student/elective model exists. Detect only batch/section now, or block on elective-registration (Phase 14)? | Scope of AC3 | Recommend: scope to batch/section now; add student-level when elective registration lands. **Confirm.** |
| OQ#2 | AC9 prerequisite sequencing — within-semester scheduling check vs curriculum/catalogue check? (story self-flagged) | Whether PREREQUISITE_SEQUENCE is in scope and where | Stakeholder (Registrar/Academic Affairs) confirmation. |
| OQ#3 | Are real-time conflicts persisted (conflict-log) or transient? | Data model, audit | Recommend transient here; persistence owned by conflict-log story. **Confirm.** |
| OQ#4 | Travel-time buffer: data source and buffer value(s)? No travel-time/distance model exists. | TRAVEL_TIME feasibility | Depends on multi-campus (A4-35) / building-distance data. [TBD] |
| OQ#5 | Faculty hard-availability check is a stub (returns false). Does A4-16 depend on it being implemented first? | FACULTY_HARD_BLOCK correctness | Likely a prerequisite fix. **Confirm sequencing.** |
| OQ#6 | Delivery transport: Spring WebSocket/STOMP (org steering is Java/Spring) vs another. Label says `websocket`. | FR-6 design | Design-phase decision; org standard suggests Spring STOMP. |
| OQ#7 | Occupancy source: build a new loader that hydrates occupancy from persisted `ScheduledSession` rows (CSPState is generation-coupled). | Reuse vs new component | Design-phase; likely a new lightweight occupancy index. |
| OQ#8 | Multi-slot and fortnightly (A4-13) sessions — check conflicts only on occurring weeks/covered slots. | Correctness for labs/fortnightly | Reuse A4-13 recurrence-overlap logic. |
| OQ#9 | Caching for 50+ concurrent coordinators (hot-path occupancy). | Performance/NFR | Design-phase; confirm load target. |

## 17. Traceability

| BRD | Requirement | FR / AC |
|---|---|---|
| 6.9 | Real-time faculty double-booking | FR-1/FR-3 CT-1; AC1 |
| 6.9 | Real-time room double-booking | FR-3 CT-2; AC2 |
| 6.9 | Student/batch clash | FR-3 CT-3; AC3 (batch/section; student-level = OQ#1) |
| 6.9 | Max daily/weekly teaching hours | FR-3 CT daily/weekly; (AC via reuse) |
| 8 | Conflict checks < 2 seconds | FR-5, CT-SLA; AC1/AC6 |
| 7.3 | Room capacity ≥ strength | FR-3 CT capacity; AC4 |
| 7.3 | Location/transit buffer | FR-3 TRAVEL_TIME — DEFERRED pending OQ#4 |
| 7.4 | Max consecutive teaching hours | FR-3 CT consecutive; AC5 |
| 7.1 | Prerequisite mapping | FR-3 PREREQUISITE_SEQUENCE — DEFERRED pending OQ#2 (stakeholder) |

---

*Prepared for the Requirement Generation subtask (A4-72) of A4-16. Contains open
questions requiring stakeholder confirmation before design derivation; items
tagged DEFERRED are explicitly not asserted as firm requirements in this phase.*
