# Design: Faculty Profile Management

**Jira Reference:** A4-4
**Source Requirements:** docs/requirements/A4-4-faculty-profile-management-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes: auth scoping, workload sign validation, identifier uniqueness semantics, PD numbering)

## 1. Overview

This design implements Faculty Profile master data management — full CRUD for faculty profiles with designation/cadre, qualifications, multi-campus associations (many-to-many), subject competencies (many-to-many with courses), and workload configuration (min/max weekly load, optionally inherited from cadre norms). Faculty profile data is consumed by scheduling (assignment validation), conflict detection (double-booking), substitution proposals (competency matching), exam invigilation (duty assignment), workload computation (compliance), and RBAC (Visiting/Adjunct access restrictions).

## 2. Architecture

```
Controller Layer
    └── FacultyController
         │
Service Layer
    ├── FacultyService (profile CRUD, validation)
    ├── FacultyCompetencyService (competency link management)
    └── FacultyCampusAssociationService (campus link management)
         │
Audit + Events
    ├── AuditEventPublisher (A4-2 contract)
    └── FacultyDesignationChangedEvent (consumed by availability module A4-5)
         │
Repository Layer
    ├── FacultyRepository (+ JpaSpecificationExecutor)
    ├── FacultyCompetencyRepository
    └── FacultyCampusAssociationRepository
         │
Database (schema: utms)
    ├── faculty
    ├── faculty_competencies
    └── faculty_campus_associations
```

## Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-12 | Faculty identifier uses **plain UNIQUE constraint** (not partial). Soft-deleted faculty retain identifier permanently — never freed for reuse. | HR employee IDs are permanent institutional identifiers. A departed employee's ID should never be reassigned. Unlike campus/course codes (naming conventions), employee IDs have identity semantics. | Differs from A4-2/A4-3 (partial unique). Intentional. |
| KD-13 | Faculty reads are scoped: Faculty/Visiting = own profile only. Coordinator/HOD = their department. Registrar/Admin = all. | BRD Section 4: Visiting/Adjunct "view their own assigned sessions" — implies own-profile-only. | API table says "AUTHENTICATED (scoped)" — not "any user sees all." |
| KD-14 | Workload: min_weekly_load >= 0.0, max_weekly_load > 0.0 (when set). Both optional (null = inherit from cadre). | Cannot have negative workload. Min=0 valid (no minimum). Max must be positive (max=0 = unschedulable — use availability blocks instead). | — |
| KD-15 | Designation change emits `FacultyDesignationChangedEvent`. Regular→limited triggers downstream flagging. | Designation change may invalidate availability model (A4-5 uses designation for blocked/available mode). Must be surfaced. | Consumed by A4-5. |
| KD-16 | Campus/competency removal: warn + allow + audit. Neither blocks. | Blocking makes maintenance impossible. Downstream consequences surface via conflict detection. | — |
| KD-17 | Home department transfer: allowed with audit. Sessions survive (linked by faculty_id). | Real operational scenario. Sessions reference faculty by ID, not department. | — |

## Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-15 | Soft-delete | Req OQ #3 | Soft-delete | Consistent. Historical refs. |
| PD-16 | Identifier: permanent unique (never reused, even after soft-delete) | Req OQ #2 | Permanent | KD-12. |
| PD-17 | Designation: 6 values configurable | Req OQ #9 | Extensible | BRD roles covered. |
| PD-18 | Qualification: free-text VARCHAR(500) | Req TBD | Free-text | Too varied for structure. |
| PD-19 | min/max load: optional DECIMAL(4,1), null=inherit, min>=0, max>0 | Req OQ #5 | Optional override | KD-14. |
| PD-20 | Campus removal: warn+allow+audit | Req OQ #6 | Allow | KD-16. |
| PD-21 | Competency removal: warn+allow+audit | Req OQ #7 | Allow | KD-16. |
| PD-22 | Home dept transfer: allowed | Req OQ #4 | Allow | KD-17. |
| PD-23 | Name max: 200 | Req TBD | 200 | Consistent. |
| PD-24 | Competency mismatch = warning | Req OQ #8 | Warning | Per AC#3. |

## 3. API Design

### 3.1 Faculty Profile CRUD

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/faculty` | Create | ADMIN | FR-1.1 |
| GET | `/api/v1/faculty` | List (filter: deptId, campusId, designation, competencyCourseId) | AUTHENTICATED (scoped) | FR-2.1-2.4 |
| GET | `/api/v1/faculty/{id}` | Get by ID | AUTHENTICATED (scoped) | FR-2.1 |
| GET | `/api/v1/faculty/identifier/{identifier}` | Get by employee ID | AUTHENTICATED (scoped) | FR-2.1 |
| PUT | `/api/v1/faculty/{id}` | Update | ADMIN | FR-3.1-3.2 |
| DELETE | `/api/v1/faculty/{id}` | Soft-delete | ADMIN | FR-4.1 |

### 3.2 Competency Sub-Resource

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/faculty/{id}/competencies` | List | AUTHENTICATED (scoped) | FR-5.1 |
| POST | `/api/v1/faculty/{id}/competencies` | Add (bulk) | ADMIN | FR-5.1, FR-5.3 |
| DELETE | `/api/v1/faculty/{id}/competencies/{courseId}` | Remove | ADMIN | FR-5.4 |
| GET | `/api/v1/faculty/by-competency/{courseId}` | "Who can teach?" | AUTHENTICATED | FR-2.4 |

