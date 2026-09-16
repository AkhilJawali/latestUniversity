# Requirement Document — Locked Slot Preservation and Partial Re-Generation

## 1. Introduction

This document captures the requirements for two related capabilities that extend the timetable generation engine (A4-11): (1) **locked slot preservation** — the coordinator can fix specific sessions in place so the engine never moves them, and (2) **partial re-generation** — the coordinator can re-run the engine on a selected subset of the timetable without disturbing locked sessions or already-approved sections.

This story EXTENDS A4-11. It does not redefine the engine's constraint model or solver; it adds the notions of *immovable pre-placed sessions* and *scoped regeneration* on top of the existing generation pipeline.

## 2. User Story

**A4-14:** As a Department Coordinator, I want to lock specific sessions in fixed slots before running the engine, and re-run generation on a subset of the timetable without disturbing already-approved or locked sections, so that I retain control over pre-decided placements and can iteratively refine parts of the schedule.

**Story Points:** 5

**BRD Requirements:** 6.2 — "Allow department coordinators to lock specific sessions before running the auto-suggestion engine"; 6.10 — "Support re-running the engine on a subset of the timetable without disturbing already-approved sections."

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Locks/unlocks sessions, selects the subset to regenerate, triggers partial re-generation, reviews the result |
| Scheduling Engine (system, A4-11) | Preserves locked and out-of-scope sessions as fixed occupancy; regenerates only the selected subset |
| GenerationRequest / TimetableDraft (entities from A4-11) | Extended to carry lock state and re-generation scope |
| Infeasibility Reporting (A4-12) | Consumed when locks/approved sessions make the subset infeasible |
| Audit Trail (common) | Records lock changes and partial re-generation events |

## 4. User Journeys

### Journey 1: Coordinator Locks Sessions Before Generation (Happy Path)

**Before:**
- A draft timetable exists (produced by A4-11), or the coordinator is preparing inputs before generation.
- The coordinator has identified specific sessions that must stay exactly where they are (e.g., a guest lecture fixed on Friday 10:00 in LH-1).

**During:**
1. The coordinator marks those sessions as locked.
2. The coordinator triggers generation (or re-generation).
3. The engine treats each locked session as a pre-placed, immovable assignment — it consumes the faculty, room, and batch occupancy for that (day, slot) but is never moved or reassigned.
4. The engine schedules all other sessions around the locked ones, honoring all hard constraints.
5. The engine produces a draft in which every locked session is at its exact original (day, slot, room, faculty).

**After:**
- The coordinator reviews the draft; locked sessions are unchanged.
- The audit trail records which sessions were locked and by whom.

### Journey 2: Coordinator Re-Generates a Subset

