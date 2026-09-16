# Design: Campus Hierarchy Master Data Management

**Jira Reference:** A4-2
**Source Requirements:** docs/requirements/A4-2-campus-hierarchy-master-data-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25

## 1. Overview

This design implements the Campus Hierarchy Master Data module — full CRUD for the 5-level organizational hierarchy: Campus → Department → Program → Batch → Section. The module enforces referential integrity (no orphan references, no deletion of entities with active children or historical references), provides scoped and institution-wide tree traversal queries from any level, and records all mutations in the audit trail within the same transaction.

This is the foundational module — all other modules (scheduling, conflict detection, RBAC) depend on hierarchy data existing before they can operate.

## 2. Architecture

```
Controller Layer (HTTP)
    ├── CampusController
    ├── DepartmentController
    ├── ProgramController
    ├── BatchController
    ├── SectionController
    └── HierarchyTreeController
         │
Service Layer (Business Logic + Transactions)
    ├── CampusService
    ├── DepartmentService
    ├── ProgramService
    ├── BatchService
    ├── SectionService
    └── HierarchyTreeService
         │
Audit Integration
    └── AuditEventPublisher (emits audit events within same transaction)
         │
Repository Layer (Data Access)
    ├── CampusRepository
    ├── DepartmentRepository
    ├── ProgramRepository
    ├── BatchRepository
    └── SectionRepository
         │
Database (PostgreSQL - schema: utms)
    ├── campuses
    ├── departments
    ├── programs
    ├── batches
    └── sections
```

**Key Decisions:**

| # | Decision | Rationale | Interaction Notes |
|---|---|---|---|
| KD-1 | Soft-delete pattern (set deleted_at, never physical remove) | Satisfies FR-5.2 + data retention NFR. Historical timetables can always resolve FK references. | Interacts with KD-2 — see below. |
| KD-2 | Partial unique indexes: `UNIQUE ... WHERE deleted_at IS NULL` | Allows code reuse after soft-deletion. A deleted campus's code is freed for reuse. Without this, soft-delete permanently locks all codes. | Resolves the KD-1 + uniqueness collision. |
| KD-3 | `deleted_at` is the single source of truth for deletion status. No separate `is_active` column. | Avoids divergence between two columns representing the same state. Active = `deleted_at IS NULL`. Queries use the Specification pattern filtering on `deleted_at`. | — |
| KD-4 | Flat controller structure with parent IDs in request bodies | Hierarchy expressed via FK fields, not nested URL paths. Simpler routing, clearer ownership per entity. | — |
| KD-5 | Referential integrity at BOTH database (FK) AND service (count check) layers | DB FKs prevent orphans. Service-layer checks provide user-friendly error messages with referencing entity types and counts (FR-5.4). | — |

**Provisional Decisions (pending stakeholder ratification):**

| # | Decision Made | Requirement TBD It Resolves | Default Chosen | Rationale |
|---|---|---|---|---|
| PD-1 | Soft-delete (not hard-delete) | Requirements OQ #3 | Soft-delete | Safest default for data retention. Reversing to hard-delete loses data; reversing from soft-delete does not. |
| PD-2 | Name max length: 200 chars | Requirements validation [TBD] | 200 | Accommodates long names like "Department of Electronics and Communication Engineering." |
| PD-3 | Code max length: 20 chars | Requirements validation [TBD] | 20 | Sufficient for codes like "BTCS-2026." |
| PD-4 | Code format: `^[A-Za-z0-9_-]+$` | Requirements validation [TBD] | Alphanumeric + hyphen/underscore | Standard identifier format. No spaces or special characters. |
| PD-5 | Location max length: 500 chars | Unconfirmed | 500 | Accommodates full addresses. |

## 3. API Design

### 3.1 Campus Endpoints

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/campuses` | Create campus | ADMIN | FR-1.1 |
| GET | `/api/v1/campuses` | List all (paginated, filterable) | ANY_AUTHENTICATED | FR-1.2 |
| GET | `/api/v1/campuses/{id}` | Get by ID | ANY_AUTHENTICATED | FR-1.2 |
| GET | `/api/v1/campuses/code/{code}` | Get by code | ANY_AUTHENTICATED | FR-1.2 |
| PUT | `/api/v1/campuses/{id}` | Update (name, location only) | ADMIN | FR-1.3 |
| DELETE | `/api/v1/campuses/{id}` | Soft-delete | ADMIN | FR-1.4 |

**POST /api/v1/campuses**
```json
// Request
{"name": "Main Campus", "code": "MC01", "location": "123 University Road, City"}

// Success (201)
{"data": {"id": 1, "name": "Main Campus", "code": "MC01", "location": "123 University Road, City", "createdAt": "2026-08-25T10:00:00Z", "updatedAt": "2026-08-25T10:00:00Z"}}

