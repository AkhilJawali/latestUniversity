# Requirement Document — Faculty Profile Management

## 1. Introduction

This document captures the detailed requirements for managing faculty profiles as master data — including qualifications, designation/cadre, home department, multi-campus associations, subject competencies, and workload configuration. Faculty profile data is consumed by the scheduling engine (faculty assignment), conflict detection (double-booking, workload limits), substitution proposals (competency matching), exam invigilation (availability), and compliance reporting (workload norms per cadre).

## 2. User Story

**A4-4:** As an HOD, I want to manage faculty profiles including designation, home department, qualifications, subject competency list, and multi-campus associations, so that faculty can be correctly assigned to courses they are qualified to teach across campuses.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| System Administrator (IT) | Creates, reads, updates, and deletes faculty profiles. BRD Section 4 assigns master data management to this role. |
| HOD | BRD Section 4: "Allocates faculty to courses; manages department workload norms." This implies HOD has read access and may update competency/assignment data — see Open Question #1. |
| Faculty member (self) | Reads their own profile. Does NOT self-edit qualification/designation/competency — those are admin-managed. (Availability is managed in A4-5, not here.) |
| Visiting/Adjunct Faculty | Same as Faculty but with limited system access (BRD Section 4). Profile is created by Admin with appropriate designation. |
| Scheduling Engine (system) | Reads competency list to verify valid faculty-course assignments. Reads campus associations for multi-campus scheduling. |
| Workload Computation (A4-31) | Reads min/max weekly load and designation for compliance checks. |
| Substitution Service (A4-30) | Reads competency list to propose eligible substitutes. |

## 4. User Journeys

### Journey 1: Administrator Creates a Faculty Profile

**Before:** The faculty member's home department exists (A4-2). Courses exist for competency linking (A4-3). At least one campus exists.

