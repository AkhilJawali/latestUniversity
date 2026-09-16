# Requirement Document — Faculty Availability and Preference Management

## 1. Introduction

This document captures the detailed requirements for faculty to declare their availability windows (hard unavailability and soft preferences) so that the scheduling engine respects these constraints when assigning sessions. Hard unavailability (e.g., administrative duties, research time) is inviolable. Soft preferences (e.g., preferred morning slots, prefer spread sessions) are optimization targets the engine tries to satisfy but may relax with justification.

## 2. User Story

**A4-5:** As a Faculty member, I want to declare my availability windows (hard unavailability and soft time preferences), so that the scheduling engine respects my constraints and preferences when assigning sessions.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| Faculty member | Declares their own hard unavailability windows and soft time preferences. BRD 6.5 implies faculty self-declaration ("Capture faculty time-preferences/unavailability"). |
| Visiting/Adjunct Faculty | Same as Faculty — declares availability. BRD Section 4: "declare availability." This is one of their two permitted actions. |
| System Administrator | May create/edit availability on behalf of faculty (e.g., during initial data setup). See Open Question #1. |
| Scheduling Engine (system) | Reads hard windows as inviolable constraints. Reads soft preferences as optimization targets. |
| Conflict Detection (Story 15/A4-16) | Detects if a session is placed in a faculty member's hard unavailability window. |
| Substitution Service (Story 29/A4-30) | Checks proposed substitute's availability — must not have hard unavailability in the affected slot. |

## 4. User Journeys

### Journey 1: Faculty Declares Hard Unavailability

**Before:** Faculty profile exists (A4-4). Faculty is authenticated.

