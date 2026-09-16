# Requirement Document — Time-Slot Grid Configuration

## 1. Introduction

This document captures the detailed requirements for configuring time-slot grids per campus — defining the available periods within each working day, their durations, and break/lunch windows. The grid is the temporal scaffolding into which the scheduling engine places sessions. BRD 7.7 requires a single grid to accommodate mixed durations (60-minute lectures, 90-minute tutorials, and 180-minute practicals) simultaneously without overlap.

## 2. User Story

**A4-10:** As a System Administrator, I want to configure time-slot grids per campus with mixed slot durations (60, 90, 180 minutes), break/lunch windows, and period definitions, so that each campus has its own daily schedule structure accommodating lectures, tutorials, and practicals simultaneously.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| System Administrator (IT) | Creates, reads, updates, and deletes time-slot grids. BRD Section 4: master data management. |
| Registrar | May review/approve grid configurations. |
| Scheduling Engine (system) | Reads the grid to determine valid time positions for session placement. A session can only be placed in a slot that matches its required duration. |
| Drag-Drop Editor (A4-15) | Displays the grid as the visual row/column structure coordinators interact with. |
| Lab Scheduling (A4-24) | Requires 180-min slots to exist in the grid for 3-hour lab sessions. |
| Faculty Availability (A4-5) | Availability windows reference times that should align with grid boundaries (Open Question in A4-5). |

## 4. User Journeys

### Journey 1: Administrator Creates a Time-Slot Grid for a Campus

**Before:** Campus exists (A4-2).

**During:**
1. Admin selects a campus.
2. Admin defines the grid: a set of time-slot definitions, each with start_time, end_time, duration, and slot_type (teaching/break/lunch).
3. Admin ensures mixed durations coexist: e.g., Period 1: 8:00-9:00 (60 min lecture), Period 2: 9:00-10:00 (60 min), Period 3: 10:00-10:15 (break), Period 4: 10:15-11:45 (90 min tutorial), Period 5: 11:45-12:30 (lunch), Period 6: 12:30-15:30 (180 min lab).
4. System validates: no overlapping slots, start < end for each slot, all slots within valid daily bounds [TBD], mixed durations permitted.
5. System persists the grid.

**After:** Scheduling engine uses this grid for the campus. The drag-drop editor renders this grid visually. Audit trail records creation.

### Journey 2: Administrator Modifies an Existing Grid (Adds an Evening Slot)

**Before:** Grid exists for a campus. Timetables may have been generated using it.

**During:**
1. Admin adds a new slot (e.g., 16:00-17:00 evening lecture period).
2. System validates: new slot does not overlap with existing slots.
3. System persists the update.

**After:** New slot is available for future scheduling runs. Existing sessions are unaffected (they stay in their current slots). Audit trail records the change.

### Journey 3: Administrator Removes a Slot from a Grid

**Before:** Grid exists. Sessions may have been placed in the slot being removed.

