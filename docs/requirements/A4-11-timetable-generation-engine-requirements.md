# Requirement Document — Timetable Generation Engine

## 1. Introduction

This document captures the requirements for the core timetable generation engine — the system's ability to auto-generate a first-draft weekly timetable for a department based on all configured master data, hard constraints, and soft constraints. This is the most computationally complex component in UTMS: a constraint satisfaction problem solver that must produce a valid, high-quality schedule within a 2-minute time bound.

The engine does NOT handle: timeout/infeasibility (A4-12), fortnightly patterns (A4-13), locked slots/partial re-generation (A4-14), drag-drop editing (A4-15), or real-time conflict detection (A4-16). Those are separate stories that consume or extend this engine's output.

## 2. User Story

**A4-11:** As a Department Coordinator, I want to trigger auto-generation of a first-draft weekly timetable for my department based on all configured master data, hard constraints, and soft constraints, so that I have a valid starting point to review and refine rather than building from scratch.

**Story Points:** 8

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Triggers generation for their department, reviews output (scores, violations) |
| Scheduling Engine (system) | Reads all master data, applies constraints, generates session placements |
| Master Data Services (A4-2 to A4-10) | Provide input data: courses, faculty, rooms, calendar, time-slots, availability, blocks |
| Conflict Detection (A4-16) | Consumes the generated draft for validation |
| Approval Workflow (A4-18) | Receives the draft when coordinator submits |

## 4. User Journeys

### Journey 1: Coordinator Triggers Generation (Happy Path)

**Before:**
- Master data configured: courses with L-T-P and faculty assignments, rooms with capacity/equipment, time-slot grid per campus, academic calendar with holidays/exam windows, faculty availability declared, resource blocks active.
- Coordinator has selected department and semester.

**During:**
1. Coordinator navigates to scheduling module and selects "Generate Timetable."
2. System validates preconditions: all required data exists (courses have faculty, rooms available, calendar defined, grid configured).
3. System creates a GenerationRequest and starts the engine.
4. Engine loads: courses for this department/semester, assigned faculty with availability, eligible rooms, time-slot grid, calendar (working days), active blocks, institution-level common slots (CCC/UWE).
5. Engine applies hard constraints (no violations allowed in output): faculty double-booking, room double-booking, batch clashes, capacity mismatches, hard blocks, common slot reservations.
6. Engine optimizes soft constraints (best-effort): faculty preferences, room proximity, gap minimization, soft resource holds.
7. Engine produces a TimetableDraft with: sessions placed in slots, feasibility score, quality score, list of unresolved soft-constraint violations.
8. Coordinator sees the draft with scores and violations.

**After:**
- Draft is available for review in the editor (A4-15).
- Coordinator can lock slots, manually adjust, or re-run.
- Audit trail records generation event.

### Journey 2: Precondition Validation Fails

**Before:** Some required data is missing (e.g., no time-slot grid configured for this campus).

**During:**
1. Coordinator triggers generation.
2. System checks preconditions.
3. System finds missing data.
4. System returns a clear error listing what's missing (e.g., "No time-slot grid configured for Campus A. Configure before generating.").

**After:** No draft created. Coordinator fixes the data gap and retries.

### Journey 3: Generation Completes with Soft Violations

**Before:** All data present, but not all soft constraints satisfiable.

**During:**
1. Engine completes within 2 minutes.
2. Output: draft with all sessions placed, zero hard constraint violations.
3. Quality score < 100% because some soft constraints were relaxed.
4. Violations list shows: "Faculty Dr. Kumar: morning preference not satisfied (assigned afternoon slots)."

**After:** Coordinator reviews violations, decides whether to accept or manually adjust.

## 5. Functional Requirements

### FR-1: Generation Trigger

