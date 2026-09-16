# Requirement Document — Campus Hierarchy Master Data Management

## 1. Introduction

This document captures the detailed requirements for managing the institutional organizational hierarchy (Campus → Department → Program → Batch/Section) as the foundational master data layer for the University Timetable Management System. All scheduling, conflict detection, RBAC scoping, and reporting depend on this hierarchy being accurate and referentially consistent.

## 2. User Story

**A4-2:** As a System Administrator, I want to manage the hierarchical master data (Campus → Department → Program → Batch/Section) with full CRUD operations and referential integrity enforcement, so that the institution's organizational structure is accurately represented as the foundation for all scheduling activities.

**Story Points:** 5

## 3. Actors

| Actor | Interaction |
|---|---|
| System Administrator (IT) | Creates, reads, updates, and deletes all hierarchy entities. BRD Section 4 assigns master data management to this role. |
| Department Coordinator | Reads hierarchy data relevant to their department. (Note: BRD Section 4 does not explicitly grant coordinators mutation rights on hierarchy entities — if coordinators need to create/update batches within their dept, this must be confirmed with stakeholder. Labeled as inference below.) |
| Registrar | Reads institution-wide hierarchy for consolidated views. |
| RBAC System | Uses hierarchy for data segregation (scoping coordinators to their campus/department). |

**Inference requiring confirmation:** Coordinator mutation rights on batches/sections within their department are a plausible operational need but not explicitly stated in BRD Section 4. See Open Questions #1.

## 4. User Journeys

### Journey 1: Administrator Creates a New Campus

**Before:** The admin has valid credentials and admin role. The system is accessible.

**During:**
1. Admin navigates to master data management.
2. Admin selects "Create Campus."
3. Admin enters: campus name, campus code (unique), location/address.
4. System validates: code uniqueness, required fields present, format checks.
5. System persists the campus.
6. System confirms creation with the new campus visible in the hierarchy.

**After:** The campus is available as a parent for departments. Audit trail records the creation.

### Journey 2: Administrator Creates Department Under a Campus

**Before:** At least one campus exists in the system.

**During:**
1. Admin selects a parent campus.
2. Admin enters: department name, department code, any metadata.
3. System validates: parent campus exists, code uniqueness within campus, required fields.
4. System persists the department linked to the campus.
5. Department appears as a child of the selected campus.

**After:** The department is available as a parent for programs. RBAC can now scope users to this department.

### Journey 3: Administrator Updates a Department

**Before:** Department exists.

**During:**
1. Admin selects the department and chooses "Edit."
2. Admin modifies allowed fields (name, metadata). Code mutability is governed by Open Question #2.
3. System validates changes.
4. System persists the update.

**After:** Updated values are reflected. Audit trail records previous and new values.

### Journey 4: Administrator Attempts to Delete a Department with Active Children

**Before:** Department exists with programs or batches referencing it.

**During:**
1. Admin selects the department and chooses "Delete."
2. System checks for referential dependencies (programs, batches, faculty associations, scheduled sessions, historical/archived timetables referencing this department).
3. System finds active or historical references.
4. System rejects the deletion with a clear error listing which entities still reference the department.