// Error: duplicate code (409)
{"timestamp": "...", "status": 409, "error": "Conflict", "message": "Campus with code 'MC01' already exists", "path": "/api/v1/campuses"}

// Error: deletion blocked (422) — only shows checks actually performed
{"timestamp": "...", "status": 422, "error": "Unprocessable Entity", "message": "Cannot delete campus: has active references", "path": "/api/v1/campuses/1", "details": [{"referenceType": "Department", "count": 3}]}
```

Note: The 422 error response only includes reference types the service currently checks. Historical timetable checks will appear once A4-42 is integrated (see OQ #4).

### 3.2 Department Endpoints

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/departments` | Create | ADMIN | FR-2.1 |
| GET | `/api/v1/departments` | List (filterable by campusId) | ANY_AUTHENTICATED | FR-2.1 |
| GET | `/api/v1/departments/{id}` | Get by ID | ANY_AUTHENTICATED | FR-2.1 |
| PUT | `/api/v1/departments/{id}` | Update (name only) | ADMIN | FR-2.3 |
| DELETE | `/api/v1/departments/{id}` | Soft-delete | ADMIN | FR-2.4 |

### 3.3 Program Endpoints

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/programs` | Create | ADMIN | FR-3.1 |
| GET | `/api/v1/programs` | List (filterable by departmentId) | ANY_AUTHENTICATED | FR-3.1 |
| GET | `/api/v1/programs/{id}` | Get by ID | ANY_AUTHENTICATED | FR-3.1 |
| PUT | `/api/v1/programs/{id}` | Update (name, duration, degreeType) | ADMIN | FR-3.3 |
| DELETE | `/api/v1/programs/{id}` | Soft-delete | ADMIN | FR-3.4 |

### 3.4 Batch Endpoints

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/batches` | Create | ADMIN | FR-4.1 |
| GET | `/api/v1/batches` | List (filterable by programId) | ANY_AUTHENTICATED | FR-4.1 |
| GET | `/api/v1/batches/{id}` | Get by ID | ANY_AUTHENTICATED | FR-4.1 |
| PUT | `/api/v1/batches/{id}` | Update (strength, electiveBasket) | ADMIN | FR-4.4 |
| DELETE | `/api/v1/batches/{id}` | Soft-delete | ADMIN | FR-4.6 |

### 3.5 Section Endpoints

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/batches/{batchId}/sections` | Create section | ADMIN | FR-4.2 |
| GET | `/api/v1/batches/{batchId}/sections` | List sections for batch | ANY_AUTHENTICATED | FR-4.2 |
| GET | `/api/v1/sections/{id}` | Get section by ID | ANY_AUTHENTICATED | FR-4.2 |
| PUT | `/api/v1/sections/{id}` | Update (identifier, subStrength) | ADMIN | FR-4.5 |
| DELETE | `/api/v1/sections/{id}` | Soft-delete | ADMIN | FR-4.7 |

### 3.6 Hierarchy Tree Endpoints

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/hierarchy/tree` | Full institution tree | REGISTRAR, ADMIN | FR-6.2 |
| GET | `/api/v1/campuses/{id}/tree` | Tree under campus | ANY_AUTHENTICATED (scoped) | FR-6.1 |
| GET | `/api/v1/departments/{id}/tree` | Tree under department | ANY_AUTHENTICATED (scoped) | FR-6.1 |
| GET | `/api/v1/programs/{id}/tree` | Tree under program | ANY_AUTHENTICATED (scoped) | FR-6.1 |
| GET | `/api/v1/batches/{id}/tree` | Tree under batch (sections) | ANY_AUTHENTICATED (scoped) | FR-6.1 |

**Scoped access behavior (FR-6.3):** If a user requests a tree for an entity outside their RBAC scope, the system returns **403 Forbidden** — not an empty result. An empty result is indistinguishable from "no data exists." A 403 clearly communicates access denial.

## 4. Data Model

### 4.1 Entities

All entities extend BaseEntity (id, created_at, updated_at, created_by, updated_by, deleted_at). No `is_active` column (KD-3).

| Entity | Table | Key Fields | Relationships |
|--------|-------|-----------|---------------|
| Campus | campuses | name, code (unique active), location | — |
| Department | departments | name, code (unique per active campus), campus_id FK | ManyToOne → Campus |
| Program | programs | name, code (unique per active dept), duration_semesters, degree_type enum, department_id FK | ManyToOne → Department |
| Batch | batches | year_identifier, strength (>0), elective_basket (String), program_id FK | ManyToOne → Program |
| Section | sections | section_identifier (unique per active batch), sub_strength (nullable), batch_id FK | ManyToOne → Batch |

### 4.2 Flyway Migration

