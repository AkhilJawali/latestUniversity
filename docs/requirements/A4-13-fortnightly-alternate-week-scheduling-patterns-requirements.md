# Requirement Document — Fortnightly and Alternate-Week Scheduling Patterns

## 1. Introduction

This document captures the requirements for extending UTMS scheduling beyond simple weekly repetition to support **fortnightly** (every-other-week) and **alternate-week** recurrence patterns for individual sessions. The canonical model assumed by the generation engine (A4-11) is that every placed session repeats identically every teaching week. This story introduces the ability for a session to occur on only some weeks — for example, a lab that runs on "every other Monday", or two lab sub-groups sharing one slot where Group A occupies odd weeks and Group B occupies even weeks. The same capability supports **special courses** whose sessions do not run every week.

This capability is a cross-cutting metadata concern: it attaches a recurrence pattern to a session and then changes how three downstream behaviors interpret that session — conflict detection (must only check the weeks a session actually occurs), timetable display (must show which weeks a session runs), and calendar export (must emit events only on the correct weeks).

This document owns the **recurrence pattern concept** and the **alternate-week non-conflict rule**. It does NOT own session placement (A4-11), the drag-drop editor UI (A4-15), the conflict detection engine itself (A4-16), or the calendar feed implementation (A4-39) — those stories consume the pattern defined here.

## 2. User Story