**During:**
1. Admin selects a slot and removes it.
2. System checks: are there active sessions placed in this slot for this campus?
3. If yes: flags impacted sessions (they reference a slot that no longer exists). [TBD: block removal? Warn? See Open Question #3]
4. If no: slot removed cleanly.

**After:** Slot no longer available for future scheduling. Impacted sessions (if any) need rescheduling.

### Journey 4: Overlap Validation on Grid Configuration

**Before:** Admin is configuring or updating a grid.

**During:**
1. Admin attempts to add a slot 9:30-11:00 when a slot 9:00-10:00 already exists.
2. System detects the overlap (9:30-10:00 is shared).
3. System rejects the addition with a validation error explaining the overlap.

**After:** Grid remains valid. Admin must adjust times to avoid overlap.

### Journey 5: Different Grids for Different Campuses

**Before:** Two campuses exist.

**During:**
1. Admin configures Campus A grid: starts at 8:00, 8 periods, 60 min each, lunch at 12:00.
2. Admin configures Campus B grid: starts at 9:00, 6 periods with mixed durations, lunch at 13:00.
3. System persists both independently.

**After:** Each campus has its own grid. The scheduling engine uses the correct grid per campus. Cross-campus faculty scheduling (Story 34/A4-35) must account for different grid structures when computing travel-time feasibility.

## 5. Functional Requirements

### FR-1: Grid Creation

- FR-1.1: The system shall allow creating a time-slot grid for a campus with:
  - campus_id (required, must reference existing campus from A4-2)
  - grid_name/label (required — e.g., "Campus A Weekday Grid")
  - A set of slot definitions (at least one teaching slot required)
- FR-1.2: Each slot definition shall include: start_time (required), end_time (required), slot_type (required: teaching / break / lunch), duration_minutes (derived: end_time - start_time).
- FR-1.3: The system shall support mixed durations within a single grid: 60 min, 90 min, and 180 min slots coexisting (BRD 7.7: "A single time-slot grid must accommodate 1-hour lecture, 1.5-hour, and 3-hour practical slot lengths simultaneously").
- FR-1.4: The system shall reject a grid if any two slot definitions overlap in time.
- FR-1.5: The system shall reject a slot if start_time >= end_time.
- FR-1.6: Whether a campus can have multiple active grids (e.g., weekday vs Saturday, or semester-specific) is governed by Open Question #1.

### FR-2: Grid Reading and Query

- FR-2.1: The system shall allow reading the grid for a specific campus.
- FR-2.2: The system shall allow querying: which slots are available on a given day for a campus (considering that different days might have different structures — see Open Question #5).
- FR-2.3: The system shall return slot data including start_time, end_time, duration, and type.

### FR-3: Grid Update

- FR-3.1: The system shall allow adding new slots to an existing grid (subject to overlap validation).
- FR-3.2: The system shall allow removing slots from a grid [TBD: with impact check on existing sessions? See Open Question #3].
- FR-3.3: The system shall allow modifying a slot's times (subject to overlap validation against other slots).
- FR-3.4: On any modification, overlap validation must re-run against the full set of remaining slots.

### FR-4: Grid Deletion

- FR-4.1: The system shall allow deleting an entire grid only if no active timetable (draft or published) references it.
- FR-4.2: Whether old grids are hard-deleted or archived is governed by Open Question #4.

### FR-5: Non-Overlap Validation

- FR-5.1: The system shall validate that no two teaching slots in the same grid (for the same day, if day-specific) overlap in time.
- FR-5.2: Break/lunch slots shall not overlap with teaching slots.
- FR-5.3: Overlap is defined as: Slot A overlaps Slot B if A.start_time < B.end_time AND B.start_time < A.end_time.
- FR-5.4: Validation shall run on every create and update of slot definitions.

### FR-6: Break and Lunch Window Definition

- FR-6.1: The system shall allow defining break/lunch windows as explicit slot entries with slot_type = "break" or "lunch."
- FR-6.2: The scheduling engine shall not place sessions during break/lunch slots.
- FR-6.3: Break/lunch slots participate in overlap validation (no teaching slot can overlap a break).

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-GRID-1 | No two slots in the same grid (same day) may overlap in time | Hard |
| HC-GRID-2 | No session shall be placed during a break or lunch slot | Hard |
| HC-GRID-3 | Slot start_time must be < end_time | Hard |
| HC-GRID-4 | A grid must have at least one teaching slot | Hard |
| HC-GRID-5 | Every grid must reference an existing campus | Hard |
| HC-GRID-6 | A single grid must accommodate mixed durations (60, 90, 180 min) simultaneously (BRD 7.7) | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Campus must exist in hierarchy | Story 1 (A4-2) |
| Scheduling engine places sessions only in teaching slots matching session duration | Story 10 (A4-11) |
| Lab scheduling requires 180-min slots for 3-hour practicals | Story 23 (A4-24) |
| Academic calendar defines which DAYS are working; grid defines which PERIODS within those days | Story 8 (A4-9) |
| Travel-time buffer calculation references grid times for cross-campus feasibility | Story 34 (A4-35) |
| Drag-drop editor renders grid as visual structure | Story 14 (A4-15) |
| Historical archive may need old grid definitions for past timetable context | Story 41 (A4-42) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| campus_id | Required, must reference existing campus (A4-2) |
| grid_name | Required, non-empty, max length [TBD] |
| slot start_time | Required, valid time (HH:MM format) |
| slot end_time | Required, valid time, > start_time |
| slot_type | Required, one of: teaching, break, lunch |
| slot duration | Derived (end_time - start_time). Must be one of the supported durations for teaching slots: 60, 90, or 180 minutes [TBD: or any duration? See Open Question #6]. |
| grid slot set | Must contain at least one teaching slot. No overlaps between any two slots (same day). |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Grid queries are used frequently during scheduling (once per slot evaluation). Must be fast. Exact target [TBD]. Grid data is small and can be cached. |
| Audit | Every create, update, and delete of grid/slot definitions shall be recorded (Story 42/A4-43). |
| Security | Only System Administrator may mutate grid data (BRD Section 4). All other roles have read-only access. |

## 10. Acceptance Criteria

1. **Given** an admin role, **When** they define a time-slot grid for a campus with mixed durations (60 min lectures, 90 min tutorials, 180 min labs), **Then** the grid is persisted and all durations coexist without overlap.
2. **Given** a time-slot grid, **When** two slot definitions would overlap in time, **Then** the system rejects the configuration with a validation error.
3. **Given** break/lunch windows defined in the grid, **When** the scheduling engine runs, **Then** no sessions are placed during those windows.
4. **Given** two campuses with different grids (e.g., Campus A starts at 8:00, Campus B at 9:00), **When** scheduling runs, **Then** each campus uses its own grid independently.
5. **Given** a 180-minute lab slot defined in the grid, **When** a 3-hour lab session is scheduled, **Then** it occupies exactly one 180-minute slot cleanly.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| TimeSlotGrid | id, campus_id (FK), grid_name, is_active, applicable_days [TBD — see Open Question #5], created_at, updated_at | belongs to Campus; has many SlotDefinitions |
| SlotDefinition | id, grid_id (FK), day_of_week (or "all" — see Open Question #5), start_time, end_time, slot_type (teaching/break/lunch) | belongs to TimeSlotGrid |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Campus Hierarchy (A4-2) | Campus must exist before grid can be created |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Only Admin can mutate |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. A grid defines the available time containers for a day. Which sessions go into which containers is the scheduling engine's job (Story 10/A4-11), not this document's.
2. BRD 7.7 names three specific durations (60, 90, 180 min). These are the minimum required. If the institution also has 45-minute or 120-minute periods, the grid should support them — the system should not hard-code only these three. See Open Question #6.
3. Breaks and lunches are explicit entries in the grid — they are not merely "gaps between teaching slots." This makes them visible in the UI and enforces that no session is placed there.
4. The grid is the same for all working days of the week unless day-specific variation is supported (Open Question #5).

## 14. Consistency Notes

- HC-GRID-6 requires mixed durations "simultaneously" — this means a single grid contains slots of different lengths. It does NOT mean all durations must be used every day. A day might use only 60-min slots; the grid just needs to ALLOW 90/180 min slots to be defined.
- FR-5.3 gives a precise mathematical definition of overlap. This prevents ambiguity in validation implementation.
- The relationship between "slot duration" and "session duration" (from course L-T-P): a 3-1-2 course generates three types of sessions (3x60min lectures, 1x60min tutorials, 2x90min or 1x180min practicals). The grid must have slots matching these durations for the engine to place them.

## 15. Out of Scope

- Scheduling engine logic for placing sessions into slots (Story 10/A4-11).
- Academic calendar (which days to schedule on) is managed by Story 8 (A4-9).
- Lab session block scheduling (contiguous 180-min sessions) is managed by Story 23 (A4-24).
- Drag-drop editor rendering of the grid is managed by Story 14 (A4-15).
- Bulk configuration of grids is covered by Story 50 (A4-51) if applicable.

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Can a campus have multiple active grids simultaneously? E.g., a "Weekday Grid" and a "Saturday Grid" with different structures? Or does one campus always have exactly one active grid? | Affects FR-1.6, data model (is_active flag or separate grids per day-type), scheduling engine grid selection logic. | Academic Affairs / System Design |
| 2 | Can the grid change mid-semester (e.g., after mid-sem exams, the afternoon schedule shifts)? Or is it fixed for the entire semester? | Affects FR-3 (when can updates happen), impact on existing sessions. | Academic Affairs |
| 3 | When a slot is removed from a grid and existing sessions occupy that slot, should the system: (a) block removal until sessions are rescheduled, (b) allow removal and flag impacted sessions, or (c) force reschedule before allowing? | Affects FR-3.2, user workflow. | Academic Affairs / System Design |
| 4 | Should old/replaced grid configurations be archived (for historical timetable context) or hard-deleted? Past timetables were built on a specific grid — understanding them requires knowing what the grid looked like at that time. | Affects FR-4.2, data model, historical reporting. | IT / Registrar |
| 5 | Can different days of the week have different slot structures within the same campus? E.g., Monday has 8 periods but Friday has only 6 (half-day)? If yes, the grid must be day-specific rather than universal. | Affects data model (SlotDefinition needs day_of_week), FR-2.2, grid complexity. | Academic Affairs |
| 6 | Are teaching slot durations restricted to exactly 60, 90, and 180 minutes (BRD 7.7 names these three), or can any duration be defined (e.g., 45 min, 120 min)? BRD 7.7 says the grid "must accommodate" those three — it doesn't say "only" those three. | Affects validation rules (restrict to enum or allow any positive duration). | Academic Affairs / System Design |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.2 — "Support configurable time-slot grids per campus" | FR-1 (grid creation per campus), FR-3 (grid update) |
| 7.4 — "Time-slot grid: Period duration, number of periods/day, break/lunch windows, configurable per campus" | FR-1.2 (slot definitions with duration), FR-6 (break/lunch), FR-1.1 (per campus) |
| 7.7 — "Mixed slot durations: A single time-slot grid must accommodate 1-hour lecture, 1.5-hour, and 3-hour practical slot lengths simultaneously" | FR-1.3 (mixed durations), HC-GRID-6 |
| 6.12 — "Support campus-specific calendars, time-slot grids, and holiday lists within one unified system" | FR-1.1 (campus_id association), Journey 5 (different grids per campus) |
