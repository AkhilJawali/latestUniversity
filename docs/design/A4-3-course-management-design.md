# Design: Course Management

**Jira Reference:** A4-3
**Source Requirements:** docs/requirements/A4-3-course-management-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — rewrite addressing update path, cross-listing logic, OQ#4, decision interactions)

## 1. Overview

This design implements Course/Subject master data management — full CRUD (including the update path with cycle re-validation, L-T-P consistency checking, and type-change event emission), prerequisite relationships (acyclic directed graph with path-tracking cycle detection), equipment tags (case-sensitive matching — explicit design decision per Assumption 3), and cross-listing across departments (with code-collision prevention in target departments).

## 2. Architecture

```
Controller Layer
    └── CourseController
         │
Service Layer
    ├── CourseService (full CRUD including update, type-change event, LTP-change event)
    ├── CoursePrerequisiteService (add/remove prereqs, cycle detection with path)
    └── CourseCrossListingService (add/remove cross-listings with code-collision check)
         │
Event Publishing
    ├── AuditEventPublisher (same contract as A4-2)
    ├── CourseTypeChangedEvent (consumed by A4-25 registration module when built)
    └── CourseLtpChangedEvent (consumed by A4-11 scheduling module when built)
         │
Repository Layer
    ├── CourseRepository (+ JpaSpecificationExecutor)
    ├── CoursePrerequisiteRepository
    └── CourseDepartmentLinkRepository
         │
Database (schema: utms)
    ├── courses
    ├── course_prerequisites
    └── course_department_links
```

## Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-6 | Cross-listing via junction table `course_department_links`. Course has one owning department (primary) + zero or more additional linked departments. | Cleaner than multi-valued FK. Allows querying "all courses visible to dept X" with a single join. | **Interacts with code uniqueness (see KD-8).** |
| KD-7 | Prerequisite cycle detection via BFS **with parent-tracking** (reconstructs the full cycle path for error messages) | Enables actionable error messages showing the exact cycle. | Performance: O(V+E), negligible for <500 courses. |
| KD-8 | Cross-listing code-collision prevention: when adding a cross-listing to dept Y, reject if dept Y already has an active course with the same code. | Prevents two courses with identical codes appearing in the same department's catalogue. PD-7 scopes uniqueness to owning dept — this extends the check to cross-listed target depts. | Resolves the PD-7 + KD-6 collision. |
| KD-9 | Prerequisite junction rows are physically deleted (not soft-deleted). Course entities themselves are soft-deleted. | A prerequisite link either exists or doesn't — there's no "inactive prerequisite" state. | — |
| KD-10 | Equipment tag matching is **case-sensitive** (explicit decision per req Assumption 3). Tags stored as-entered; matching uses exact string equality. | Simplest correct behavior. If case-insensitive needed later: add LOWER() normalization on write. | Consumers (A4-24 room matching) must use same casing. Document this in integration contract. |
| KD-11 | Type-change and LTP-change emit domain events (not transient warnings). Events persist in the event stream and are consumed by downstream modules when they're built. | A transient HTTP warning is lost if the caller ignores it. A persisted event guarantees downstream modules can react (flag registrations, flag sessions) even asynchronously. | Same Spring ApplicationEvent mechanism as audit events. Within same transaction. |

## Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-6 | Soft-delete for courses (not hard-delete) | Req OQ #3 | Soft-delete | Consistent with A4-2. Historical timetables reference courses. |
| PD-7 | Course code unique within owning department (active records only, partial index) | Req OQ #2 | Per-department | Different depts may have same code (e.g., "101"). Extended by KD-8 for cross-listing targets. |
| PD-8 | Credits: DECIMAL(3,1), allowing half-credits (e.g., 1.5) | Req OQ #8 | Decimal | Some institutions use half-credit courses. |
| PD-9 | Block deletion if course is prerequisite for OTHER ACTIVE courses (not counting soft-deleted dependents) | Req OQ #7 | Block active dependents only | Refined: soft-deleted dependents don't block. Resolves PD-9/PD-14 collision. |
| PD-10 | Type change emits `CourseTypeChangedEvent`. Until A4-25 exists, event published but no consumer. When A4-25 is built, it marks affected registrations as REVIEW_REQUIRED. | Req OQ #5 | Event-based flagging | Cannot auto-invalidate registrations (may disrupt students). Event allows async downstream action. |
| PD-11 | L-T-P update while draft sessions exist: emits `CourseLtpChangedEvent`. Scheduling module (A4-11) can flag affected sessions as STALE_LTP. Until A4-11 exists, no-op. | Req OQ #4 | Event-based flagging | Same pattern as PD-10. Downstream handles the consequence. |
| PD-12 | Name max: 200 chars | Req validation TBD | 200 | Consistent with A4-2. |
| PD-13 | Code max: 20 chars, format `^[A-Za-z0-9_-]+$` | Req validation TBD | Alphanumeric + hyphen/underscore | Consistent with A4-2. |
| PD-14 | Existing prerequisite links to soft-deleted courses are preserved (not auto-removed) | Interaction: soft-delete + prereqs | Preserve | Historical curriculum context. Conflict detection (A4-16 AC 9) can still see the relationship for past semesters. |

## 3. API Design

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/courses` | Create course | ADMIN | FR-1.1 |
| GET | `/api/v1/courses` | List (filter: departmentId, type, equipmentTag, code, crossListedInDeptId) | AUTHENTICATED (scoped) | FR-2.1-2.3 |
| GET | `/api/v1/courses/{id}` | Get by ID (includes prereqs + cross-listings) | AUTHENTICATED (scoped) | FR-2.1 |
| GET | `/api/v1/courses/code/{code}?departmentId={deptId}` | Get by code within department | AUTHENTICATED (scoped) | FR-2.1 |
| PUT | `/api/v1/courses/{id}` | Update (name, LTP, credits, type, equipment_tags) | ADMIN | FR-3.1-3.4 |
| DELETE | `/api/v1/courses/{id}` | Soft-delete | ADMIN | FR-4.1 |
| GET | `/api/v1/courses/{id}/prerequisites` | List prerequisites | AUTHENTICATED | FR-2.3, FR-6.1 |
| GET | `/api/v1/courses/{id}/dependents` | Reverse: who requires this course | AUTHENTICATED | FR-2.4 |
| POST | `/api/v1/courses/{id}/prerequisites` | Add prerequisite | ADMIN | FR-6.1 |
| DELETE | `/api/v1/courses/{id}/prerequisites/{prereqId}` | Remove prerequisite | ADMIN | FR-6.1 |
| GET | `/api/v1/courses/{id}/cross-listings` | List cross-listed departments | AUTHENTICATED | FR-5.2 |
| POST | `/api/v1/courses/{id}/cross-listings` | Add cross-listing to department | ADMIN | FR-5.1 |
| DELETE | `/api/v1/courses/{id}/cross-listings/{deptId}` | Remove cross-listing | ADMIN | FR-5.1 |

**POST /api/v1/courses**
```json
{
  "name": "Data Structures and Algorithms",
  "code": "CS201",
  "departmentId": 1,
  "lectureHours": 3,
  "tutorialHours": 1,
  "practicalHours": 2,
  "credits": 4.0,
  "type": "CORE",
  "equipmentTags": ["projector", "computer_lab"],
  "prerequisiteCourseIds": [5, 8]
}
```

**PUT /api/v1/courses/{id}**
```json
{
  "name": "Data Structures and Algorithms (Updated)",
  "lectureHours": 3,
  "tutorialHours": 0,
  "practicalHours": 3,
  "credits": 4.5,
  "type": "ELECTIVE",
  "equipmentTags": ["projector", "computer_lab", "whiteboard"]
}
```
Note: `code` and `departmentId` NOT in update payload — immutable (consistent with A4-2). Prerequisites managed via sub-resource endpoints.

**Cycle Detection Error (422) — path reconstructed by algorithm (KD-7)**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Adding prerequisite would create circular dependency",
  "path": "/api/v1/courses/10/prerequisites",
  "details": [{"cyclePath": ["CS201 (id:10)", "CS101 (id:5)", "CS050 (id:3)", "CS201 (id:10)"]}]
}
```