- FR-1.1: The system shall allow a coordinator to trigger timetable generation for a specific department and semester.
- FR-1.2: The system shall validate preconditions before starting generation: (a) academic calendar exists for this campus/semester, (b) time-slot grid exists for this campus, (c) working day pattern exists for this campus, (d) at least one course exists for this department with faculty assigned, (e) at least one eligible room exists.
- FR-1.3: If any precondition fails, the system shall return a clear error listing all missing prerequisites — not just the first one found.
- FR-1.4: If preconditions pass, the system shall create a GenerationRequest record and start the engine [TBD: async vs sync — see Open Question #1].

### FR-2: Input Data Loading

- FR-2.1: The engine shall load all courses for the target department/semester, including their L-T-P split (which determines the number and duration of weekly sessions to generate).
- FR-2.2: The engine shall load all faculty assigned to those courses, including their availability windows (hard blocks), soft preferences, and campus associations.
- FR-2.3: The engine shall load all eligible rooms: rooms on the campus with capacity >= batch strength and matching equipment tags for courses that require them.
- FR-2.4: The engine shall load the time-slot grid for the campus (teaching slots, breaks, lunch — respecting day-specific overrides per A4-10 design).
- FR-2.5: The engine shall load the academic calendar to determine which days are working days (excludes holidays, exam windows, orientation periods).
- FR-2.6: The engine shall load all active resource blocks (hard and soft) on rooms and assets for the semester period.
- FR-2.7: The engine shall load institution-level common slots (CCC/UWE) as pre-placed hard constraints that cannot be moved (BRD 7.7).

### FR-3: Session Derivation from Courses

- FR-3.1: For each course, the engine shall derive the number and type of weekly sessions based on L-T-P split [subject to OQ#9 — exact mapping depends on grid slot durations]:
  - L hours: lecture sessions (count and duration determined by campus grid configuration)
  - T hours: tutorial sessions (count and duration determined by campus grid configuration)
  - P hours: practical sessions (may require contiguous block scheduling — detailed rules in A4-24/Story 23)
- FR-3.2: Each derived session shall be a "variable" that the engine must place into a valid (day, time-slot, room) combination.
- FR-3.3: The total number of sessions to place = derived from L+T+P across all courses [subject to OQ#9 — not necessarily a 1:1 hour-to-session mapping].

### FR-4: Hard Constraint Enforcement

The engine's output shall have ZERO hard constraint violations. These are inviolable:

- FR-4.1: **Faculty non-double-booking** — a faculty member cannot be assigned to two sessions at the same time on the same day.
- FR-4.2: **Room non-double-booking** — a room cannot host two sessions at the same time on the same day.
- FR-4.3: **Batch non-clash** — a batch/section cannot have two sessions at the same time.
- FR-4.4: **Room capacity** — assigned room capacity must be >= batch/section strength.
- FR-4.5: **Equipment matching** — if a course requires specific equipment tags, the assigned room must have those tags.
- FR-4.6: **Hard availability blocks** — no session placed during a faculty member's hard unavailability window.
- FR-4.7: **Hard resource blocks** — no session placed in a room/asset during an active hard block.
- FR-4.8: **Calendar exclusion** — no session placed on non-working days (holidays, exam windows, orientation).
- FR-4.9: **Time-slot grid conformance** — sessions can only be placed in defined teaching slots (not breaks, not lunch, not outside grid bounds).
- FR-4.10: **Common slot reservation (CCC/UWE)** — institution-level common slots are pre-placed and cannot be overwritten by department scheduling (BRD 7.7).
- FR-4.11: **Faculty max daily/weekly teaching load** — the engine shall not assign a faculty member more sessions than their configured maximum daily or weekly load allows (per cadre norms from A4-32 or per-faculty override from A4-4). (BRD 6.10: "accreditation norms" as engine constraint; BRD 7.2: "Min/max weekly teaching load").
- FR-4.12: **Faculty max consecutive teaching hours** — the engine shall not assign a faculty member sessions that would result in more than the configured maximum consecutive teaching hours (BRD 7.4: "max consecutive teaching hours for faculty"). This is distinct from daily/weekly totals — 4 back-to-back hours can violate consecutive limit while staying under the daily cap.

### FR-5: Soft Constraint Optimization

The engine shall attempt to satisfy these, relaxing where necessary with justification:

- FR-5.1: **Faculty time-of-day preference** — respect declared morning/afternoon preference where possible.
- FR-5.2: **Faculty session distribution** — respect consecutive vs. spread preference.
- FR-5.3: **Room proximity** — for back-to-back sessions (same faculty or same batch), prefer rooms in the same building/floor to minimize travel.
- FR-5.4: **Gap minimization** — reduce idle gaps between sessions in a faculty's or batch's daily schedule.
- FR-5.5: **Soft resource holds** — avoid placing sessions in soft-blocked slots unless no alternative exists. If overridden, record justification (calls BlockService.recordSoftBlockOverride from A4-8).
- FR-5.6: **Day-pattern saturation balancing** — distribute load across alternative day patterns rather than compounding clashes on saturated/preferred days (BRD 7.7).

### FR-6: Output

- FR-6.1: The engine shall produce a **TimetableDraft** containing all placed sessions with their assigned (day, time-slot, room, faculty, course, batch/section) tuples.
- FR-6.2: The engine shall compute and return a **feasibility score** (0.0 to 1.0): proportion of sessions successfully placed vs. total sessions required.
- FR-6.3: The engine shall compute and return a **quality score** (0.0 to 1.0): weighted satisfaction of soft constraints [weights TBD — see Open Question #4].
- FR-6.4: The engine shall return a **violations list**: for each soft constraint relaxed, describe what was relaxed, for whom, and why.
- FR-6.5: The engine shall complete within **2 minutes** (BRD Section 8). If it cannot complete, behavior is defined by A4-12 (Timeout Handling).

### FR-7: Performance Target

- FR-7.1: The engine shall generate a full department timetable within 2 minutes (BRD Section 8: "Auto-generate a full department timetable within 2 minutes"). Tech steering performance guideline sizes this at ~40 sections.
- FR-7.2: The engine shall handle the institution's scale subset: a department's portion of 10,000+ students, 500+ faculty, 200+ rooms.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-ENG-1 | Faculty non-double-booking | Hard |
| HC-ENG-2 | Room non-double-booking | Hard |
| HC-ENG-3 | Batch non-clash | Hard |
| HC-ENG-4 | Room capacity >= batch strength | Hard |
| HC-ENG-5 | Equipment tag matching | Hard |
| HC-ENG-6 | Faculty hard availability respected | Hard |
| HC-ENG-7 | Hard resource blocks respected | Hard |
| HC-ENG-8 | Calendar exclusion | Hard |
| HC-ENG-9 | Time-slot grid conformance | Hard |
| HC-ENG-10 | Common slot reservation (CCC/UWE) | Hard |
| HC-ENG-11 | Faculty max daily/weekly teaching load | Hard |
| HC-ENG-12 | Faculty max consecutive teaching hours | Hard |
| SC-ENG-1 | Faculty time preference | Soft |
| SC-ENG-2 | Faculty distribution preference | Soft |
| SC-ENG-3 | Room proximity | Soft |
| SC-ENG-4 | Gap minimization | Soft |
| SC-ENG-5 | Soft resource hold avoidance | Soft |
| SC-ENG-6 | Day-pattern saturation balancing | Soft |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Faculty availability windows | A4-5 (AvailabilityQueryService) |
| Room capacity and equipment tags | A4-6 |
| Active resource blocks | A4-8 |
| Academic calendar (working days) | A4-9 (CalendarQueryService) |
| Time-slot grid (effective slots) | A4-10 (TimeSlotGridService) |
| Course L-T-P | A4-3 |
| Faculty-course assignment | A4-4 |
| Batch strength | A4-2 |
| Soft block override recording | A4-8 (BlockService.recordSoftBlockOverride) |
| Faculty soft preferences | A4-5 (AvailabilityQueryService.getSoftPreferences) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| departmentId | Required, must reference existing active department |
| semester/academic year | Required, must reference existing calendar |
| Generated sessions | Each: valid day, valid slot, valid room (capacity+equipment), valid faculty (available, within load limits) |
| Feasibility score | 0.0 to 1.0 |
| Quality score | 0.0 to 1.0 |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Complete within 2 minutes for ~40 sections (BRD Section 8 + tech steering) |
| Isolation | Generation should not block other system operations |
| Determinism | [TBD — see Open Question #3] |
| Audit | Generation trigger + result recorded |

## 10. Acceptance Criteria

1. Given master data configured, When coordinator triggers generation, Then draft produced within 2 minutes.
2. Given generated draft, When presented, Then includes feasibility and quality scores.
3. Given hard constraints, When engine completes, Then zero hard violations in output.
4. Given unsatisfiable soft constraints, When engine completes, Then violations list describes what was relaxed.
5. Given CCC/UWE common slots, When engine runs, Then treated as pre-placed hard constraints.
6. Given day-pattern saturation, When engine runs, Then balances across alternative patterns.
7. Given back-to-back sessions, When rooms assigned, Then prefers proximity.
8. Given daily schedule, When sessions assigned, Then minimizes gaps.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| GenerationRequest | id, departmentId, academicYear, semester, status, triggeredBy, triggeredAt, completedAt | triggers one TimetableDraft |
| TimetableDraft | id, generationRequestId, departmentId, semester, status, feasibilityScore, qualityScore | has many ScheduledSessions |
| ScheduledSession | id, draftId, courseId, facultyId, batchId, sectionId, roomId, dayOfWeek, slotDefinitionId | belongs to TimetableDraft |
| SoftConstraintViolation | id, draftId, constraintType, affectedEntityType, affectedEntityId, description, relaxationReason | belongs to TimetableDraft |

## 12. Dependencies

| Dependency | Blocking? |
|---|---|
| A4-2 (Batches) | Yes |
| A4-3 (Courses L-T-P) | Yes |
| A4-4 (Faculty competency) | Yes |
| A4-5 (Availability) | Yes |
| A4-6 (Rooms) | Yes |
| A4-8 (Blocks) | Yes |
| A4-9 (Calendar) | Yes |
| A4-10 (Time-slots) | Yes |
| A4-12 (Timeout) | No — extends this |
| A4-14 (Locked slots) | No — extends this |

## 13. Assumptions

1. Engine generates for ONE department at a time.
2. Faculty-to-course assignment is pre-done (engine places, doesn't assign).
3. Canonical weekly pattern (fortnightly is A4-13).
4. Feasibility score = sessions_placed / total_required.
5. Quality score = weighted_soft_satisfied / total_applicable [weights TBD].

## 14. Consistency Notes

- 12 hard constraints here (HC-ENG-1 through HC-ENG-12). A4-16 (Conflict Detection) detects 9 conflict types including workload violations (daily/weekly + consecutive). The delta: HC-ENG-8 (calendar exclusion), HC-ENG-9 (grid conformance), and HC-ENG-10 (common slot reservation) are generation-time constraints that the engine enforces structurally — they don't need post-placement "detection" because the engine simply never places there.
- FR-5.5 references A4-8's BlockService.recordSoftBlockOverride (KD-33 write path).
- Engine output (TimetableDraft + Sessions) is input to A4-14, A4-16, A4-18.

## 15. Out of Scope

- Timeout/infeasibility → A4-12
- Fortnightly patterns → A4-13
- Locked slots / partial re-gen → A4-14
- Drag-drop editing → A4-15
- Real-time conflict detection → A4-16
- Approval workflow → A4-18
- Lab technician availability bounds → A4-24 (Story 23)
- Algorithm choice → Design decision (R2: no algorithm lock-in)

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Sync vs async generation? | API design, UX | System Design / UX |
| 2 | Concurrent generations for same dept? | Concurrency model | System Design |
| 3 | Deterministic or randomized? | Algorithm, testability | System Design |
| 4 | Quality score weights — who configures? | FR-6.3, priorities | Academic Affairs |
| 5 | Engine assigns faculty, or only places pre-assigned? | FR-2.1, preconditions | Academic Affairs |
| 6 | Existing draft on re-generation — overwrite or version? | Data model | System Design |
| 7 | Travel-time buffer within single-dept generation? | FR-4 scope | Academic Affairs |
| 8 | How are cross-listed courses (BRD 7.1: "requiring synchronized slots") handled when departments generate independently (Assumption 1)? Options: (a) first dept claims shared slot, second treats as locked; (b) cross-listed pre-placed institution-wide before dept generation; (c) coordination step after both generate. | Cross-department consistency | Academic Affairs / Registrar |
| 9 | How does L-T-P map to sessions with mixed slot durations (BRD 7.7)? 3 lecture hours with 90-min slots = 2 sessions or 3? Mapping depends on grid config and may not be 1:1. Who defines it? | FR-3.1, session derivation | Academic Affairs / System Design |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.10 — "auto-generate using constraint-based algorithm" | FR-1, FR-4, FR-5, FR-6 |
| 6.2 — "generate weekly timetable per batch based on credit structure, faculty, rooms" | FR-2, FR-3 |
| 6.10 — "feasibility/quality score and violations list" | FR-6.2, FR-6.3, FR-6.4 |
| Section 8 — "within 2 minutes" | FR-6.5, FR-7.1 |
| 7.7 — "day-pattern saturation balancing" | FR-5.6 |
| 7.7 — "institution-level common slots (CCC/UWE)" | FR-4.10 |
| 7.3 — "minimise travel time" | FR-5.3 |
| 7.2 — "preference weighting" | FR-5.1, FR-5.2 |
| 7.7 — "compressed practical windows" | FR-3.1 (P sessions, detailed in A4-24) |
| 6.10 — "accreditation norms" as engine constraint | FR-4.11 (max daily/weekly load per cadre) |
| 7.4 — "max consecutive teaching hours for faculty" | FR-4.12 (max consecutive) |
