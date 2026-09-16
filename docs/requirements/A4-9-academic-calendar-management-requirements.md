# Requirement Document — Academic Calendar Management

## 1. Introduction

This document captures the detailed requirements for defining and managing the academic calendar — semester dates, holidays, exam windows, orientation/induction periods, and working-day patterns — with per-campus variations within a unified system. The calendar determines which days are available for scheduling (the scheduling engine excludes holidays and non-working days) and which periods are reserved for exams (no regular classes during exam windows). When the calendar changes after timetable generation, impacted sessions must be detected and flagged.

## 2. User Story

**A4-9:** As a Registrar, I want to define and manage the academic calendar including semester start/end dates, holidays, exam windows, orientation/induction periods, and working-day patterns with per-campus variations, so that the scheduling engine correctly excludes non-working days and respects term boundaries.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| Registrar | Creates, reads, updates, and deletes calendar data. BRD Section 4: "defines academic calendar." Primary owner. |
| System Administrator | May assist with calendar configuration. |
| Campus-level Administrator | [TBD — see Open Question #7: should each campus admin manage their own campus calendar, or is it centralized with the Registrar?] |
| Scheduling Engine (system) | Reads the calendar to: exclude holidays, exclude non-working days, exclude exam windows from regular class placement, determine valid scheduling period (semester start to end). |
| Exam Scheduling (A4-22) | Reads exam window dates to determine when exams can be placed. |
| Conflict Detection (A4-16) | Detects sessions scheduled on holidays or during exam windows as violations (post-calendar-change). |
| Department Coordinator | Reads calendar for planning. Receives flags when their sessions land on newly added holidays. |

## 4. User Journeys

### Journey 1: Registrar Defines a New Semester Calendar

**Before:** Campus exists (A4-2). Academic year and semester are known.

**During:**
1. Registrar selects the campus (or "institution-wide" for global entries).
2. Registrar enters: academic year, semester identifier, semester start date, semester end date.
3. Registrar adds holidays (individual dates or date ranges) with descriptions (e.g., "Diwali Break," "Republic Day").
4. Registrar defines exam window(s) (start date, end date — may be multiple per semester: mid-sem, end-sem).
5. Registrar defines orientation/induction period (date range — blocked from regular scheduling).
6. System validates: start <= end for all ranges, no exam window outside semester bounds, no duplicate calendar for same campus + year + semester [see Open Question #8], no self-contradicting entries.
7. System persists the calendar.

**After:** Scheduling engine can use this calendar. Exam scheduler knows the exam windows. Audit trail records creation.

### Journey 2: Registrar Adds a Campus-Specific Holiday

**Before:** Calendar exists for a campus.

**During:**
1. Registrar selects a specific campus and adds a holiday (e.g., "Regional Festival — 15 Oct" for Campus B only).
2. System persists the holiday linked to Campus B's calendar.

**After:** Only Campus B's scheduling is affected. Other campuses' schedules for that date are unchanged.

### Journey 3: Registrar Adds an Institution-Wide Holiday

**Before:** Calendars exist for multiple campuses.

**During:**
1. Registrar defines a holiday as institution-wide (applies to all campuses).
2. System applies the holiday to all campus calendars.

**After:** All campuses' scheduling excludes that date.

### Journey 4: Registrar Configures Working-Day Pattern

**Before:** Campus exists.

**During:**
1. Registrar configures the working-day pattern for a campus (e.g., "6-day week with alternate Saturdays off — working Saturdays: 1st and 3rd Saturday of each month").
2. System persists the pattern.

**After:** Scheduling engine treats non-working Saturdays (2nd, 4th, 5th) as holidays for that campus. Working Saturdays are available for scheduling.

### Journey 5: Calendar Change After Timetable Generation (Holiday Added)

**Before:** A timetable has been generated (draft or published). Sessions exist on a date that is now being declared a holiday.

**During:**
1. Registrar adds a new holiday (e.g., unexpected institutional closure on 20 Nov).
2. System saves the holiday.
3. System detects that sessions are already scheduled on 20 Nov for the affected campus(es).
4. System flags those sessions as impacted (conflict: session on non-working day).

**After:** Coordinator is informed of impacted sessions and can reschedule them. The system does NOT auto-cancel — it flags only (see Open Question #3 for behavior details).

### Journey 6: Registrar Removes a Holiday

**Before:** A holiday exists. Sessions may have been excluded/rescheduled because of it.

**During:**
1. Registrar removes a holiday (e.g., a previously declared holiday is revoked by the university).
2. System removes the holiday entry.

**After:** The date becomes a working day again. Existing schedules are NOT auto-modified — the freed date is available for future scheduling. Audit trail records the removal.

## 5. Functional Requirements

### FR-1: Calendar Creation and Semester Definition

- FR-1.1: The system shall allow defining an academic calendar with: academic_year, semester_identifier, semester_start_date (required), semester_end_date (required), associated campus (required — specific campus FK, or "institution-wide" for global calendar).
- FR-1.2: The system shall validate that semester_start_date <= semester_end_date.
- FR-1.3: The system shall support multiple semesters per academic year (e.g., Odd semester, Even semester, Summer term).
- FR-1.4: The system shall enforce uniqueness of calendars [TBD: one calendar per campus per semester? See Open Question #8].

### FR-2: Holiday Management

- FR-2.1: The system shall allow adding holidays to a calendar as individual dates or date ranges, each with a description.
- FR-2.2: Holidays may be scoped as: campus-specific (applies to one campus only) or institution-wide (applies to all campuses).
- FR-2.3: Institution-wide holidays shall automatically apply to all campus calendars without manual per-campus entry.
- FR-2.4: The system shall allow updating and removing holidays.
- FR-2.5: On adding a holiday after timetable generation, the system shall flag any sessions already scheduled on that date for the affected campus(es) as impacted.
- FR-2.6: Whether holidays must fall within semester bounds or can be defined outside semester periods (e.g., summer break between semesters) is governed by Open Question #9.

### FR-3: Exam Window Management

- FR-3.1: The system shall allow defining one or more exam windows per semester (e.g., mid-semester exams: 1-5 Oct, end-semester exams: 1-15 Dec), each as a date range.
- FR-3.2: The system shall validate that exam windows fall within the semester's start/end dates.
- FR-3.3: The scheduling engine shall not place regular classes/lectures during exam windows.
- FR-3.4: The exam scheduling module (A4-22) shall use exam windows to determine valid dates for exam placement.

### FR-4: Orientation/Induction Period Management

- FR-4.1: The system shall allow defining orientation/induction periods as date ranges within a semester.
- FR-4.2: The scheduling engine shall not place regular classes during orientation periods [TBD: or should orientation allow special orientation events? See Open Question #2].

### FR-5: Working-Day Pattern Configuration

- FR-5.1: The system shall allow configuring the working-day pattern per campus (e.g., 5-day week: Mon-Fri; 6-day week: Mon-Sat; alternate Saturdays: 1st and 3rd Saturday working).
- FR-5.2: Non-working days per the pattern shall be treated as holidays for scheduling purposes (no sessions placed).
- FR-5.3: The pattern shall be configurable with enough precision to identify exactly which days are working (e.g., "1st and 3rd Saturday" or an explicit list — see Open Question #6).
- FR-5.4: Different campuses may have different working-day patterns within the same institution.

### FR-6: Calendar Reading and Query

- FR-6.1: The system shall allow querying: all holidays for a campus in a date range, all exam windows for a semester, all non-working days for a campus in a date range (combining holidays + pattern-based non-working days).
- FR-6.2: The system shall support a "is this date a working day for campus X?" query — used by the scheduling engine for each slot evaluation.
- FR-6.3: The system shall support listing all calendars by academic year and campus.

### FR-7: Change Impact Detection

- FR-7.1: When a new holiday or non-working day is added (or an existing working day is changed to non-working), and sessions exist on that date for the affected campus, the system shall identify and flag those sessions.
- FR-7.2: The flag shall be surfaced to the department coordinator(s) owning those sessions.
- FR-7.3: The system shall NOT auto-cancel or auto-reschedule flagged sessions — it reports the impact only. Manual resolution is required (consumed by conflict detection and post-publication change management).

### FR-8: Calendar Update and Deletion

- FR-8.1: The system shall allow updating any calendar entry (dates, descriptions, scope).
- FR-8.2: The system shall allow deleting calendar entries (holidays, exam windows, orientation periods).
- FR-8.3: On deletion of a holiday (converting a non-working day back to working), existing schedules are NOT auto-modified — the date simply becomes available.
- FR-8.4: Whether entire past-semester calendars can be deleted or are retained for historical queries is governed by Open Question #4.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-CAL-1 | No regular class session shall be scheduled on a holiday | Hard |
| HC-CAL-2 | No regular class session shall be scheduled on a non-working day (per working-day pattern) | Hard |
| HC-CAL-3 | No regular class session shall be scheduled during an exam window | Hard |
| HC-CAL-4 | No regular class session shall be scheduled during an orientation/induction period [TBD — see Open Question #2] | Hard (pending confirmation) |
| HC-CAL-5 | Semester start date must be <= semester end date | Hard |
| HC-CAL-6 | Exam windows must fall within semester bounds | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Campus must exist in hierarchy | Story 1 (A4-2) |
| Scheduling engine excludes non-working days and exam windows | Story 10 (A4-11) |
| Exam scheduling uses exam window dates | Story 21 (A4-22) |
| Conflict detection flags sessions on holidays | Story 15 (A4-16) |
| Historical archive retains past timetables (may need past calendars for context) | Story 41 (A4-42) |
| Time-slot grid defines which periods exist per day; calendar defines which days exist | Story 9 (A4-10) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| academic_year | Required, format [TBD — e.g., "2026-27" or "2026"] |
| semester_identifier | Required, [TBD — e.g., "Odd", "Even", "Summer" or numeric 1, 2, 3] |
| semester_start_date | Required, valid date |
| semester_end_date | Required, valid date, >= semester_start_date |
| campus | Required — specific campus (FK to A4-2) or "institution-wide" marker |
| holiday date | Required, valid date. [TBD: must be within semester bounds? See Open Question #9] |
| exam_window start/end | Required, valid dates, start <= end, within semester bounds |
| orientation period start/end | Required, valid dates, start <= end, within semester bounds |
| working_day_pattern | Required per campus, valid pattern definition |
| calendar uniqueness | [TBD: unique constraint on (campus_id + academic_year + semester)? See Open Question #8] |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | "Is this date a working day?" query must be fast — it is called per slot during scheduling. Must support the scale implied by BRD Section 8 without being a bottleneck (exact target [TBD]). |
| Audit | Every create, update, and delete of calendar entries shall be recorded (Story 42/A4-43). |
| Security | Only the Registrar role (and possibly System Admin or campus-level admin — see Open Question #7) may mutate calendar data (BRD Section 4: Registrar "defines academic calendar"). All other roles have read-only access. |

## 10. Acceptance Criteria

1. **Given** a registrar role, **When** they define a semester with start/end dates, holidays, exam windows, and orientation periods, **Then** the calendar is persisted.
2. **Given** a campus-specific holiday (e.g., regional festival), **When** added to that campus's calendar, **Then** it only affects scheduling for that campus, not others.
3. **Given** institution-wide holidays, **When** defined, **Then** they apply to all campuses simultaneously.
4. **Given** a working-day pattern (e.g., alternate Saturdays off), **When** configured per campus, **Then** the scheduling engine excludes non-working days for that campus.
5. **Given** a change to the calendar after timetable generation (e.g., adding a new holiday), **When** saved, **Then** the system flags sessions already scheduled on the newly added non-working day.
6. **Given** exam windows in the calendar, **When** the regular class scheduling engine runs, **Then** no lectures are placed during exam windows.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| AcademicCalendar | id, academic_year, semester_identifier, semester_start_date, semester_end_date, campus_id (FK, nullable for institution-wide), created_at, updated_at | belongs to Campus (or institution-wide) |
| CalendarHoliday | id, calendar_id (FK), date (or start_date + end_date for ranges), description, scope (campus_specific / institution_wide) | belongs to AcademicCalendar |
| CalendarExamWindow | id, calendar_id (FK), start_date, end_date, exam_type (mid_sem / end_sem / supplementary) | belongs to AcademicCalendar |
| CalendarOrientationPeriod | id, calendar_id (FK), start_date, end_date, description | belongs to AcademicCalendar |
| WorkingDayPattern | id, campus_id (FK), pattern_type, pattern_definition (e.g., JSON/rule specifying which days are working) | belongs to Campus |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Campus Hierarchy (A4-2) | Campus must exist before calendar can be associated |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Only Registrar/Admin can mutate |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. Academic calendars are semester-based. The system does not support trimester or quarter systems unless modeled as additional semesters (Open Question #5 could affect this).
2. "Alternate Saturdays" is the most complex working-day pattern. More exotic patterns (e.g., "every 3rd Wednesday off") are not expected but could be supported by a flexible pattern definition format.
3. Holidays are date-based (full day off). Half-day holidays are not supported (if needed, model as a time-slot constraint, not a calendar entry).
4. The calendar is authoritative: BRD Section 10 states "Academic calendar and semester start dates are fixed by the university and cannot be adjusted to ease scheduling." The scheduling engine adapts to the calendar, never the reverse.

## 14. Consistency Notes

- HC-CAL-3 (no classes during exam windows) and FR-3.3 are consistent. The exam scheduling module (A4-22) owns what happens DURING exam windows (places exams there). This document owns the rule that regular classes stay OUT.
- FR-7 (change impact detection) flags but does not auto-resolve. The resolution path is: coordinator sees the flag, uses drag-drop editor (A4-15) or post-publication change (A4-48) to reschedule. This document does not own the resolution — only the detection.
- Working-day patterns (FR-5) and holidays (FR-2) overlap in effect (both produce non-working days). The engine query "is this date working?" must check BOTH: pattern says working AND date is not a holiday.

## 15. Out of Scope

- Time-slot grid (which periods exist within a working day) is managed by Story 9 (A4-10).
- Scheduling engine logic for excluding non-working days is managed by Story 10 (A4-11).
- Exam scheduling within exam windows is managed by Story 21 (A4-22).
- Post-publication session rescheduling (when sessions are flagged) is managed by Story 47 (A4-48).
- Bulk import of calendar data is covered by Story 50 (A4-51).
- Calendar feed/export (iCal) is managed by Story 38 (A4-39).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Can a semester have multiple exam windows (e.g., mid-sem in Week 8, end-sem in Week 16)? Or only one per semester? | Affects FR-3.1, data model (one-to-many vs. one-to-one). | Academic Affairs |
| 2 | Is the "orientation/induction period" a full block (no scheduling at all), or can special orientation sessions be scheduled during it? If special sessions are allowed, how are they distinguished from regular classes? | Affects HC-CAL-4, FR-4.2. | Academic Affairs |
| 3 | When a new holiday is added after sessions are already scheduled on that date, should the system: (a) only flag/report the impacted sessions, (b) auto-cancel them and notify, or (c) flag and require manual resolution before the calendar change is committed? | Affects FR-7.1 behavior, FR-2.5 workflow. | Academic Affairs / Registrar |
| 4 | Should past-semester calendars be retained indefinitely (for historical timetable context), or can they be archived/deleted after the data retention period? | Affects FR-8.4, storage, historical reporting. | Registrar / IT |
| 5 | Does the institution use only semesters, or also trimesters/quarters/summer terms? How many "terms" per year must the calendar support? | Affects FR-1.3, semester_identifier values. | Academic Affairs |
| 6 | How is "alternate Saturdays" precisely defined? "1st and 3rd Saturday of each month are working" or "odd-numbered Saturdays from semester start" or an explicit manually-curated list? | Affects FR-5.3, pattern definition format, implementability. | Academic Affairs / Registrar |
| 7 | Who manages campus-specific calendar entries? Registrar centrally for all campuses, or can each campus have a designated admin who manages their own calendar entries (holidays, working pattern)? BRD Section 4 says Registrar "defines academic calendar" but with multiple campuses, delegation may be needed. | Affects Actors, RBAC permissions, Security NFR. | Registrar / Academic Affairs |
| 8 | Should the system enforce uniqueness of calendars — at most one calendar per (campus + academic_year + semester)? Or can multiple calendar objects exist for the same scope? If unique: what happens if a Registrar tries to create a second calendar for the same campus/semester? | Affects FR-1.4, validation rules, data model. | System Design / Registrar |
| 9 | Can holidays be defined outside semester date bounds (e.g., inter-semester breaks, summer holidays)? If the system only supports holidays within semester bounds, how are breaks between semesters represented? | Affects FR-2.6, validation rule for holiday dates. | Academic Affairs |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.1 — "Academic calendar: semester start/end dates, holidays, exam windows, orientation/induction periods, per-campus calendar variation" | FR-1 (semester), FR-2 (holidays), FR-3 (exam windows), FR-4 (orientation), FR-5 (per-campus via working pattern + campus-scoped entries) |
| 7.4 — "Academic calendar: Semester dates, holidays, exam windows, induction/orientation blocks" | Same as above — duplicate BRD reference to same concepts |
| 7.4 — "Working days pattern: 5-day/6-day week, alternate Saturdays, campus-specific variations" | FR-5 (working-day pattern per campus) |
| 6.12 — "Support campus-specific calendars, time-slot grids, and holiday lists within one unified system" | FR-2.2 (campus-specific holidays), FR-5.4 (different patterns per campus), FR-1.1 (campus association) |
| Section 10 — "Academic calendar and semester start dates are fixed by the university and cannot be adjusted to ease scheduling" | Assumption #4, HC-CAL constraints (engine adapts to calendar, not reverse) |
| BRD Section 4 — Registrar: "defines academic calendar" | Actor table, Security NFR |