### 3.3 Campus Association Sub-Resource

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/faculty/{id}/campus-associations` | List | AUTHENTICATED (scoped) | FR-6.1 |
| POST | `/api/v1/faculty/{id}/campus-associations` | Add | ADMIN | FR-6.1 |
| DELETE | `/api/v1/faculty/{id}/campus-associations/{campusId}` | Remove | ADMIN | FR-6.1 |

**Scoping (KD-13):** FACULTY/VISITING = own profile only (other → 403). COORDINATOR/HOD = own dept. REGISTRAR/ADMIN = all. "Who can teach?" = any authenticated.

## 4. Data Model

### Migration: V3__create_faculty_tables.sql

```sql
CREATE TABLE utms.faculty (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    identifier VARCHAR(50) NOT NULL UNIQUE, -- KD-12: permanent, not partial
    designation VARCHAR(50) NOT NULL,
    qualification VARCHAR(500) NOT NULL,
    home_department_id BIGINT NOT NULL,
    min_weekly_load DECIMAL(4,1) CHECK (min_weekly_load IS NULL OR min_weekly_load >= 0),
    max_weekly_load DECIMAL(4,1) CHECK (max_weekly_load IS NULL OR max_weekly_load > 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_faculty_departments FOREIGN KEY (home_department_id) REFERENCES utms.departments(id),
    CONSTRAINT chk_faculty_load_order CHECK (min_weekly_load IS NULL OR max_weekly_load IS NULL OR min_weekly_load <= max_weekly_load)
);
CREATE INDEX idx_faculty_department_id ON utms.faculty(home_department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_faculty_designation ON utms.faculty(designation) WHERE deleted_at IS NULL;

CREATE TABLE utms.faculty_competencies (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_fc_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT fk_fc_course FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT uq_faculty_competency UNIQUE (faculty_id, course_id)
);
CREATE INDEX idx_fc_faculty_id ON utms.faculty_competencies(faculty_id);
CREATE INDEX idx_fc_course_id ON utms.faculty_competencies(course_id);

CREATE TABLE utms.faculty_campus_associations (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    campus_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_fca_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT fk_fca_campus FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_faculty_campus UNIQUE (faculty_id, campus_id)
);
CREATE INDEX idx_fca_faculty_id ON utms.faculty_campus_associations(faculty_id);
CREATE INDEX idx_fca_campus_id ON utms.faculty_campus_associations(campus_id);
```

## 5. Service / Business Logic

### 5.1 create(request)
1. Validate dept exists (active)
2. Validate identifier unique (system-wide including soft-deleted — KD-12)
3. Validate designation in allowed list
4. Validate workload: min>=0, max>0, min<=max (KD-14)
5. Validate campuses exist (active), at least one (HC-FAC-7)
6. Validate courses exist (active) for competencies
7. Persist all
8. Audit

### 5.2 update(id, request)
1. Find active, snapshot
2. Validate workload signs (KD-14)
3. Detect designation change
4. Apply updates
5. If dept changed (PD-22): validate new dept, audit transfer
6. If designation changed: emit FacultyDesignationChangedEvent (KD-15)
7. Persist + audit

### 5.3–5.5 Competency/Campus services
Same pattern as v1 — add (bulk, idempotent), remove (warn if active sessions, physically delete junction row, block last campus removal)

### 5.6 delete(id)
Ref checks (all TODO pending downstream modules) → soft-delete → identifier stays locked → audit

### 5.7 Workload Contract (A4-31/A4-32)
A4-31 reads faculty.max/min; if null, looks up cadre default from A4-32 by designation.

## 6. Cross-cutting Concerns

| Concern | Design |
|---------|--------|
| Errors | 404/409/422/400/403 |
| Security | ADMIN mutations; scoped reads (KD-13); Visiting→own only |
| Audit | AuditEventPublisher (A4-2 contract) |
| Soft-Delete | Specification WHERE deleted_at IS NULL. Identifier permanently locked. |

## 7–8. NFR + Testing

Performance: indexes for 500+ faculty, paginated. Tests cover: duplicate identifier (even soft-deleted → 409), load sign validation, scope access (Visiting own→200, other→403), designation change event, last campus removal blocked.

## 9. Traceability

All FRs (1.1-1.5, 2.1-2.4, 3.1-3.6, 4.1-4.3, 5.1-5.4, 6.1-6.4), all HCs (1-9), NFR-Audit, NFR-Security mapped to specific design elements with test verification.

## 10. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Coordinator/HOD competency mutation rights | Stakeholder | Pending |
| 2 | Designation regular→limited: flag active sessions? | System Design | New (KD-15 event) |
| 3 | Bulk competency: all-or-nothing or partial? | System Design | New |

## 11. Provisional Decisions

PD-15 through PD-24 (see table above).