**During:**
1. Admin enters: faculty name, identifier [format TBD — see Open Question #2], designation (Professor / Associate Professor / Assistant Professor / Visiting / Adjunct), qualification, home department reference, min/max weekly teaching load [or inherit from cadre — see Open Question #5].
2. Admin adds campus associations (one or more campuses the faculty is associated with).
3. Admin adds subject competencies (references to courses the faculty is qualified to teach).
4. System validates: home department exists, all referenced campuses exist, all referenced courses exist, min ≤ max for workload values.
5. System persists the profile.

**After:** Faculty is available for course assignment, scheduling, and substitution proposals. Audit trail records creation.

### Journey 2: Administrator Updates Faculty Competency List

**Before:** Faculty profile exists. Courses exist.

**During:**
1. Admin adds a new course to the faculty's competency list.
2. System validates: course exists.
3. System persists the new competency link.

**After:** Faculty can now be assigned to sessions of that course. Substitution service will consider them for that subject.

### Journey 3: Administrator Adds a Campus Association

**Before:** Faculty profile exists. Target campus exists.

**During:**
1. Admin adds a campus association.
2. System validates: campus exists, association doesn't already exist (no duplicates).
3. System persists the association.

**After:** Scheduling engine considers this faculty for sessions on the newly associated campus. Travel-time buffer rules (Story 34/A4-35) now apply between this campus and others.

### Journey 4: HOD Views Department Faculty with Workload Configuration

**Before:** Faculty profiles exist in the HOD's department.

**During:**
1. HOD queries faculty in their department.
2. System returns profiles with designation, competencies, campus associations, and configured min/max load.

**After:** HOD can review workload capacity for planning.

### Journey 5: Administrator Attempts to Delete a Faculty Profile with Active References

**Before:** Faculty has assigned sessions in an active/published timetable, or historical timetables reference them.

**During:**
1. Admin selects faculty and chooses "Delete."
2. System checks: active sessions, invigilation duties, historical timetable references, active substitution assignments.
3. References found → deletion rejected with error listing referencing entities.

**After:** Profile unchanged. Admin must deactivate (per Open Question #3) or remove all references first.

### Journey 6: Administrator Creates a Visiting/Adjunct Faculty Profile

**Before:** Same as Journey 1.

**During:**
1. Admin enters profile with designation = "Visiting" or "Adjunct."
2. System validates same as Journey 1.
3. System persists with limited-access designation noted.

**After:** Visiting/Adjunct faculty gets limited system access (declare availability via A4-5, view own sessions only — per BRD Section 4). RBAC (A4-44) enforces the access limitation based on designation.

## 5. Functional Requirements

### FR-1: Faculty Profile Creation

- FR-1.1: The system shall allow creation of a faculty profile with:
  - name (required)
  - identifier [format and uniqueness TBD — see Open Question #2]
  - designation (required, one of: Professor, Associate Professor, Assistant Professor, Visiting, Adjunct — [TBD: confirm full list with stakeholder])
  - qualification (required, free-text or structured — [TBD])
  - home_department reference (required, must exist in hierarchy A4-2)
  - min_weekly_load (see Open Question #5 — per-faculty or inherited from cadre)
  - max_weekly_load (see Open Question #5)
  - campus_associations (required, at least one, all must reference existing campuses)
  - subject_competencies (optional at creation, array of references to existing courses)
- FR-1.2: The system shall reject creation if the home department does not exist.
- FR-1.3: The system shall reject creation if any referenced campus does not exist.
- FR-1.4: The system shall reject creation if any referenced competency course does not exist.
- FR-1.5: The system shall reject creation if min_weekly_load > max_weekly_load (when explicitly set).

### FR-2: Faculty Profile Reading and Search

- FR-2.1: The system shall allow reading a single faculty profile by ID or identifier.
- FR-2.2: The system shall allow listing faculty filtered by: home department, campus association, designation, subject competency (course), or any combination.
- FR-2.3: The system shall return profile data including all campus associations, all competencies, and workload configuration.
- FR-2.4: The system shall support a "who can teach this course?" query — returning all faculty with that course in their competency list.

### FR-3: Faculty Profile Update

- FR-3.1: The system shall allow updating: name, designation, qualification, min/max weekly load, campus associations (add/remove), subject competencies (add/remove).
- FR-3.2: Whether home department can change (faculty transfer) is governed by Open Question #4.
- FR-3.3: On competency addition, the system shall validate the referenced course exists.
- FR-3.4: On campus association addition, the system shall validate the referenced campus exists and the association doesn't already exist.
- FR-3.5: On campus association removal, the system shall [TBD: check if active sessions on that campus reference this faculty? Block removal if so? See Open Question #6].
- FR-3.6: On designation change, the system shall [TBD: recalculate workload norms if min/max is inherited from cadre? See Open Question #5].

### FR-4: Faculty Profile Deletion

- FR-4.1: The system shall allow deletion of a faculty profile only if no active sessions, invigilation duties, historical/archived timetables, or active substitution assignments reference it.
- FR-4.2: On attempted deletion with active references, the system shall return a clear error identifying referencing entities by type and count.
- FR-4.3: Whether deletion is hard-delete or soft-delete (deactivation) is governed by Open Question #3 (same concern as A4-2, A4-3).

### FR-5: Subject Competency Management

- FR-5.1: The system shall maintain competency links as a many-to-many relationship between Faculty and Course.
- FR-5.2: A competency link shall be a unique pair (faculty_id, course_id) — no duplicate links.
- FR-5.3: The system shall allow bulk addition of competencies (adding multiple courses at once).
- FR-5.4: When a competency is removed, the system shall [TBD: check if active sessions assign this faculty to that course? Warn? Block? See Open Question #7].

### FR-6: Multi-Campus Association Management

- FR-6.1: The system shall maintain campus associations as a many-to-many relationship between Faculty and Campus.
- FR-6.2: A campus association shall be a unique pair (faculty_id, campus_id) — no duplicates.
- FR-6.3: At least one campus association is required (a faculty must be associated with at least one campus).
- FR-6.4: Campus associations are consumed by the scheduling engine for multi-campus session assignment and travel-time buffer enforcement (Story 34/A4-35).

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-FAC-1 | Every faculty must reference an existing home department | Hard |
| HC-FAC-2 | Every campus association must reference an existing campus | Hard |
| HC-FAC-3 | Every competency link must reference an existing course | Hard |
| HC-FAC-4 | Faculty-campus association pairs must be unique | Hard |
| HC-FAC-5 | Faculty-course competency pairs must be unique | Hard |
| HC-FAC-6 | min_weekly_load must be ≤ max_weekly_load (when both explicitly set) | Hard |
| HC-FAC-7 | At least one campus association required per faculty | Hard |
| HC-FAC-8 | Deletion blocked if any entity (sessions, invigilation, historical data) references the faculty | Hard |
| HC-FAC-9 | Designation must be one of the defined values | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Home department must exist in hierarchy | Story 1 (A4-2) |
| Campuses must exist in hierarchy | Story 1 (A4-2) |
| Courses must exist for competency links | Story 2 (A4-3) |
| Availability windows are managed separately | Story 4 (A4-5) — references faculty profile |
| Scheduling engine uses competency for assignment validation | Story 10 (A4-11) |
| Conflict detection uses faculty for double-booking checks | Story 15 (A4-16) |
| Workload computation reads min/max and designation | Story 30 (A4-31) |
| Workload norms per cadre configured externally | Story 31 (A4-32) |
| Substitution uses competency for proposal | Story 29 (A4-30) |
| Exam invigilation references faculty for duty assignment | Story 22 (A4-23) |
| Travel-time buffers apply to multi-campus faculty | Story 34 (A4-35) |
| Historical archive references faculty in past timetables | Story 41 (A4-42) |
| RBAC enforces Visiting/Adjunct limited access based on designation | Story 43 (A4-44) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| name | Required, non-empty, max length [TBD] |
| identifier | [TBD — see Open Question #2: format, uniqueness scope] |
| designation | Required, one of defined values [TBD — confirm full list. At minimum: Professor, Associate Professor, Assistant Professor, Visiting, Adjunct] |
| qualification | Required, max length [TBD] |
| home_department | Required, must reference existing department (A4-2) |
| min_weekly_load | Non-negative number, ≤ max_weekly_load. [TBD: required or inherited from cadre?] |
| max_weekly_load | Positive number, ≥ min_weekly_load. [TBD: required or inherited from cadre?] |
| campus_associations | At least one required, each must reference existing campus, unique pairs |
| subject_competencies | Optional (can be empty at creation), each must reference existing course, unique pairs |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Faculty search/filter queries shall return within [TBD] for up to 500+ faculty (BRD Section 8: "500+ faculty"). |
| Audit | Every create, update, and delete action shall be recorded in the audit trail (Story 42 / A4-43). |
| Security | System Administrator creates/updates profiles (BRD Section 4). HOD read access + possible competency management (Open Question #1). Faculty read own profile only. Visiting/Adjunct limited to availability declaration + own session view. |

## 10. Acceptance Criteria

1. **Given** an HOD role, **When** they create a faculty profile with designation, home department, and subject competency list, **Then** the profile is persisted with all fields.
2. **Given** a faculty member, **When** campus associations are configured (e.g., associated with Campus A and Campus B), **Then** both associations are stored and visible.
3. **Given** a faculty member's competency list, **When** a course assignment is attempted for a course not in their competency list, **Then** the system flags a mismatch warning.
4. **Given** a faculty profile, **When** min/max weekly teaching load is configured per cadre designation, **Then** the values are stored and available for workload validation.
5. **Given** a search for faculty by subject competency, **When** "Data Structures" is searched, **Then** all faculty with that competency are returned.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| Faculty | id, name, identifier, designation, qualification, home_department_id (FK), min_weekly_load, max_weekly_load, created_at, updated_at | belongs to Department; has many CampusAssociations; has many Competencies |
| FacultyCampusAssociation | faculty_id (FK), campus_id (FK) | junction: Faculty ↔ Campus |
| FacultyCompetency | faculty_id (FK), course_id (FK) | junction: Faculty ↔ Course |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Campus Hierarchy (A4-2) | Home department and campuses must exist |
| Course Management (A4-3) | Courses must exist for competency links |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Role checks; Visiting/Adjunct access restrictions |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. A faculty member has exactly one home department. Multi-department teaching is represented through campus associations and cross-department course competencies — not through multiple home departments.
2. Visiting/Adjunct faculty are the same entity type as regular faculty, distinguished by the designation field. Their limited access is enforced by RBAC based on designation, not by a separate entity type.
3. Subject competency means "qualified/approved to teach" — it does not mean "currently assigned." Assignment to specific sessions is a scheduling operation, not a profile operation.
4. The workload min/max values stored on the faculty profile may be overridden by cadre-level norms configured in Story 31 (A4-32). The relationship between per-faculty values and cadre defaults needs clarification (Open Question #5).

## 14. Consistency Notes

- AC #1 says "HOD role" creates profiles. BRD Section 4 assigns "master data" to Admin, and HOD's stated role is "allocates faculty to courses" (assignment, not profile creation). Same ambiguity as A4-2 and A4-3 — flagged as Open Question #1.
- AC #3 says "flags a mismatch warning" — this is a soft warning at assignment time, not a hard block. The system warns but does not prevent assignment of faculty to courses outside their listed competency. This is correct per BRD 7.2 ("qualified/approved" is guidance, not an absolute restriction). If it should be a hard block, that's Open Question #8.
- The story says "min/max weekly teaching load" — Story 31 (A4-32) configures norms "per cadre." This story stores per-faculty values. The interaction between these two (override? inherit? both checked?) is Open Question #5.

## 15. Out of Scope

- Faculty availability windows and time preferences are managed by Story 4 (A4-5).
- Faculty workload computation and violation detection is managed by Story 30 (A4-31).
- Faculty leave and substitution is managed by Story 29 (A4-30).
- Faculty assignment to specific sessions is a scheduling engine operation (Story 10/A4-11).
- Bulk import of faculty is covered by Story 50 (A4-51).

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Who creates/updates faculty profiles? Admin only (BRD Section 4: master data = IT/Admin) or also HOD (BRD Section 4: HOD "allocates faculty to courses")? Does "allocates" include profile management or only session assignment? | Affects Actors, RBAC permissions, Security NFR. | Registrar / Academic Affairs |
| 2 | What is the faculty identifier format? Employee ID from HR? System-generated UUID? Unique system-wide or within department? | Affects FR-1.1, validation rules, uniqueness constraint. | IT / HR |
| 3 | Should faculty deletion be hard-delete or soft-delete? Same data retention concern: historical timetables reference faculty for years. Departing faculty's records need to persist in archives. | Affects FR-4, data model (active/inactive flag). | Registrar / IT |
| 4 | Can a faculty member's home department change (transfer between departments)? If yes, what happens to their existing session assignments in the old department? | Affects FR-3.2, cascading effects on scheduling. | Academic Affairs |
| 5 | Is min/max weekly load stored per-faculty (individual override), or inherited from cadre designation norms (configured in Story 31/A4-32), or both with per-faculty overriding cadre default? | Affects FR-1.1, FR-3.6, interaction with Story 31. | Academic Affairs / Compliance Officer |
| 6 | When a campus association is removed from a faculty member, and that faculty has active sessions on that campus, should the system block the removal or allow it with a warning? | Affects FR-3.5, scheduling consistency. | Academic Affairs |
| 7 | When a competency link is removed (faculty no longer qualified for a course), and that faculty is currently assigned to active sessions of that course, should the system block removal, warn, or silently allow? | Affects FR-5.4, scheduling consistency. | Academic Affairs |
| 8 | Is competency-based assignment a hard constraint (cannot assign faculty to course outside competency) or a soft warning (system warns but allows override)? AC #3 says "flags a mismatch warning" — confirming it's a warning, not a block. But should there be an option to make it a hard block per institution policy? | Affects HC constraints, scheduling engine behavior. | Academic Affairs |
| 9 | What is the full enumeration of designation values? BRD mentions: designation broadly, and "Visiting / Adjunct Faculty" as a specific role. Indian academic designations typically include: Professor, Associate Professor, Assistant Professor, Lecturer, Visiting Faculty, Adjunct Faculty, Professor Emeritus. Confirm the exact list. | Affects HC-FAC-9 allowed values. | Academic Affairs / HR |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.1 — "Faculty master: qualification, designation, home department, max/min weekly teaching load, subjects competent to teach, campus(es) of association" | FR-1.1 (all fields), FR-5 (competencies), FR-6 (campus associations) |
| 7.2 — "Subject competency mapping: Which courses a faculty member is qualified/approved to teach" | FR-5 (competency management), FR-2.4 (who-can-teach query) |
| 7.2 — "Min/max weekly teaching load: Institutional and accreditation-mandated workload bounds" | FR-1.1 (min/max fields), consumed by Story 30 |
| 7.2 — "Multi-department / multi-campus load: Combined workload visibility where faculty teach across units" | FR-6 (campus associations), consumed by Story 30 |
| Section 4 — "Visiting / Adjunct Faculty (external): Limited-access users who declare availability and view their own assigned sessions" | FR-1.1 (designation includes Visiting/Adjunct), Journey 6, Assumption #2, consumed by RBAC (A4-44) |