**Cross-listing code collision (409) — produced by KD-8 check**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Department 'Electronics' already has an active course with code 'CS201'. Cross-listing would create a code collision.",
  "path": "/api/v1/courses/10/cross-listings"
}
```

**Security scoping:** Coordinator sees courses in own dept + cross-listed TO their dept. Registrar/Admin see all. Filtered results (not 403) for out-of-scope queries — courses are a shared catalogue.

## 4. Data Model

### Migration: V2__create_courses_tables.sql

```sql
CREATE TABLE utms.courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(20) NOT NULL,
    department_id BIGINT NOT NULL,
    lecture_hours INTEGER NOT NULL DEFAULT 0 CHECK (lecture_hours >= 0),
    tutorial_hours INTEGER NOT NULL DEFAULT 0 CHECK (tutorial_hours >= 0),
    practical_hours INTEGER NOT NULL DEFAULT 0 CHECK (practical_hours >= 0),
    credits DECIMAL(3,1) NOT NULL CHECK (credits > 0),
    course_type VARCHAR(20) NOT NULL CHECK (course_type IN ('CORE', 'ELECTIVE', 'AUDIT')),
    equipment_tags TEXT[] DEFAULT '{}',
    is_cross_listed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_courses_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT chk_courses_ltp_positive CHECK (lecture_hours + tutorial_hours + practical_hours > 0)
);
CREATE UNIQUE INDEX uq_courses_department_code ON utms.courses(department_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_department_id ON utms.courses(department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_type ON utms.courses(course_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_equipment_tags ON utms.courses USING GIN(equipment_tags) WHERE deleted_at IS NULL;

CREATE TABLE utms.course_prerequisites (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    prerequisite_course_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT fk_cp_prerequisite FOREIGN KEY (prerequisite_course_id) REFERENCES utms.courses(id),
    CONSTRAINT uq_course_prerequisite UNIQUE (course_id, prerequisite_course_id),
    CONSTRAINT chk_no_self_prerequisite CHECK (course_id != prerequisite_course_id)
);

CREATE TABLE utms.course_department_links (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_cdl_course FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT fk_cdl_department FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_course_department_link UNIQUE (course_id, department_id)
);
```

## 5. Service / Business Logic

### 5.1 CourseService.create(request)
1. Validate department exists (active)
2. Validate code uniqueness within department (active records)
3. Validate L-T-P: each >= 0, sum > 0
4. Validate credits > 0
5. Validate type enum
6. If prerequisites: validate each exists (active), run cycle detection
7. Persist course + prerequisite links
8. Publish audit event

### 5.2 CourseService.update(id, request)

```java
public CourseDto update(Long id, UpdateCourseRequest request) {
    Course course = findActiveByIdOrThrow(id);
    Course previousState = course.snapshot();

    // FR-3.3: Validate L-T-P
    if (request.getLectureHours() < 0 || request.getTutorialHours() < 0 || request.getPracticalHours() < 0)
        throw validationError("L-T-P values must be non-negative");
    if (request.getLectureHours() + request.getTutorialHours() + request.getPracticalHours() == 0)
        throw validationError("At least one of L-T-P must be > 0");

    // Detect changes for events
    boolean ltpChanged = (request.getLectureHours() != course.getLectureHours() ||
                          request.getTutorialHours() != course.getTutorialHours() ||
                          request.getPracticalHours() != course.getPracticalHours());
    boolean typeChanged = !request.getType().equals(course.getCourseType());

    // Apply (code/dept immutable)
    course.setName(request.getName());
    course.setLectureHours(request.getLectureHours());
    course.setTutorialHours(request.getTutorialHours());
    course.setPracticalHours(request.getPracticalHours());
    course.setCredits(request.getCredits());
    course.setCourseType(request.getType());
    course.setEquipmentTags(request.getEquipmentTags());
    course = courseRepository.save(course);

    // KD-11: Domain events within @Transactional
    if (ltpChanged) eventPublisher.publishEvent(new CourseLtpChangedEvent(course.getId(), previousState, course));
    if (typeChanged) eventPublisher.publishEvent(new CourseTypeChangedEvent(course.getId(), previousState.getCourseType(), course.getCourseType()));

    auditEventPublisher.publish(AuditEvent.updated("Course", course.getId(), previousState, course));
    return courseMapper.toDto(course);
}
```

**FR-3.2:** Prerequisites NOT in PUT — managed via sub-resource. Each add runs cycle detection.

### 5.3 CoursePrerequisiteService.addPrerequisite(courseId, prereqId)
1. Both exist, active
2. Prereq not soft-deleted (PD-14)
3. Not self-referential
4. Idempotent (skip if exists)
5. Cycle detection (5.4)
6. Persist, audit

### 5.4 Cycle Detection (KD-7 — BFS with path)

```java
private void validateNoCycle(Long courseId, Long newPrereqId) {
    Map<Long, Long> parentMap = new HashMap<>();
    Deque<Long> queue = new ArrayDeque<>();
    Set<Long> visited = new HashSet<>();
    queue.add(newPrereqId);
    parentMap.put(newPrereqId, courseId);
    while (!queue.isEmpty()) {
        Long current = queue.poll();
        if (current.equals(courseId)) {
            throw new BusinessRuleViolationException("Adding prerequisite would create circular dependency",
                Map.of("cyclePath", reconstructPath(parentMap, courseId, newPrereqId)));
        }
        if (visited.add(current)) {
            for (Long prereq : coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(current)) {
                if (!visited.contains(prereq)) { parentMap.put(prereq, current); queue.add(prereq); }
            }
        }
    }
}
```

Path reconstruction walks parentMap backward producing `["CS201 (id:10)", "CS101 (id:5)", "CS050 (id:3)", "CS201 (id:10)"]`.

### 5.5 CourseCrossListingService

```java
public void addCrossListing(Long courseId, Long targetDeptId) {
    Course course = findActiveCourseOrThrow(courseId);
    Department targetDept = findActiveDeptOrThrow(targetDeptId);
    if (targetDeptId.equals(course.getDepartment().getId()))
        throw new BusinessRuleViolationException("Cannot cross-list to owning department");
    // KD-8: code collision
    if (courseRepository.existsByDepartmentIdAndCodeAndDeletedAtIsNull(targetDeptId, course.getCode()) ||
        linkRepository.existsOtherCourseWithCodeInDepartment(targetDeptId, course.getCode(), courseId))
        throw new ConflictException("Code collision in target department");
    if (linkRepository.existsByCourseIdAndDepartmentId(courseId, targetDeptId)) return; // idempotent
    linkRepository.save(new CourseDepartmentLink(course, targetDept));
    course.setIsCrossListed(true); courseRepository.save(course);
    auditEventPublisher.publish(AuditEvent.created("CourseCrossListing", courseId, Map.of("dept", targetDept.getName())));
}

public void removeCrossListing(Long courseId, Long deptId) {
    CourseDepartmentLink link = linkRepository.findByCourseIdAndDepartmentId(courseId, deptId)
        .orElseThrow(() -> new EntityNotFoundException("Cross-listing not found"));
    linkRepository.delete(link);
    if (linkRepository.countByCourseId(courseId) == 0) {
        Course c = findActiveCourseOrThrow(courseId); c.setIsCrossListed(false); courseRepository.save(c);
    }
    auditEventPublisher.publish(AuditEvent.deleted("CourseCrossListing", link.getId(), link));
}
```

### 5.6 CourseService.delete(id)
1. Active check
2. PD-9: `countByPrerequisiteCourseIdAndCourseDeletedAtIsNull(id)` — only active dependents block
3. Other refs: TODO (sessions, competency, registrations, historical)
4. Blocked → 422; else soft-delete + audit

### 5.7 Deletion Checks

| Reference | Status |
|-----------|--------|
| Prerequisite for ACTIVE courses | Implemented |
| Scheduled sessions | TODO |
| Faculty competency | TODO |
| Elective registrations | TODO |
| Historical timetables | TODO |

### 5.8 Domain Events

```java
public record CourseLtpChangedEvent(Long courseId, Course prev, Course next) {}
public record CourseTypeChangedEvent(Long courseId, String prevType, String newType) {}
```
Consumers (when built): A4-11 flags STALE_LTP sessions, A4-25 flags REVIEW_REQUIRED registrations.

## 6. Cross-cutting Concerns

| Concern | Design |
|---------|--------|
| Errors | 404/409/422/400/403 via GlobalExceptionHandler |
| Security | ADMIN mutations; AUTHENTICATED scoped reads (coordinator sees own dept + cross-listed) |
| Audit | AuditEventPublisher (A4-2 contract) |
| Soft-Delete | Specification WHERE deleted_at IS NULL; partial unique index |

## 7. Non-Functional Design

| NFR | Design |
|-----|--------|
| Performance | GIN index; BFS O(V+E); paginated |
| Audit | AuditEventPublisher |
| Security | @PreAuthorize + Specification scope |

## 8. Testing Strategy

**Unit:** create (happy/dup/invalidDept/ltpZero/prereqNotExists), update (ltpEvent/typeEvent/ltpZero), delete (activeDepBlock/softDeletedDepAllow), cycle (linear/direct/deep/diamond), crossListing (happy/collision/ownDept/idempotent/removeFlag)

**Integration:** POST→201; dup→409; PUT LTP→event; PUT type→event; PUT ltpZero→422; cycle→422+path; prereqNotExists→404; collision→409; DELETE prereq-active→422; DELETE prereq-softDeleted→200; GET tag case-sensitive; GET crossListedInDeptId

## 9. Requirement Traceability

| Req | Element | Verified |
|---|---|---|
| FR-1.1-1.4 | POST + create() | tests |
| FR-2.1 | GET /{id} + /code/{code}?deptId | endpoints |
| FR-2.2 | Specification (5 filters) | tests |
| FR-2.3 | DTO includes prereqs+tags | DTO |
| FR-2.4 | GET /dependents | endpoint |
| FR-3.1 | PUT + update() 5.2 | tests |
| FR-3.2 | Sub-resource prereqs + BFS | tests |
| FR-3.3 | update() LTP validation | test |
| FR-3.4 | CourseTypeChangedEvent (PD-10) | test |
| FR-4.1-4.3 | DELETE + checks 5.7 | tests |
| FR-5.1-5.3 | CrossListingService 5.5 | tests |
| FR-6.1-6.3 | PrerequisiteService + BFS 5.4 | tests |
| HC-CRS-1-7 | DB+service (as detailed above) | tests |
| OQ#4 | PD-11 CourseLtpChangedEvent | test |
| Assumption 3 | KD-10 case-sensitive | test |

## 10. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Coordinator mutation rights | Stakeholder | Pending |
| 2 | Cross-listing across campuses | Academic Affairs | Pending |
| 3 | Equipment tag vocabulary (A4-6 OQ#6) | System Design | Pending |

## 11. Provisional Decisions

| ID | Decision | Reversible? |
|---|---|---|
| PD-6 | Soft-delete | No |
| PD-7 | Code unique per owning dept | Yes |
| PD-8 | Credits DECIMAL(3,1) | Yes |
| PD-9 | Block delete if prereq for active only | Yes |
| PD-10 | Type change → event | Yes |
| PD-11 | LTP change → event | Yes |
| PD-12 | Name max 200 | Yes |
| PD-13 | Code max 20 `^[A-Za-z0-9_-]+$` | Yes |
| PD-14 | Prereq links to deleted preserved | Yes |