**During:**
1. Faculty navigates to their availability management.
2. Faculty adds a hard unavailability window: selects day(s) of week, start time, end time, and provides a reason (e.g., "Administrative duties," "Research time," "External commitment").
3. System validates: times are within the campus time-slot grid boundaries (if applicable — see Open Question #2), start < end, faculty profile exists.
4. System persists the window as a hard constraint.

**After:** The scheduling engine will never place a session for this faculty in this window. If existing draft sessions occupy this window, they are flagged as conflicts. Audit trail records the declaration.

### Journey 2: Faculty Sets Soft Time Preference

**Before:** Faculty profile exists.

**During:**
1. Faculty navigates to preferences.
2. Faculty sets: preferred time-of-day (e.g., "mornings" = before 12 PM), session spread preference (e.g., "prefer spread sessions over consecutive").
3. System persists as soft constraints.

**After:** The scheduling engine will try to satisfy these but may relax them with justification recorded (Story 10, AC 4). No hard enforcement.

### Journey 3: Faculty Updates an Existing Hard Unavailability Window

**Before:** A hard window exists. Sessions may have been scheduled respecting this window.

**During:**
1. Faculty selects the window and modifies times (e.g., shifts from Tuesday 2-4 PM to Tuesday 3-5 PM).
2. System validates new times.
3. System persists the update.

**After:** Subsequent scheduling runs use the updated window. If the change frees a slot that was previously blocked, existing schedules are NOT auto-rescheduled — the freed slot becomes available for future runs. If the change blocks a new slot where sessions already exist, those sessions are flagged as conflicts. Audit trail records previous and new values.

### Journey 4: Faculty Removes a Hard Unavailability Window

**Before:** A hard window exists.

**During:**
1. Faculty selects the window and removes it.
2. System deletes the window.

**After:** The slot is now available for scheduling. Existing schedules are not auto-modified. Audit trail records deletion.

### Journey 5: Part-Time/Visiting Faculty Declares Limited Availability

**Before:** Faculty profile exists with designation Visiting or Adjunct.

**During:**
1. Visiting faculty declares their available windows (e.g., "available only Monday and Wednesday 9 AM–1 PM").
2. System stores: all other times as implicitly hard-unavailable, declared windows as available.

**After:** Scheduling engine only considers this faculty for sessions within their declared available windows. All other times are treated as hard-blocked.

## 5. Functional Requirements

### FR-1: Hard Unavailability Window CRUD

- FR-1.1: The system shall allow a faculty member to create a hard unavailability window with: day_of_week (required), start_time (required), end_time (required), reason (required, from a list or free-text — [TBD: see Open Question #3]).
- FR-1.2: The system shall validate that start_time < end_time.
- FR-1.3: The system shall allow reading all hard unavailability windows for a given faculty member.
- FR-1.4: The system shall allow updating a hard window's day, times, and reason.
- FR-1.5: The system shall allow deleting a hard window.
- FR-1.6: On creation or update, if the new/modified window overlaps a time where the faculty has an active session in a draft or published timetable, the system shall flag those sessions as conflicts (consumed by Story 15/A4-16).
- FR-1.7: The system shall support recurring windows (e.g., "every Tuesday 2-4 PM") as the default pattern for weekly scheduling. One-off exceptions for specific dates are [TBD — see Open Question #4].

### FR-2: Soft Time Preference Management

- FR-2.1: The system shall allow a faculty member to set soft preferences including:
  - Preferred time-of-day (e.g., morning / afternoon / no preference)
  - Session distribution preference (e.g., prefer consecutive sessions / prefer spread sessions / no preference)
- FR-2.2: The system shall store soft preferences as optimization hints, not hard constraints.
- FR-2.3: The system shall allow updating preferences at any time.
- FR-2.4: The scheduling engine (Story 10) shall read these preferences and attempt to satisfy them, recording justification when relaxed (Story 10, AC 4).

### FR-3: Part-Time / Limited Availability Model

- FR-3.1: The system shall support a "declare available windows" model for part-time/visiting faculty, where only declared windows are available and all other times are implicitly hard-blocked.
- FR-3.2: This model shall coexist with the standard "declare blocked windows" model — the system determines which model applies based on [TBD: designation-based? explicit toggle? See Open Question #5].
- FR-3.3: The scheduling engine shall only place sessions for limited-availability faculty within their declared available windows.

### FR-4: Conflict Surfacing on Availability Change

- FR-4.1: When a new hard window is created or an existing one is expanded (covers more time), and sessions for this faculty exist in the newly blocked period, the system shall flag those sessions as conflicts.
- FR-4.2: When a hard window is removed or shrunk (covers less time), the system shall NOT auto-reschedule — it only makes the freed time available for future scheduling runs.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-AVL-1 | No session shall be placed in a faculty member's hard unavailability window | Hard |
| HC-AVL-2 | Part-time/visiting faculty sessions must fall within declared available windows only | Hard |
| HC-AVL-3 | start_time must be < end_time for any window | Hard |
| SC-AVL-1 | Preferred time-of-day should be respected where possible | Soft |
| SC-AVL-2 | Session distribution preference (consecutive vs. spread) should be respected where possible | Soft |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Faculty profile must exist | Story 3 (A4-4) |
| Scheduling engine reads hard windows as inviolable | Story 10 (A4-11) |
| Scheduling engine reads soft preferences as optimization targets | Story 10 (A4-11), AC 4, AC 7, AC 8 |
| Conflict detection flags sessions in hard-blocked windows | Story 15 (A4-16), AC 7 |
| Substitution checks availability of proposed substitute | Story 29 (A4-30), AC 2 |
| Exam invigilation checks availability before duty assignment | Story 22 (A4-23), AC 3 |
| Time-slot grid defines campus time boundaries | Story 9 (A4-10) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| faculty_id | Required, must reference existing faculty profile (A4-4) |
| day_of_week | Required, valid day (Monday–Saturday, or Sunday if applicable) |
| start_time | Required, valid time within campus operating hours [TBD — see Open Question #2] |
| end_time | Required, must be > start_time |
| constraint_type | Required, one of: hard, soft |
| reason | Required for hard unavailability, format [TBD — see Open Question #3] |
| preference_type | Required for soft preferences, one of defined types |
| preference_value | Required for soft preferences, valid value for the type |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Availability queries for a single faculty shall return within [TBD] — used in real-time during conflict checks (< 2 seconds per BRD Section 8: "conflict checks return in under 2 seconds"). |
| Audit | Every create, update, and delete of availability windows shall be recorded (Story 42 / A4-43). |
| Security | Faculty can manage their OWN availability only. Admin can manage on behalf. No faculty can modify another's availability. |

## 10. Acceptance Criteria

1. **Given** a faculty member, **When** they declare a hard unavailability window (e.g., Tuesday 2–4 PM for administrative duties), **Then** it is stored as a hard constraint that the scheduling engine must never violate.
2. **Given** a faculty member, **When** they set a soft time preference (e.g., prefers morning slots, prefers spread sessions over consecutive), **Then** it is stored as a soft constraint that the engine will try to satisfy.
3. **Given** a hard unavailability window, **When** the scheduling engine runs, **Then** no session is placed in that window for that faculty member.
4. **Given** a part-time/visiting faculty member, **When** their limited availability is declared, **Then** only their declared available windows are considered for scheduling.
5. **Given** a faculty member updates their availability, **When** the change is saved, **Then** it is reflected in subsequent scheduling runs and any existing conflicts are flagged.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| FacultyAvailabilityWindow | id, faculty_id (FK), day_of_week, start_time, end_time, constraint_type (hard/soft), reason, created_at, updated_at | belongs to Faculty |
| FacultyPreference | id, faculty_id (FK), preference_type, preference_value, created_at, updated_at | belongs to Faculty |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Faculty Profile (A4-4) | Faculty must exist before availability can be declared |
| Time-Slot Grid (A4-10) | Campus operating hours define valid time boundaries |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Faculty can only manage own; admin can manage on behalf |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. Availability windows are defined on a weekly recurring basis (same day/time every week). Date-specific exceptions (e.g., "available this one Tuesday despite normally blocked") are out of scope unless Open Question #4 adds them.
2. Soft preferences are simple categorical choices (morning/afternoon, consecutive/spread), not numeric weights. BRD 7.2 says "Preference weighting" — if numeric weighting is needed, this assumption must be revised (see Open Question #6).
3. Removing a hard block does NOT trigger automatic rescheduling into the freed slot. It only makes the slot available for future generation runs.
4. The part-time/visiting "available only" model is the inverse of the regular "blocked during" model but achieves the same result: non-available time = hard constraint.

## 14. Consistency Notes

- BRD 7.2 says "Preference weighting" — this could mean numeric weights (e.g., 1-10 importance scale) or categorical preferences. The document implements categorical for now (morning/afternoon, consecutive/spread) and flags numeric weighting as Open Question #6.
- HC-AVL-1 (no session in hard window) is consumed by both the scheduling engine (avoids placing) and conflict detection (flags if placed). Both consumers are cross-referenced in Section 7.
- The distinction between "declare blocked windows" (regular faculty) and "declare available windows" (visiting/part-time) produces the same scheduling behavior (engine only uses available time) but via different UI flows. FR-3 handles this.

## 15. Out of Scope

- Faculty leave and substitution (declaring leave for specific dates, proposing substitutes) is managed by Story 29 (A4-30).
- Faculty workload computation is managed by Story 30 (A4-31).
- The scheduling engine's optimization algorithm for satisfying soft preferences is managed by Story 10 (A4-11).
- Faculty profile creation/management is in Story 3 (A4-4).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Can System Administrators create/edit availability on behalf of a faculty member (e.g., during bulk data setup), or is it strictly self-service by the faculty? | Affects Actors, RBAC permissions. | Academic Affairs / IT |
| 2 | Must availability window times align with the campus time-slot grid boundaries, or can they be arbitrary times (e.g., 2:15 PM–3:45 PM even if no slot boundary exists there)? | Affects FR-1.2 validation, interaction with Story 9 (A4-10). | System Design |
| 3 | Should "reason" for hard unavailability be free-text or selected from a predefined list (e.g., Research, Administrative, External Commitment, Personal)? A predefined list enables reporting; free-text is more flexible. | Affects FR-1.1, validation rules, reporting capability. | Academic Affairs |
| 4 | Are date-specific exceptions supported (e.g., "I'm normally unavailable Tuesday 2-4, but THIS Tuesday I'm available")? Or only recurring weekly patterns? | Affects FR-1.7, data model (needs date field for exceptions). | Academic Affairs |
| 5 | For part-time/visiting faculty, is the "available only during declared windows" model triggered automatically by designation (Visiting/Adjunct), or does the user explicitly choose between "declare blocked" vs "declare available" models? | Affects FR-3.2, RBAC logic, user experience. | Academic Affairs / System Design |
| 6 | Does "Preference weighting" (BRD 7.2) mean numeric weights (importance 1-10) that the engine uses for trade-off decisions, or simply categorical preferences (morning/afternoon)? If numeric, what scale? How does the engine compare weights across faculty? | Affects FR-2.1, data model (needs weight field), engine optimization logic (Story 10). | Academic Affairs / System Design |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.5 — "Capture faculty time-preferences/unavailability as soft or hard constraints" | FR-1 (hard windows), FR-2 (soft preferences), HC-AVL-1, SC-AVL-1/2 |
| 7.2 — "Availability / blocked slots: Research time, administrative duties, part-time/visiting constraints" | FR-1 (hard windows with reason), FR-3 (part-time model) |
| 7.2 — "Preference weighting: Soft preferences such as preferred time-of-day, consecutive vs. spread sessions" | FR-2 (preference management), SC-AVL-1, SC-AVL-2 |
| Section 4 — Visiting/Adjunct Faculty: "declare availability" | FR-3 (limited availability model), Journey 5 |
