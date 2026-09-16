# Requirement Document — Course Management

## 1. Introduction

This document captures the detailed requirements for managing course/subject master data including credit structure, type classification, prerequisite relationships, equipment requirements, and cross-listing. Course data drives session generation (how many sessions of what duration per week), room matching (equipment tags), conflict detection (prerequisite sequencing, elective vs. core clash logic), and compliance reporting (credit-to-contact-hour mapping).

## 2. User Story

**A4-3:** As a Department Coordinator, I want to manage courses with their credit structure (L-T-P split), type classification (core/elective/audit), prerequisite references, and equipment tags, so that the scheduling engine has accurate course data for session generation and room matching.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| System Administrator (IT) | Creates, reads, updates, and deletes courses. BRD Section 4 assigns master data management to this role. |
| Department Coordinator | Reads course data for their department. May also create/update courses within their department — see Open Question #1 (same ambiguity as A4-2). |
| Scheduling Engine (system) | Reads course L-T-P split to determine session count and duration. Reads equipment tags for room matching. |
| Conflict Detection (system) | Reads prerequisite relationships for sequencing checks. Reads course type for clash-avoidance rules. |
| Faculty Management (A4-4) | References courses in faculty competency mapping. |

## 4. User Journeys

### Journey 1: Administrator Creates a New Course

**Before:** The owning department exists in the hierarchy (A4-2 dependency). The admin has valid credentials and appropriate role.