**A4-13:** As a Department Coordinator, I want to define sessions with fortnightly or alternate-week recurrence patterns (e.g., Lab Group A on Week 1, Lab Group B on Week 2), so that the system supports scheduling patterns beyond simple weekly repetition as required for split labs and special courses.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| Department Coordinator | Assigns/edits a fortnightly or alternate-week pattern on a session; reviews the resulting schedule. Primary owner. |
| Scheduling Engine (A4-11) | Produces sessions; may set a recurrence pattern when deriving split-lab sub-group sessions [subject to OQ#5]. Consumer/producer of the pattern field. |
| Drag-Drop Editor (A4-15) | UI surface where a coordinator sets/edits the pattern and where the pattern is displayed. Consumer. |
| Conflict Detection (A4-16) | Reads each session's pattern to decide whether two sessions actually co-occur before flagging a clash. Consumer. |
| Calendar Export (A4-39) | Reads the pattern to emit calendar events only on the weeks the session occurs. Consumer. |
| Academic Calendar (A4-9) | Provides the semester start date used as the anchor for computing week parity, and the working-day/holiday data. Referenced. |
| Faculty / Student | View the published timetable and calendar feed; see which weeks a fortnightly session runs. Read-only. |

## 4. User Journeys

### Journey 1: Coordinator Marks a Session as Fortnightly (Happy Path)

**Before:**
- A session exists (placed by the engine A4-11 or created in the editor A4-15) for a course/batch on a given (day, time-slot, room, faculty).
- The academic calendar for the campus/semester exists with a defined semester start date (A4-9).

**During:**
1. Coordinator opens the session in the editor.
2. Coordinator changes its recurrence from "Weekly" (default) to "Fortnightly" and selects which week group it runs on (e.g., Week-A / odd weeks).
3. System validates the pattern (valid recurrence type, valid week group, anchor resolvable from the calendar).
4. System persists the recurrence pattern on the session.

**After:**
- The session now occurs only on the selected alternating weeks.
- Conflict detection re-evaluates using week-awareness.
- The session displays its recurrence pattern in timetable views.
- The calendar feed emits events only on the occurring weeks.
- Audit trail records the change.

### Journey 2: Two Lab Sub-Groups Share One Slot on Alternating Weeks

**Before:** A lab batch is split into two sub-groups (Group A, Group B). One slot (same day, same time, same room) is available.

**During:**
1. Coordinator places Group A's lab session in the slot with recurrence "Fortnightly — Week-A".
2. Coordinator places Group B's lab session in the same slot with recurrence "Fortnightly — Week-B".
3. Conflict detection evaluates: the two sessions share the same (day, slot, room) but occupy disjoint week groups.
4. System does NOT report a room/batch/faculty clash, because the sessions never co-occur.

**After:** Both sub-groups are scheduled in the same physical slot on alternating weeks with no conflict flagged.

### Journey 3: Conflict Detection Respects Week-Awareness (Boundary)

**Before:** A weekly session and a fortnightly session are placed in the same (day, slot) for the same batch.

**During:**
1. Conflict detection runs.
2. It determines the weekly session occurs on every week; the fortnightly session occurs on a subset.
3. Since their occurring weeks overlap (the fortnightly weeks are a subset of the weekly session's weeks), a clash IS reported for those overlapping weeks.

**After:** Coordinator is shown the conflict and resolves it. (Contrast with Journey 2, where two fortnightly sessions on opposite week groups never overlap.)

### Journey 4: Fortnightly Occurrence Falls on a Holiday (Failure/Edge)

**Before:** A fortnightly session is scheduled; one of its occurring weeks contains a date that is a holiday or non-working day (per A4-9).

**During:**
1. System evaluates occurrences against the calendar.
2. The occurrence on the holiday date is not a valid teaching occurrence.

**After:** Behavior is governed by Open Question #2 — either the occurrence is skipped (no shift) or it is flagged for coordinator attention. This document does not silently decide; it defers to OQ#2.

### Journey 5: Student Views Fortnightly Session on Timetable and Feed

**Before:** A published timetable contains fortnightly sessions.

**During:**
1. Student opens their personal timetable.
2. The fortnightly session is displayed with a clear recurrence indicator (which weeks it occurs).
3. Student subscribes to the calendar feed.
4. The feed contains events only on the alternating weeks the session actually runs.

**After:** Student sees an accurate schedule; no phantom events on non-occurring weeks.

## 5. Functional Requirements

### FR-1: Recurrence Pattern Assignment

- FR-1.1: The system shall allow a recurrence pattern to be associated with a session. Supported recurrence types: **Weekly** (default — occurs every teaching week) and **Fortnightly / Alternate-Week** (occurs every other teaching week). (BRD 6.2)
- FR-1.2: When a session is Fortnightly, the system shall record which **week group** it occupies (two disjoint groups covering alternating weeks — e.g., Week-A/Week-B or odd/even) [naming and representation subject to OQ#4].
- FR-1.3: In the absence of an explicitly assigned pattern, a session shall default to Weekly, preserving the canonical behavior assumed by A4-11 (A4-11 Assumption 3).
- FR-1.4: The system shall allow a coordinator to change a session's recurrence pattern and to revert a fortnightly session back to weekly.
- FR-1.5: A fortnightly session's recurrence is tied to its existing scheduled day and time-slot; the pattern determines *which weeks* the session runs, not which day. (Story AC #1 — "every other Monday")
- FR-1.6: Recurrence beyond fortnightly (e.g., every third week, monthly) is out of scope for this story — see Section 15 and OQ#6.

### FR-2: Week Parity Anchoring

- FR-2.1: The system shall determine, for any teaching date, which week group it belongs to (Week-A or Week-B), so that a fortnightly session's occurrences can be computed deterministically.
- FR-2.2: The week-parity computation shall be anchored to a fixed reference so it is stable and reproducible [anchor definition subject to OQ#1 — proposed default: the semester start date from the campus's academic calendar (A4-9), with the first teaching week = Week-A].
- FR-2.3: Week parity shall be computed relative to teaching weeks of the semester, using the academic calendar (A4-9) to establish the semester boundary. This document does not redefine the calendar — it references A4-9.

### FR-3: Alternate-Week Non-Conflict Rule

- FR-3.1: The system shall treat two sessions that share the same (day, time-slot) and the same contended resource (room, faculty, or batch/section) as **non-conflicting** if and only if they occupy **disjoint week groups** (i.e., they never occur in the same calendar week). (Story AC #2)
- FR-3.2: The system shall treat two sessions sharing the same (day, time-slot) and a contended resource as **conflicting** if their occurring weeks overlap in at least one teaching week (e.g., weekly vs. fortnightly, or two sessions on the same week group). (Story AC #3, Journey 3)
- FR-3.3: This non-conflict rule applies to all resource-contention conflict types that are otherwise time-slot based: faculty double-booking, room double-booking, and batch/section clash.

### FR-4: Week-Aware Conflict Detection Input

- FR-4.1: The system shall expose each session's recurrence pattern (type + week group) so that conflict detection (A4-16) can determine the set of weeks a session occurs before comparing two sessions. (Story AC #3)
- FR-4.2: For a fortnightly session, conflict checks against calendar-derived constraints (holiday, non-working day, exam window per A4-9) shall be evaluated only for the weeks the session actually occurs, not for every week. (Story AC #3)
- FR-4.3: A fortnightly session shall still be subject to ALL hard constraints from A4-11 (capacity, equipment, faculty availability, grid conformance, etc.) on the weeks it occurs — fortnightly recurrence relaxes only the "which weeks" dimension, nothing else.

### FR-5: Timetable Display of Recurrence

- FR-5.1: The system shall make a session's recurrence pattern available for display so that timetable views and the editor (A4-15) can clearly indicate which weeks a fortnightly session occurs. (Story AC #4)
- FR-5.2: The recurrence indicator shall distinguish weekly from fortnightly and, for fortnightly, indicate the week group (e.g., "Weeks A" / "odd weeks"). Exact visual representation is owned by the frontend story (A4-15); this document requires only that the data be available and unambiguous. [presentation format subject to OQ#4]

### FR-6: Calendar Feed Correctness

- FR-6.1: The system shall provide the recurrence pattern to the calendar export service (A4-39) so that exported feeds contain events only on the weeks a fortnightly session actually occurs. (Story AC #5)
- FR-6.2: A weekly session's feed behavior is unchanged (an event on every teaching week). This document adds only the fortnightly case.

### FR-7: Pattern Integrity Through Lifecycle

- FR-7.1: The recurrence pattern shall travel with the session through draft, submission, approval, and publication — it is not lost when a draft is submitted or published.
- FR-7.2: Editing a recurrence pattern after publication is governed by post-publication change management (A4-48) and is out of scope for this document beyond persisting the field.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-FN-1 | Two sessions sharing (day, slot) + a contended resource (room/faculty/batch) do NOT conflict if they occupy disjoint week groups | Hard (non-conflict rule) |
| HC-FN-2 | Two sessions sharing (day, slot) + a contended resource DO conflict if their occurring weeks overlap in any teaching week | Hard |
| HC-FN-3 | A fortnightly session must respect all A4-11 hard constraints on each week it occurs (fortnightly relaxes only the week dimension) | Hard |
| HC-FN-4 | Recurrence type must be one of the supported values (Weekly, Fortnightly); a fortnightly session must specify exactly one week group | Hard (validation) |
| SC-FN-1 | When the engine splits a lab batch into sub-groups sharing a slot, prefer assigning them to opposite week groups to maximize slot reuse [subject to OQ#5] | Soft |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Session placement and canonical weekly recurrence | A4-11 (ScheduledSession, Assumption 3) |
| Semester start date (week-parity anchor) | A4-9 (AcademicCalendar.semester_start_date) |
| "Is this date a working day / holiday / exam window?" | A4-9 (CalendarQueryService) |
| Resource-contention conflict types (faculty/room/batch double-booking) | A4-16 (Conflict Detection) — this doc supplies the week-awareness rule it applies |
| Calendar feed event emission | A4-39 (Calendar Export) |
| Recurrence display in editor/timetable views | A4-15 (Drag-Drop Editor) |
| Batch sub-group splitting for labs | Lab scheduling story (Story 23) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| recurrenceType | Required. One of {WEEKLY, FORTNIGHTLY}. Defaults to WEEKLY if unset. |
| weekGroup | Required when recurrenceType = FORTNIGHTLY; must be one of exactly two disjoint groups (e.g., WEEK_A / WEEK_B). Must be null/absent when recurrenceType = WEEKLY. |
| anchor resolvability | The session's campus/semester must have an academic calendar with a semester start date (A4-9), else week parity cannot be computed — reject with a clear error. |
| session existence | Recurrence can only be set on an existing session with a valid (day, slot) placement. |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Week-parity computation and week-aware conflict checks must not degrade conflict detection below its target (real-time feedback; A4-16). Parity computation shall be O(1) per date given the anchor. |
| Backward compatibility | Existing weekly sessions (no recurrence field) must continue to behave exactly as before (default WEEKLY). No migration of existing behavior. |
| Audit | Setting or changing a session's recurrence pattern shall be recorded in the audit trail (A4-43). |
| Determinism | Given the same anchor and calendar, week parity for any date must be deterministic and reproducible. |

## 10. Acceptance Criteria

1. **Given** a session, **When** a fortnightly/alternate-week pattern is configured (e.g., "every other Monday"), **Then** the session repeats on that pattern rather than every week. (Story AC #1)
2. **Given** two sub-groups of a lab batch, **When** alternate-week scheduling is applied with the two sub-groups on opposite week groups, **Then** Group A occupies Week-A and Group B occupies Week-B in the same slot without a conflict being reported. (Story AC #2)
3. **Given** a fortnightly session, **When** conflict detection runs, **Then** it only checks for conflicts on the weeks the session actually occurs (not every week). (Story AC #3)
4. **Given** a weekly session and a fortnightly session on the same (day, slot) for the same batch, **When** conflict detection runs, **Then** a conflict IS reported because their occurring weeks overlap. (edge/boundary)
5. **Given** a fortnightly session, **When** the published timetable is viewed by a student/faculty, **Then** the recurrence pattern is clearly indicated (which weeks it occurs). (Story AC #4)
6. **Given** a calendar feed for a user with fortnightly sessions, **When** exported, **Then** events appear only on the correct alternating weeks. (Story AC #5)
7. **Given** a session with no recurrence set, **When** any consumer reads it, **Then** it behaves as a weekly session (backward compatibility).

## 11. Data Model (Conceptual)

The recurrence pattern is modeled as metadata on the existing session, not a new top-level entity. Conceptually:

| Entity | Key Attributes (added/relevant) | Relationships |
|---|---|---|
| ScheduledSession (extended) | existing: dayOfWeek, slotDefinitionId, roomId, facultyId, batchId, sectionId; **added: recurrenceType (WEEKLY / FORTNIGHTLY), weekGroup (nullable; WEEK_A / WEEK_B when fortnightly)** | belongs to TimetableDraft |
| (reference) AcademicCalendar | semester_start_date (used as parity anchor) | owned by A4-9 |

Conceptual only — column types, indexes, and whether the pattern is an embedded value or a separate table are design decisions (A4-61).

## 12. Dependencies

| Dependency | Blocking? | Description |
|---|---|---|
| A4-11 (Generation Engine) | Yes | Produces the sessions the pattern attaches to |
| A4-9 (Academic Calendar) | Yes | Supplies semester start date (parity anchor) and working-day/holiday data |
| A4-16 (Conflict Detection) | No — consumes | Applies the week-aware non-conflict rule defined here |
| A4-15 (Drag-Drop Editor) | No — consumes | UI to set/edit and display the pattern |
| A4-39 (Calendar Export) | No — consumes | Emits feed events on occurring weeks |
| A4-43 (Audit Trail) | No | Records pattern changes |

## 13. Assumptions

1. Only two recurrence types are needed for this story: weekly and fortnightly/alternate-week. Fortnightly implies exactly two disjoint week groups. (BRD 6.2)
2. A fortnightly session keeps the same (day, time-slot) across its occurring weeks — it does not move between days on alternating weeks. (Consistent with Journey 2; contrasting cases → OQ#3.)
3. The "Group A = Week 1, Group B = Week 2" mapping in the story is illustrative of the alternate-week use case, not a hardcoded system rule — the coordinator chooses which group each session occupies.
4. The campus working-day pattern (A4-9 WorkingDayPattern: five-day, six-day, alternate-Saturday) is a distinct concept from session recurrence and must not be conflated. Working-day pattern decides which *days* are teaching days; session recurrence decides which *weeks* a session runs.
5. Week parity is defined over teaching weeks bounded by the academic calendar; the exact anchor is proposed as semester start but requires confirmation (OQ#1).
6. "Special courses" referenced in the story are supported by the same fortnightly mechanism — no separate course-type feature is introduced by this story.

## 14. Consistency Notes

- **No conflict with A4-11:** A4-11 explicitly defers fortnightly patterns to this story and assumes a canonical weekly pattern (its Assumption 3). This document extends the session model with an optional recurrence field that defaults to weekly, so A4-11's output remains valid unchanged.
- **Division of ownership with A4-16:** This document OWNS the *rule* (disjoint week groups do not conflict; overlapping weeks do — HC-FN-1, HC-FN-2). A4-16 OWNS the *detection mechanism* that applies the rule. No duplication: A4-16 references HC-FN-1/HC-FN-2 rather than re-defining them.
- **Working-day pattern vs. recurrence:** Explicitly separated in Assumption 4 to prevent seam-blindness between A4-9's campus day pattern and this story's per-session week recurrence.
- **Holiday interaction is unresolved:** FR-4.2 and Journey 4 flag that a fortnightly occurrence landing on a holiday needs a defined behavior — captured as OQ#2, not silently decided.

## 15. Out of Scope

- The generation engine placement algorithm — A4-11.
- The conflict detection engine implementation — A4-16 (this doc supplies the week-awareness rule only).
- The editor UI and visual representation of recurrence — A4-15.
- Calendar feed (iCal) generation mechanics — A4-39.
- Recurrence periods other than weekly and fortnightly (every-third-week, monthly, custom N-week cycles) — deferred (OQ#6).
- Fortnightly sessions that change day/slot between week groups (a "biweekly A/B split across different days") — deferred (OQ#3).
- Post-publication editing of recurrence — A4-48.
- Lab batch sub-group splitting logic itself — Story 23; this doc only assigns week groups to already-split sub-group sessions.

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | What anchors "Week 1 / Week-A"? Proposed default: the semester start date from the campus academic calendar (A4-9), with the first teaching week = Week-A. Alternatives: ISO week-number parity, or a coordinator-configured anchor per department. | FR-2.2, determinism, cross-campus consistency | Academic Affairs / System Design |
| 2 | When a fortnightly occurrence lands on a holiday/non-working day/exam window, should the system (a) skip that occurrence with no shift, (b) shift it to the next occurring week, or (c) flag it for the coordinator? | FR-4.2, Journey 4 | Academic Affairs / Registrar |
| 3 | Must both alternate-week groups share the same (day, slot), or can Group A be Monday-Week-A and Group B be Tuesday-Week-B? Assumption 2 currently says same day/slot. | FR-1.2, data model, non-conflict rule scope | Academic Affairs |
| 4 | What is the canonical naming/representation for week groups (Week-A/Week-B vs. odd/even vs. Week-1/Week-2), and how should it be displayed on timetables and feeds? | FR-1.2, FR-5.2 | Academic Affairs / UX |
| 5 | When the engine (A4-11) auto-splits a lab batch into sub-groups sharing a slot, should it automatically assign opposite week groups, or is week-group assignment always a manual coordinator action? | SC-FN-1, FR-1.1, engine scope | Academic Affairs / System Design |
| 6 | Are recurrence periods beyond fortnightly ever required (e.g., every third week, monthly, custom N-week)? BRD 6.2 states only fortnightly/alternate-week. | FR-1.6, scope | Academic Affairs |
| 7 | Does a mid-semester calendar change that alters teaching weeks (e.g., an inserted week-long break) re-map week parity for already-placed fortnightly sessions, and how are affected sessions handled? | FR-2, change impact | Academic Affairs / System Design |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.2 — "Support recurring weekly patterns as well as fortnightly/alternate-week patterns" | FR-1 (recurrence assignment), FR-2 (parity anchoring) |
| Story AC #1 — fortnightly repeats on pattern, not every week | FR-1.1, FR-1.2, FR-1.5, AC #1 |
| Story AC #2 — Group A Week 1 / Group B Week 2, same slot, no conflict | FR-3.1, SC-FN-1, HC-FN-1, AC #2 |
| Story AC #3 — conflict detection only on occurring weeks | FR-3.2, FR-4.1, FR-4.2, HC-FN-2, AC #3, AC #4 |
| Story AC #4 — recurrence clearly indicated on published timetable | FR-5.1, FR-5.2, AC #5 |
| Story AC #5 — calendar feed events only on correct alternating weeks | FR-6.1, FR-6.2, AC #6 |
| A4-11 deferral of fortnightly + canonical weekly assumption | FR-1.3, FR-4.3, Assumption 1, Consistency Notes |