**Before:**
- A draft exists. Part of it is satisfactory (or approved); part needs rework.
- The coordinator selects the subset to regenerate (see OQ#3 for the selector granularity).

**During:**
1. The coordinator selects the subset (e.g., the sessions of specific batches) and triggers partial re-generation.
2. The engine loads: the selected subset as free variables to place, and all non-selected sessions (locked, approved, and simply out-of-scope) as fixed occupancy.
3. The engine regenerates only the selected subset, scheduling around the fixed occupancy without violating hard constraints.
4. The engine produces an updated draft in which non-selected sessions are unchanged and only the selected subset has new placements.

**After:**
- The coordinator reviews; only the intended subset changed.
- The audit trail records the re-generation scope and outcome.

### Journey 3: Locked/Approved Sessions Make the Subset Infeasible

**Before:**
- The coordinator has locked sessions and/or has approved sections, then triggers (partial) re-generation.

**During:**
1. The engine attempts to place the free (selected) sessions around the fixed ones.
2. A free session's domain becomes empty — no valid slot+room satisfies all hard constraints given the fixed occupancy.
3. The engine reports infeasibility (consuming A4-12's infeasibility detection and report), and the report indicates that the locked/approved placements are among the constraints in conflict.

**After:**
- The coordinator can unlock a session, widen the subset, or adjust inputs, then re-run.
- The audit trail records the infeasibility outcome.

### Journey 4: Coordinator Unlocks a Session

**Before:** A session is currently locked.

**During:**
1. The coordinator unlocks the session.
2. On the next generation/re-generation, that session becomes a free variable the engine may move.

**After:** The unlock is persisted and audited. *(Subject to OQ#5 — whether unlocking is part of this story or the editor A4-15.)*

## 5. Functional Requirements

### FR-1: Session Locking

- FR-1.1: The system shall allow a session in a draft to be marked as **locked**, persisting the lock state on the session (the `is_locked` attribute already exists on the session record from A4-11).
- FR-1.2: The system shall allow a locked session to be **unlocked**, clearing the lock state [ownership of the lock/unlock UI is subject to OQ#1].
- FR-1.3: A locked session shall retain its exact assignment: day, time-slot, room, faculty, batch/section. Locking captures the full placement, not just the time.
- FR-1.4: The system shall record every lock and unlock action in the audit trail (who, when, which session).

### FR-2: Locked Slot Preservation During Generation

- FR-2.1: When the engine runs, it shall treat every locked session as a **pre-placed, immovable assignment** — analogous to how institution common slots (CCC/UWE) are pre-placed in A4-11 (KD-52).
- FR-2.2: The engine shall never move, reassign, or overwrite a locked session, regardless of soft-constraint optimization opportunities.
- FR-2.3: Locked sessions shall contribute to occupancy: their faculty, room, and batch are considered occupied for their (day, slot), so the engine schedules other sessions around them without hard-constraint violations.
- FR-2.4: The engine output shall contain every locked session at its exact original placement (zero drift).

### FR-3: Partial Re-Generation Scope

- FR-3.1: The system shall allow the coordinator to trigger re-generation scoped to a **selected subset** of the timetable rather than the whole department [subset granularity — batch, section, or course — is subject to OQ#3].
- FR-3.2: During partial re-generation, only sessions within the selected subset shall be treated as free variables the engine may (re)place.
- FR-3.3: All sessions outside the selected subset — including locked sessions, approved sections, and simply unselected sessions — shall be treated as fixed occupancy and shall remain byte-identical to their prior placement in the output.
- FR-3.4: The engine shall schedule the selected subset around all fixed occupancy without violating any hard constraint (HC-ENG-1 through HC-ENG-12 from A4-11 continue to apply to the regenerated subset).

### FR-4: Approved Section Preservation

- FR-4.1: The system shall preserve sessions belonging to an **approved section** across any (partial or full) re-generation — approved sessions are never moved [the mechanism that marks a section "approved" is subject to OQ#2, since the approval workflow (A4-18) is a separate, later story].
- FR-4.2: Approved sessions shall be treated as fixed occupancy in the same manner as locked sessions (FR-2.3).
- FR-4.3: When a re-generation combines locked slots AND approved sections, both categories shall be preserved simultaneously (neither is moved).

### FR-5: Infeasibility When Locks/Approvals Conflict

- FR-5.1: If the fixed occupancy (locked and/or approved sessions) leaves no valid placement for one or more free sessions, the engine shall report infeasibility using A4-12's infeasibility detection and report.
- FR-5.2: The infeasibility report shall indicate that locked and/or approved placements are among the constraints contributing to the conflict, so the coordinator understands the locks are part of the problem (BRD 6.2 / A4-14 AC#4).

### FR-6: Output and Versioning

- FR-6.1: The output of a partial re-generation shall be a TimetableDraft in which non-selected, locked, and approved sessions are unchanged and only the selected subset reflects new placements.
- FR-6.2: Whether partial re-generation produces a new draft version or mutates the existing draft in place is subject to OQ#4 (A4-11 PD-72 established "new version, supersede old" for full generation).
- FR-6.3: The system shall record the re-generation scope (which subset was regenerated) and outcome in the audit trail.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-LOCK-1 | Locked sessions are immovable — preserved at their exact (day, slot, room, faculty) placement through any generation run | Hard |
| HC-LOCK-2 | Approved-section sessions are immovable during (partial or full) re-generation | Hard |
| HC-LOCK-3 | Partial re-generation regenerates only the selected subset; all non-selected sessions remain fixed | Hard |
| HC-LOCK-4 | Locked and approved sessions consume faculty/room/batch occupancy that the regenerated subset must schedule around | Hard |

## 7. Constraints Referenced from Other Documents

- **HC-ENG-1 through HC-ENG-12 (A4-11):** All engine hard constraints continue to apply to the regenerated subset. This document does not redefine them.
- **Pre-placement pattern (A4-11 KD-52):** Locked/approved sessions reuse the institution-common-slot pre-placement mechanism (immovable assignments before solving).
- **Infeasibility detection + report (A4-12 FR-3):** Consumed by FR-5 when locks/approvals cause infeasibility. This document does not redefine infeasibility.
- **Draft versioning (A4-11 PD-72):** Referenced by FR-6.2; the partial-regen versioning decision is an open question here.

## 8. Validation Rules

| Rule | Description |
|---|---|
| Lock target exists | A lock/unlock action must reference an existing session in an existing draft. |
| Subset non-empty | A partial re-generation request must select at least one session/batch/section to regenerate. |
| Subset belongs to draft | The selected subset must belong to the draft being regenerated. |
| Lock captures full placement | A session can only be locked if it has a complete placement (day, slot, room, faculty). |

## 9. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Partial re-generation of a subset should complete within the A4-11 time bound (2 minutes) or fall back to A4-12 timeout handling; regenerating a smaller subset should generally be faster than a full run. |
| Correctness | Zero drift: locked and approved sessions must be identical before and after (byte-for-byte on the placement fields). |
| Auditability | Every lock/unlock and every partial re-generation (scope + outcome) is recorded in the audit trail. |
| Isolation | Partial re-generation must not modify sessions outside the selected subset. |

## 10. Acceptance Criteria

1. **Given** a coordinator locks 3 sessions in specific slots, **When** the engine runs, **Then** those 3 sessions remain exactly in their locked positions — never moved.
2. **Given** an already-approved section of the timetable, **When** partial re-generation is triggered for a different section, **Then** the approved section is completely untouched.
3. **Given** a subset of batches selected for re-generation, **When** the engine runs, **Then** only those batches' sessions are regenerated; all others remain fixed.
4. **Given** locked slots that make the problem infeasible, **When** the engine cannot find a solution, **Then** it reports infeasibility indicating the locks are part of the conflict (consumes A4-12).
5. **Given** a combination of locked slots and approved sections, **When** re-generation runs, **Then** both locked slots AND approved sections are preserved.
6. **Given** a locked session, **When** the coordinator unlocks it and re-runs, **Then** the engine is free to move that session (subject to OQ#5).

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| ScheduledSession (extended, A4-11) | existing fields + `isLocked` (already present) | belongs to TimetableDraft |
| RegenerationRequest (conceptual) | draftId, scopeSelector (subset), triggeredBy, triggeredAt | targets one TimetableDraft |
| Approved-section marker | (mechanism TBD — see OQ#2) | associates sessions/sections with an approved state |

No new column types or index strategies are specified here — those are design decisions.

## 12. Dependencies

| Dependency | Blocking? |
|---|---|
| A4-11 (Generation Engine) | Yes — this story extends it |
| A4-12 (Infeasibility Reporting) | Yes — FR-5 consumes its report |
| A4-18 (Approval Workflow) | Partial — provides the real "approved" marker (see OQ#2) |
| A4-15 (Editor) | No — may own the lock/unlock UI (see OQ#1) |

## 13. Assumptions

1. The `is_locked` attribute already exists on the session record (A4-11) and is the persistence point for lock state.
2. Locked/approved sessions reuse A4-11's pre-placement mechanism; no new solver algorithm is introduced.
3. Fortnightly recurrence (A4-13) is orthogonal; a locked session keeps whatever recurrence it already has.
4. "Fortnightly is A4-13, timeout/infeasibility is A4-12" — this story consumes those, it does not redefine them.

## 14. Consistency Notes

- 4 hard constraints owned (HC-LOCK-1 through HC-LOCK-4). These are preservation/scoping constraints layered on top of A4-11's 12 engine constraints — they do not overlap or redefine HC-ENG-*.
- FR-5 defers all infeasibility mechanics to A4-12; this document only specifies that locks/approvals must be surfaced as contributing constraints.
- The "approved section" concept (FR-4) depends on a marker that the approval workflow (A4-18) owns; until A4-18 exists, OQ#2 governs the interim behavior. This is flagged, not silently assumed.

## 15. Out of Scope

- The generation engine itself → A4-11.
- Timeout and infeasibility detection/reporting → A4-12 (consumed here).
- Fortnightly/alternate-week recurrence → A4-13.
- Drag-and-drop editing and the lock/unlock UI affordance → possibly A4-15 (see OQ#1).
- The approval workflow that produces "approved" status → A4-18 (see OQ#2).
- Algorithm/solver design → design phase (no algorithm lock-in here).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| OQ#1 | Where does the lock/unlock action live — this story's API, or the drag-drop editor (A4-15)? | Scope boundary, API surface | System Design |
| OQ#2 | What marks a section "approved" for FR-4, given the approval workflow (A4-18) is a later story? Interim marker, or defer FR-4 until A4-18? | Precondition for AC#2/#5 | System Design / Academic Affairs |
| OQ#3 | Re-generation subset granularity — by batch, by section, or by course? BRD says "subset of the timetable"/"subset of batches"; ACs mention both "section" and "batches". | Core API design | Academic Affairs / System Design |
| OQ#4 | Does partial re-generation create a new draft version (A4-11 PD-72) or mutate the existing draft in place? | Data model, history | System Design |
| OQ#5 | Is unlock part of this story (FR-1.2 / AC#6), or handled by the editor (A4-15)? | Scope boundary | System Design |
| OQ#6 | If pre-existing locked/approved sessions conflict with each other (before any re-gen), how is that surfaced — at lock time or at generation time? | Edge case handling | System Design |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.2 — "Allow department coordinators to lock specific sessions before running the auto-suggestion engine" | FR-1, FR-2 |
| 6.10 — "Support re-running the engine on a subset of the timetable without disturbing already-approved sections" | FR-3, FR-4 |
| 6.2 / AC#4 — locked slots surfaced when infeasible | FR-5 (consumes A4-12) |
| 6.10 — subset re-generation output preserves the rest | FR-6 |