**V1__create_campus_hierarchy_tables.sql**
```sql
CREATE SCHEMA IF NOT EXISTS utms;

CREATE TABLE utms.campuses (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, code VARCHAR(20) NOT NULL,
    location VARCHAR(500) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL, deleted_at TIMESTAMP NULL);
CREATE UNIQUE INDEX uq_campuses_code ON utms.campuses(code) WHERE deleted_at IS NULL;

CREATE TABLE utms.departments (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, code VARCHAR(20) NOT NULL,
    campus_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL, deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_departments_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id));
CREATE UNIQUE INDEX uq_departments_campus_code ON utms.departments(campus_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_departments_campus_id ON utms.departments(campus_id) WHERE deleted_at IS NULL;

CREATE TABLE utms.programs (
    id BIGSERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, code VARCHAR(20) NOT NULL,
    department_id BIGINT NOT NULL, duration_semesters INTEGER NOT NULL,
    degree_type VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL, deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_programs_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id));
CREATE UNIQUE INDEX uq_programs_department_code ON utms.programs(department_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_programs_department_id ON utms.programs(department_id) WHERE deleted_at IS NULL;

CREATE TABLE utms.batches (
    id BIGSERIAL PRIMARY KEY, year_identifier VARCHAR(20) NOT NULL,
    strength INTEGER NOT NULL CHECK (strength > 0), elective_basket VARCHAR(100),
    program_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL, deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_batches_programs FOREIGN KEY (program_id) REFERENCES utms.programs(id));
CREATE INDEX idx_batches_program_id ON utms.batches(program_id) WHERE deleted_at IS NULL;

CREATE TABLE utms.sections (
    id BIGSERIAL PRIMARY KEY, section_identifier VARCHAR(10) NOT NULL,
    sub_strength INTEGER, batch_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL, updated_by VARCHAR(100) NOT NULL, deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_sections_batches FOREIGN KEY (batch_id) REFERENCES utms.batches(id));
CREATE UNIQUE INDEX uq_sections_batch_identifier ON utms.sections(batch_id, section_identifier) WHERE deleted_at IS NULL;
CREATE INDEX idx_sections_batch_id ON utms.sections(batch_id) WHERE deleted_at IS NULL;
```

## 5. Service / Business Logic

### 5.1 Deletion Reference Checks (FR-5.2)

| Entity | References Checked | Status |
|--------|-------------------|--------|
| Campus | Departments (active), Historical timetables | Depts: implemented. Historical: TODO pending A4-42. |
| Department | Programs (active), Faculty associations, Scheduled sessions, Historical timetables | Programs: implemented. Others: TODO pending respective modules. |
| Program | Batches (active), Historical timetables | Batches: implemented. Historical: TODO pending A4-42. |
| Batch | Sections (active), Scheduled sessions, Elective registrations, Historical timetables | Sections: implemented. Others: TODO pending respective modules. |
| Section | Scheduled sessions, Historical timetables | All: TODO pending scheduling + A4-42. |

Service delete methods build a `List<ReferenceCount>` and throw `BusinessRuleViolationException` if non-empty. Adding a new check = adding one line.

### 5.2 Cross-Entity Validation

| Validation | Entity | When | Behavior |
|-----------|--------|------|----------|
| sub_strength <= batch.strength | Section | Create and Update | Reject 422 if exceeded |
| batch.strength >= max(active sections.sub_strength) | Batch | Update (strength reduction) | Reject 422 if new strength < any existing section's sub_strength |
| Parent exists and is active | All child entities | Create | Reject 404 if parent missing or soft-deleted |
| Code unique within active scope | Dept (per campus), Program (per dept), Section (per batch) | Create | Reject 409 if duplicate among active records |

### 5.3 Audit Integration

Every service mutation method calls `auditEventPublisher.publish(...)` within the `@Transactional` boundary:
- create → `AuditEvent.created(entityType, id, newValue)`
- update → `AuditEvent.updated(entityType, id, previousSnapshot, newValue)`
- delete → `AuditEvent.deleted(entityType, id, deletedValue)`

The `previousSnapshot` is captured BEFORE mutation via `entity.snapshot()` (a deep-copy method on each entity).

### 5.4 Transaction Boundaries

- All mutations: `@Transactional` (includes audit event publication)
- All reads: `@Transactional(readOnly = true)`
- Tree queries: `@EntityGraph` to prevent N+1

## 6. Cross-cutting Concerns

### 6.1 Error Handling

| Exception | Status | Trigger |
|-----------|--------|---------|
| EntityNotFoundException | 404 | Entity not found or soft-deleted |
| ConflictException | 409 | Duplicate code |
| BusinessRuleViolationException | 422 | Deletion blocked; sub_strength violation; batch strength reduction below sections |
| MethodArgumentNotValidException | 400 | Jakarta validation |
| AccessDeniedException | 403 | Scope violation |