**After:** Department remains unchanged. Admin must reassign or remove child references (or deactivate instead of hard-delete — see Open Question #3) before retrying.

### Journey 5: Administrator Attempts to Delete a Batch

**Before:** Batch exists, possibly referenced by scheduled sessions, elective registrations, or historical timetables.

**During:**
1. Admin selects the batch and chooses "Delete."
2. System checks: active sessions referencing this batch? Active elective registrations? Historical/archived timetable entries?
3. If references found → deletion rejected with error identifying referencing entities (sessions, registrations, archived timetables).
4. If no references → deletion permitted.

**After:** Batch removed (if no references) or unchanged (if blocked).

### Journey 6: Coordinator Queries the Full Hierarchy

**Before:** Hierarchy data exists.

**During:**
1. User requests the hierarchy tree for a campus or institution-wide.
2. System retrieves and assembles: Campus → Departments → Programs → Batches → Sections.
3. System returns the traversable tree structure.

**After:** User has visibility into the organizational structure for planning.

## 5. Functional Requirements

### FR-1: Campus CRUD

- FR-1.1: The system shall allow creation of a campus with: name (required), code (required, unique system-wide), and location/address (required).
- FR-1.2: The system shall allow reading a single campus by ID or code, and listing all campuses.
- FR-1.3: The system shall allow updating a campus's name and location. Whether code can be updated is governed by Open Question #2.
- FR-1.4: The system shall allow deletion of a campus only if no departments reference it AND no historical/archived timetables reference it.

### FR-2: Department CRUD

- FR-2.1: The system shall allow creation of a department with: name (required), code (required, unique within parent campus), and parent campus reference (required, must exist).
- FR-2.2: The system shall reject department creation if the referenced parent campus does not exist.
- FR-2.3: The system shall allow updating a department's name and metadata. Code mutability governed by Open Question #2.
- FR-2.4: The system shall allow deletion of a department only if no programs, faculty associations, scheduled sessions, or historical/archived timetables reference it.

### FR-3: Program CRUD

- FR-3.1: The system shall allow creation of a program with: name (required), code (required, unique within parent department), duration (semesters), degree type, and parent department reference (required, must exist).
- FR-3.2: The system shall reject program creation if the referenced parent department does not exist.
- FR-3.3: The system shall allow updating a program's name, duration, and degree type. Code mutability governed by Open Question #2.
- FR-3.4: The system shall allow deletion of a program only if no batches reference it AND no historical/archived timetables reference it.

### FR-4: Batch and Section CRUD

- FR-4.1: The system shall allow creation of a batch with: year/intake identifier (required), strength (required, positive integer), parent program reference (required, must exist), and elective basket reference (see Open Question #4 for ownership).
- FR-4.2: The system shall allow creation of sections within a batch with: section identifier (required, unique within parent batch), and optional sub-strength.
- FR-4.3: The system shall reject batch creation if the referenced parent program does not exist.
- FR-4.4: The system shall allow updating a batch's strength and elective basket reference.
- FR-4.5: The system shall allow updating a section's sub-strength and identifier (subject to uniqueness within batch).
- FR-4.6: The system shall allow deletion of a batch only if no scheduled sessions, elective registrations, or historical/archived timetables reference it.
- FR-4.7: The system shall allow deletion of a section only if no scheduled sessions or historical/archived timetables reference it.

### FR-5: Referential Integrity Enforcement

- FR-5.1: The system shall enforce parent-child referential integrity across the full hierarchy chain: Campus → Department → Program → Batch → Section.
- FR-5.2: The system shall reject any deletion where the entity is referenced by: child entities in the hierarchy, scheduled sessions, elective registrations, faculty associations, or historical/archived timetable data.
- FR-5.3: The system shall reject any creation with an invalid (non-existent) parent reference.
- FR-5.4: On attempted violation, the system shall return a clear error message identifying which specific entities still reference the entity being deleted (by type and count at minimum).

### FR-6: Hierarchy Traversal

- FR-6.1: The system shall provide a query that returns the full tree from any level downward (e.g., all departments under a campus, all batches under a program).
- FR-6.2: The system shall support querying the tree institution-wide for the Registrar role.
- FR-6.3: The system shall support scoped queries (only the requesting user's campus/department) for data segregation.

## 6. Constraints Owned by This Document

| ID | Constraint | Type |
|---|---|---|
| HC-HIER-1 | Every department must reference an existing campus | Hard |
| HC-HIER-2 | Every program must reference an existing department | Hard |
| HC-HIER-3 | Every batch must reference an existing program | Hard |
| HC-HIER-4 | Every section must reference an existing batch | Hard |
| HC-HIER-5 | Deletion is blocked if any entity (child, session, registration, or historical data) references the target | Hard |
| HC-HIER-6 | Campus code must be unique system-wide | Hard |
| HC-HIER-7 | Department code must be unique within its parent campus | Hard |
| HC-HIER-8 | Program code must be unique within its parent department | Hard |
| HC-HIER-9 | Section identifier must be unique within its parent batch | Hard |

## 7. Constraints Referenced from Other Documents

| Constraint | Source |
|---|---|
| RBAC data segregation uses campus/department hierarchy | Story 43 (A4-44) |
| Scheduling engine requires batch/section data with strength | Story 10 (A4-11) |
| Room allocation references campus for proximity | Story 5 (A4-6) |
| Historical timetable archive retains references to hierarchy entities | Story 41 (A4-42) |
| Elective basket entity is owned by elective stories | Stories 24/25 (A4-25/A4-26) |

## 8. Validation Rules

| Field | Entity | Rule |
|---|---|---|
| name | All entities | Required, non-empty, max length [TBD — confirm with stakeholder] |
| code | Campus | Required, unique system-wide, format [TBD — confirm allowed characters] |
| code | Department | Required, unique within parent campus, format [TBD] |
| code | Program | Required, unique within parent department, format [TBD] |
| section_identifier | Section | Required, unique within parent batch |
| strength | Batch | Required, positive integer, min 1 |
| sub_strength | Section | Optional, positive integer if provided, must be ≤ parent batch strength |
| parent reference | Dept, Program, Batch, Section | Required, must reference an existing entity of the correct parent type |
| elective_basket | Batch | See Open Question #4 — ownership and whether required or optional |

## 9. Non-Functional Requirements

| NFR | Requirement |
|---|---|
| Performance | Hierarchy tree query (full institution) shall return within [TBD — confirm acceptable response time] for the institution's scale. BRD Section 8 states 10,000+ students, 500+ faculty, 200+ rooms — it does not state campus/department/program/batch counts. Actual entity counts must be confirmed with stakeholder to define performance targets. |
| Audit | Every create, update, and delete action on hierarchy entities shall be recorded in the audit trail (consumed from Story 42 / A4-43). |
| Security | Only the System Administrator role (BRD Section 4: "IT / System Administrator — Manages master data") may mutate hierarchy data by default. See Open Question #1 for coordinator rights. |

## 10. Acceptance Criteria

1. **Given** a user with admin role, **When** they create a campus with name, code, and location, **Then** the campus is persisted and visible in the hierarchy.
2. **Given** an existing campus, **When** a department is created under it with a valid campus reference, **Then** the department appears as a child of that campus.
3. **Given** a department with active programs referencing it, **When** a user attempts to delete the department, **Then** the system rejects the deletion with a referential integrity error message.
4. **Given** a batch/section, **When** it is created with strength, program reference, and elective basket enrolled, **Then** all fields are persisted and the batch is correctly associated.
5. **Given** an invalid parent reference (e.g., non-existent campus ID for a department), **When** creation is attempted, **Then** the system returns a validation error and does not create the entity.
6. **Given** a hierarchy, **When** queried, **Then** the full tree (Campus → Department → Program → Batch → Section) is traversable.

## 11. Data Model (Conceptual)

| Entity | Key Attributes | Parent |
|---|---|---|
| Campus | id, name, code, location, created_at, updated_at | None (root) |
| Department | id, name, code, campus_id (FK), created_at, updated_at | Campus |
| Program | id, name, code, duration_semesters, degree_type, department_id (FK) | Department |
| Batch | id, year_identifier, strength, program_id (FK), elective_basket_ref | Program |
| Section | id, section_identifier, sub_strength, batch_id (FK) | Batch |

## 12. Dependencies

| Dependency | Description |
|---|---|
| Authentication (A4-45) | User must be authenticated before accessing master data |
| RBAC (A4-44) | Role checks determine create/update/delete permissions |
| Audit Trail (A4-43) | Mutations are logged by the audit service |
| Historical Archive (A4-42) | Archived timetables reference hierarchy entities — affects deletion semantics |

## 13. Assumptions

1. The hierarchy is strictly tree-structured: a department belongs to exactly one campus, a program to exactly one department, etc. No many-to-many at the hierarchy level.
2. Codes (campus, department, program) are assigned by the institution and provided during data entry — the system does not auto-generate them.
3. Batch strength is a planning estimate; actual enrolled student count may differ and is tracked separately by the elective/roster system.

## 14. Consistency Notes

- FR-4.1 lists elective_basket as a creation field. The validation table questions whether it's required or optional. This is an internal conflict — resolved by deferring to Open Question #4. The field's presence in the creation payload is required by the BRD ("Batch/section master: strength, program, elective basket enrolled"), but whether the value can be null/empty at creation time needs confirmation.
- FR-5.2 now explicitly names all referencing entity types (child entities, sessions, registrations, faculty associations, historical data) — no hand-waving via "orphan references."
- Deletion semantics interact with the BRD's data retention requirement (Section 8: "Retain historical timetables... for minimum [X] years"). Hard-deleting a hierarchy entity that historical data references would orphan that archived data. See Open Question #3.

## 15. Out of Scope

- Faculty-to-department associations are managed by Story 3 (A4-4), not this document.
- Room-to-campus/building associations are managed by Story 5 (A4-6), not this document.
- Bulk import of hierarchy data is covered by Story 50 (A4-51).
- Reporting on hierarchy structure (e.g., "how many programs per department") is not a direct requirement of this story.
- The elective basket entity itself (what baskets exist, their courses) is owned by Stories 24/25 — this document only references baskets as a foreign key on Batch.

## 16. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| 1 | Should Department Coordinators have create/update rights on batches and sections within their own department, or is all hierarchy mutation restricted to the System Administrator role? BRD Section 4 assigns "master data" to IT/Admin. | Affects Actors table, RBAC permissions, and Security NFR. | Registrar / Academic Affairs |
| 2 | Can entity codes (campus code, department code, program code) be changed after creation? Immutability simplifies referential integrity but reduces flexibility. If codes are mutable, what happens to external references (reports, exports) that used the old code? | Affects FR-1.3, FR-2.3, FR-3.3, and whether a code-change audit entry is needed. | IT / System Administrator |
| 3 | Should deletion be hard-delete or soft-delete (deactivation)? The BRD requires retaining historical timetables for years (Section 8 — Data Retention). Archived timetables reference departments/batches. Hard-deleting a "childless" entity could orphan historical data. Soft-delete (marking inactive) preserves referential integrity for archives while hiding from active use. | Affects FR-1.4, FR-2.4, FR-3.4, FR-4.6, FR-4.7, and the data model (needs active/inactive flag). | Registrar / IT |
| 4 | Who owns the "elective basket" entity that Batch.elective_basket references? The BRD says "elective basket enrolled" as a batch attribute. Is this a reference to a basket entity defined in the elective registration module (Stories 24/25), or is it a simple text label? If it's a FK, the basket must exist before a batch can reference it — affecting creation order and validation. | Affects FR-4.1 validation, cross-story data dependency, creation sequencing. | Academic Affairs / System Design |

## 17. Traceability

| BRD Requirement | FR Mapping |
|---|---|
| 6.1 — "Maintain hierarchical master data: Campus → Department → Program → Batch/Section" | FR-1 through FR-4 (full CRUD for each entity) |
| 6.1 — "Batch/section master: strength, program, elective basket enrolled" | FR-4.1, FR-4.2 |
| 7.1 — "Program & batch structure (Program duration, semesters, sections/batches per semester, batch strength)" | FR-3.1 (duration), FR-4.1 (strength) |
| Section 8 — "Data segregation by campus/department where applicable" | FR-6.3 (scoped queries) |
| Section 8 — "Data Retention: Retain historical timetables... for minimum [X] years" | FR-5.2 (deletion must not orphan historical references), Open Question #3 |