**During:**
1. Admin selects the owning department.
2. Admin enters: course name, course code, L-T-P split (integers for Lecture/Tutorial/Practical hours per week), total credits, course type (core/elective/audit), equipment tags (optional list), prerequisite course references (optional list of existing course IDs/codes).
3. System validates: department exists, code uniqueness (scope per Open Question #2), L-T-P values are non-negative integers, referenced prerequisite courses all exist in the system, no circular prerequisite chain.
4. System persists the course.
5. Course is visible in the department's course catalogue.

**After:** The course is available for faculty assignment, scheduling, and elective registration. Audit trail records creation.

### Journey 2: Administrator Updates a Course's L-T-P Split

**Before:** Course exists. No active/published timetable sessions currently reference this course (or if they do — what happens? See Open Question #4).

**During:**
1. Admin selects the course and chooses "Edit."
2. Admin modifies the L-T-P split (e.g., changes from 3-1-0 to 3-0-2 — adding a practical component).
3. System validates new values (non-negative integers).
4. System persists the update.

**After:** The next scheduling run will use the new L-T-P to compute session count/duration. If sessions already exist for this course in an active draft, they may become inconsistent — see Open Question #4. Audit trail records previous and new values.

### Journey 3: Administrator Adds a Prerequisite to a Course

**Before:** Both courses exist. The prerequisite relationship doesn't already exist.

**During:**
1. Admin selects a course and adds a prerequisite reference to another course.
2. System validates: referenced course exists, adding this prerequisite does not create a circular chain (A requires B requires A).
3. System persists the prerequisite link.

**After:** Conflict detection (Story 15, AC 9) can now use this relationship for sequencing checks.

### Journey 4: Administrator Attempts to Delete a Course with Active References

**Before:** Course exists. Scheduled sessions, faculty assignments, elective registrations, or historical timetables reference it.

**During:**
1. Admin selects the course and chooses "Delete."
2. System checks for references: active sessions, faculty competency links, elective registrations, historical/archived timetables.
3. References found → deletion rejected with error listing referencing entities by type and count.

**After:** Course unchanged. Admin must remove references first or deactivate (per Open Question #3).

### Journey 5: Coordinator Searches Courses by Equipment Tag

**Before:** Courses exist with equipment tags.

**During:**
1. Coordinator searches for courses requiring "chemistry_fume_hood."
2. System returns all courses with that tag.

**After:** Coordinator can verify room-matching requirements for scheduling.

## 5. Functional Requirements

### FR-1: Course Creation

- FR-1.1: The system shall allow creation of a course with the following fields:
  - name (required)
  - code (required, unique within scope per Open Question #2)
  - L-T-P split: lecture_hours, tutorial_hours, practical_hours (each required, non-negative integer, at least one must be > 0)
  - credits (required, positive number)
  - type (required, one of: core, elective, audit)
  - owning department reference (required, must exist in hierarchy)
  - equipment_tags (optional, array of string tags)
  - prerequisite_courses (optional, array of references to existing courses)
- FR-1.2: The system shall reject creation if the owning department does not exist.
- FR-1.3: The system shall reject creation if any referenced prerequisite course does not exist.
- FR-1.4: The system shall reject creation if adding the specified prerequisites would create a circular dependency chain.

### FR-2: Course Reading and Search

- FR-2.1: The system shall allow reading a single course by ID or code.
- FR-2.2: The system shall allow listing courses filtered by: department, type (core/elective/audit), equipment tag, or any combination.
- FR-2.3: The system shall return course data including its L-T-P split, type, prerequisites, and equipment tags.
- FR-2.4: The system shall support listing all courses that have a given course as a prerequisite (reverse lookup — "what depends on this course?").

### FR-3: Course Update

- FR-3.1: The system shall allow updating: name, L-T-P split, credits, type, equipment_tags, prerequisite_courses. Code mutability governed by Open Question #2.
- FR-3.2: On prerequisite update, the system shall validate no circular dependency is introduced.
- FR-3.3: On L-T-P update, the system shall validate values remain non-negative integers with at least one > 0.
- FR-3.4: On type change (e.g., elective → core), the system shall [TBD — confirm impact on existing elective registrations referencing this course. See Open Question #5].

### FR-4: Course Deletion

- FR-4.1: The system shall allow deletion of a course only if no scheduled sessions, faculty competency links, elective registrations, or historical/archived timetables reference it.
- FR-4.2: On attempted deletion with active references, the system shall return a clear error identifying referencing entities by type and count.
- FR-4.3: Whether deletion is hard-delete or soft-delete (deactivation) is governed by Open Question #3.

### FR-5: Cross-Listed Course Support

- FR-5.1: The system shall support marking a course as shared/cross-listed across multiple departments (BRD 7.1: "Cross-listed / cross-department courses: Courses shared across programs/departments requiring synchronized slots").
- FR-5.2: A cross-listed course shall be visible to all departments it is shared with.
- FR-5.3: The mechanism for cross-listing (flag on course with multi-department references, or separate junction entity) is a design decision — but the requirement is: a course can belong to/be visible from more than one department. See Open Question #6.

### FR-6: Prerequisite Integrity

- FR-6.1: The system shall maintain prerequisite relationships as directed links between courses (Course A requires Course B).
- FR-6.2: The system shall prevent circular prerequisite chains at creation and update time.
- FR-6.3: When a prerequisite course is deleted (if deletion is permitted), the system shall [TBD — remove the link? Block deletion? See Open Question #7].

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-CRS-1 | Every course must reference an existing owning department | Hard |
| HC-CRS-2 | Every prerequisite reference must point to an existing course | Hard |
| HC-CRS-3 | Prerequisite chains must be acyclic (no circular dependencies) | Hard |
| HC-CRS-4 | Course code must be unique within [scope TBD — see Open Question #2] | Hard |
| HC-CRS-5 | L-T-P values must be non-negative integers with at least one > 0 | Hard |
| HC-CRS-6 | Course type must be one of: core, elective, audit | Hard |
| HC-CRS-7 | Deletion blocked if any entity (sessions, registrations, competency links, historical data) references the course | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| Owning department must exist in hierarchy | Story 1 (A4-2) — HC-HIER-1 through HC-HIER-4 |
| Scheduling engine uses L-T-P to determine session count and duration | Story 10 (A4-11) |
| Conflict detection uses prerequisite relationships for sequencing checks | Story 15 (A4-16), AC 9 |
| Room allocation uses equipment_tags for matching | Story 23 (A4-24) |
| Elective registration uses course type for clash-avoidance logic | Story 24 (A4-25) |
| Cross-department scheduling synchronizes cross-listed course slots | Story 26 (A4-27) |
| Faculty competency links courses to faculty profiles | Story 3 (A4-4) |
| Historical archive references courses in past timetables | Story 41 (A4-42) |
| Compliance reporting uses credits and L-T-P for credit-to-contact mapping | Story 33 (A4-34) |

## 8. Validation Rules

| Field | Rule |
|---|---|
| name | Required, non-empty, max length [TBD — confirm with stakeholder] |
| code | Required, unique within [TBD — see Open Question #2], format [TBD] |
| lecture_hours (L) | Required, non-negative integer |
| tutorial_hours (T) | Required, non-negative integer |
| practical_hours (P) | Required, non-negative integer |
| L + T + P | Must be > 0 (at least one component must have hours) |
| credits | Required, positive number (integer or decimal — [TBD: confirm if half-credits exist]) |
| type | Required, one of: core, elective, audit |
| owning_department | Required, must reference existing department (A4-2) |
| equipment_tags | Optional, array of non-empty strings |
| prerequisite_courses | Optional, array of references to existing courses; must not create cycles |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Course search/filter queries shall return within [TBD — confirm acceptable response time] for the institution's catalogue size. BRD does not specify course count targets. |
| Audit | Every create, update, and delete action shall be recorded in the audit trail (Story 42 / A4-43). |
| Security | Only System Administrator role may mutate course data by default (BRD Section 4). See Open Question #1 for coordinator rights. |

## 10. Acceptance Criteria

1. **Given** a coordinator role, **When** they create a course with L-T-P split (e.g., 3-1-2), credits, and type (core/elective/audit), **Then** the course is persisted with all fields correctly stored.
2. **Given** a course with a prerequisite reference, **When** the referenced prerequisite course does not exist in the system, **Then** creation is rejected with a validation error.
3. **Given** a course, **When** equipment tags are specified (e.g., ["projector", "chemistry_fume_hood"]), **Then** they are stored and retrievable for room-matching during scheduling.
4. **Given** a search by course type, **When** "elective" is filtered, **Then** only elective-classified courses are returned.
5. **Given** a course marked as cross-listed across departments, **When** viewed from either department, **Then** it appears with the shared/cross-listed designation (BRD 7.1).

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Relationships |
|---|---|---|
| Course | id, name, code, lecture_hours, tutorial_hours, practical_hours, credits, type, department_id (FK), equipment_tags, is_cross_listed | belongs to Department; has many Prerequisites; referenced by Sessions, Registrations, FacultyCompetency |
| CoursePrerequisite | id, course_id (FK), prerequisite_course_id (FK) | junction table for directed prerequisite links |
| CourseDepartmentLink (if cross-listing uses junction) | course_id (FK), department_id (FK) | For cross-listed courses visible to multiple departments. See Open Question #6. |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Campus Hierarchy (A4-2) | Owning department must exist before a course can be created |
| Authentication (A4-45) | User must be authenticated |
| RBAC (A4-44) | Role checks determine mutation permissions |
| Audit Trail (A4-43) | Mutations logged |

## 13. Assumptions

1. A course belongs to exactly one owning department (primary home). Cross-listing makes it visible to additional departments but does not change ownership.
2. L-T-P values are integers representing hours per week. Fractional hours are not supported (if needed, this is an Open Question).
3. Equipment tags are free-form strings that must match room equipment tags exactly (case-sensitive matching is a design decision).
4. A course with L=0, T=0, P=3 is valid (pure practical course). A course with L=3, T=0, P=0 is valid (pure lecture course). All-zeros is not valid.

## 14. Consistency Notes

- FR-1.1 lists prerequisite_courses as "optional" at creation. This is consistent with the validation table (also optional). No conflict here.
- FR-5 (cross-listing) describes the requirement behaviorally. The mechanism (flag vs. junction table) is deferred to design. The requirement is: "visible from multiple departments, synchronized scheduling." This is consumed by Story 26 (A4-27).
- The story's AC #1 says "coordinator role" creates courses. BRD Section 4 assigns master data to Admin. This is the same ambiguity as A4-2 — flagged consistently as Open Question #1.

## 15. Out of Scope

- Faculty-to-course competency mapping is managed by Story 3 (A4-4).
- Room-to-equipment matching logic during scheduling is managed by Story 23 (A4-24).
- Elective registration and clash prevention logic is managed by Story 24 (A4-25).
- Prerequisite consumption during conflict detection is managed by Story 15 (A4-16), AC 9.
- Bulk import of courses is covered by Story 50 (A4-51).
- Curriculum planning (which courses to offer each semester) is not in BRD scope.

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Should Coordinators have create/update rights on courses within their department, or is all course mutation restricted to the System Administrator? BRD Section 4 assigns "master data" to IT/Admin, but the story is written from Coordinator perspective. | Affects Actors table, RBAC permissions, Security NFR. | Registrar / Academic Affairs |
| 2 | What is the uniqueness scope for course codes? System-wide (no two courses anywhere share a code) or within owning department? | Affects HC-CRS-4, FR-1.1 validation. | Academic Affairs |
| 3 | Should course deletion be hard-delete or soft-delete (deactivation)? Same data retention concern as hierarchy (A4-2, Open Question #3): historical timetables reference courses for years. | Affects FR-4.1, FR-4.3, data model (needs active/inactive flag). | Registrar / IT |
| 4 | If a course's L-T-P split is updated while sessions for that course exist in an active/unpublished draft, what should happen? Invalidate those sessions? Flag them for re-generation? Allow the inconsistency until next generation? | Affects FR-3.1 behavior, user journey 2 "after" phase. | Academic Affairs / System Design |
| 5 | If a course's type changes from elective to core (or vice versa), what happens to existing elective registrations for that course? Are they invalidated? Grandfathered? | Affects FR-3.4, cross-story impact on Story 24 (A4-25). | Academic Affairs |
| 6 | How is cross-listing implemented at the data level? A flag with a multi-department reference list? A junction table? Does a cross-listed course have a "primary" department and "secondary" departments? | Affects FR-5, data model, how Story 26 (A4-27) queries synchronized courses. Design decision, but ownership structure affects requirements. | System Design |
| 7 | When a course that serves as a prerequisite for other courses is deleted (or deactivated), what happens to the prerequisite links pointing to it? Remove them silently? Block the deletion? Notify owners of dependent courses? | Affects FR-6.3, deletion semantics. | Academic Affairs |
| 8 | Can credits be fractional (e.g., 1.5 credits for a half-credit course)? Or always integers? | Affects validation rule for credits field. | Academic Affairs |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.1 — "Course master: credit hours, contact hours (L-T-P split), course type (core/elective/audit), prerequisite courses" | FR-1.1 (creation with all fields), FR-6 (prerequisites) |
| 7.1 — "Course credit structure (L-T-P): Lecture/Tutorial/Practical hour split per course; determines number and length of weekly sessions" | FR-1.1 (L-T-P fields), consumed by Story 10 |
| 7.1 — "Core vs. elective classification: Determines mandatory vs. flexible scheduling and clash-avoidance rules" | FR-1.1 (type field), HC-CRS-6, consumed by Story 24 |
| 7.1 — "Prerequisite mapping: Ensures dependent courses are not scheduled in conflicting sequence across semesters" | FR-6 (prerequisite integrity), consumed by Story 15 AC 9 |
| 7.1 — "Cross-listed / cross-department courses: Courses shared across programs/departments requiring synchronized slots" | FR-5 (cross-listed support), consumed by Story 26 |
| 6.6 — "Track lab-specific equipment/software prerequisites for allocation matching" | FR-1.1 (equipment_tags field), consumed by Story 23 |