### 6.2 Security

- Mutations: `@PreAuthorize("hasRole('ADMIN')")`
- Reads: `@PreAuthorize("isAuthenticated()")`
- Tree endpoints: scope check → 403 if entity outside user's campus/dept
- Institution tree: `@PreAuthorize("hasAnyRole('REGISTRAR', 'ADMIN')")`

### 6.3 Audit Trail Integration (Contract with A4-43)

**Component:** `AuditEventPublisher` — publishes Spring Application Events.

**Listener (provided by A4-43):** `@TransactionalEventListener(phase = BEFORE_COMMIT)` persists to `audit_events` table.

**Contract:**
- AuditEvent contains: entityType, entityId, action (CREATED/UPDATED/DELETED), previousValue (nullable), newValue, userId, timestamp.
- Published WITHIN the @Transactional boundary → committed in same DB transaction.
- If A4-43 not yet built: no listener = no-op. Events published but not persisted. Seamless once listener is registered.

### 6.4 Soft-Delete Filtering

- Repository Specifications add `WHERE deleted_at IS NULL` by default.
- Partial unique indexes (KD-2) enforce uniqueness among active records only.
- Soft-deleted records visible only via explicit admin/audit queries.

## 7. Non-Functional Design

| NFR | Design |
|-----|--------|
| Performance | `@EntityGraph` for tree; `@Cacheable("hierarchyTree")` per campus, evicted on mutation. |
| Audit | AuditEventPublisher + TransactionalEventListener (Section 6.3). |
| Security | @PreAuthorize + Specification scope filtering (Section 6.2). |

## 8. Testing Strategy

### Unit Tests
- CampusServiceTest: create (happy, duplicate), update, delete (no refs, with refs), audit event published
- DepartmentServiceTest: create (valid parent, invalid parent, duplicate code same campus, duplicate code different campus succeeds)
- SectionServiceTest: sub_strength > batch.strength → 422
- BatchServiceTest: reduce strength below existing section sub_strength → 422

### Integration Tests
- POST campus → 201; POST duplicate → 409
- DELETE campus with deps → 422 with details
- DELETE then re-POST same code → 201 (partial unique index verification)
- GET tree → nested structure
- Coordinator GETs another campus's tree → 403
- Registrar GETs /hierarchy/tree → full data
- Section sub_strength > batch.strength → 422
- Flyway migration clean run

## 9. Requirement Traceability

| Requirement | Design Element | Notes |
|---|---|---|
| FR-1.1-1.4 | Campus CRUD endpoints + CampusService | Delete checks deps + TODO historical |
| FR-2.1-2.4 | Department CRUD + DepartmentService | Parent validation, scoped uniqueness |
| FR-3.1-3.4 | Program CRUD + ProgramService | Same pattern |
| FR-4.1-4.7 | Batch + Section CRUD | Includes sub_strength cross-validation |
| FR-5.1 | FK constraints in migration | 4 relationships |
| FR-5.2 | Service deletion with reference list (Table 5.1) | All entity types listed with status |
| FR-5.3 | Service.create() validates parent active | 404 on missing |
| FR-5.4 | BusinessRuleViolationException + details array | Type + count |
| FR-6.1 | 4 tree endpoints (campus/dept/program/batch) | All levels covered |
| FR-6.2 | /hierarchy/tree (REGISTRAR/ADMIN) | Restricted |
| FR-6.3 | Specification filter + 403 behavior | Defined |
| HC-HIER-1-4 | DB FK constraints | Present |
| HC-HIER-5 | Service reference checks | Table 5.1 |
| HC-HIER-6-9 | Partial unique indexes | 4 indexes, WHERE deleted_at IS NULL |
| NFR-Audit | AuditEventPublisher + contract (Section 6.3) | Mechanism specified |
| NFR-Security | @PreAuthorize + scope filter | Section 6.2 |
| NFR-Performance | @EntityGraph + @Cacheable | Section 7 |

## 10. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Coordinator mutation rights | Stakeholder | Pending (req OQ #1) |
| 2 | Code mutability (currently immutable) | Stakeholder | Pending (req OQ #2) |
| 3 | Elective basket FK vs String | System Design | Pending (req OQ #4) |
| 4 | Historical timetable deletion check | A4-42 team | Blocked on A4-42 |

## 11. Provisional Decisions

| ID | Decision | Reversible? |
|---|---|---|
| PD-1 | Soft-delete | No (hard-delete requires migration + strategy change) |
| PD-2 | Name max: 200 | Yes (ALTER COLUMN) |
| PD-3 | Code max: 20 | Yes (ALTER, may break existing) |
| PD-4 | Code format: `^[A-Za-z0-9_-]+$` | Yes (relax regex) |
| PD-5 | Location max: 500 | Yes (ALTER COLUMN) |
