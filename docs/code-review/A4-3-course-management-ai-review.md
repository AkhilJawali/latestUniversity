# Course Management Module — Semantic Code Review

This change implements full CRUD for courses, prerequisite graph management with cycle detection, and cross-department listing support. The approach is clean layered architecture: controller handles HTTP, service owns transactions and business logic, repository is data access. Domain events are emitted for LTP and type changes, prerequisite cycles are prevented via BFS, and cross-listing checks for code collisions in the target department before persisting.

Watch for: no input sanitization on `equipmentTags` list (stored as comma-joined string — a tag containing a comma corrupts the data model) (confirmed); prerequisite cycle detection does an unbounded number of queries in the BFS loop without depth guard (confirmed); missing `GET` endpoint for cross-listings (confirmed); no `@Size` constraint on `equipmentTags` or `prerequisiteCourseIds` lists allowing arbitrarily large payloads (confirmed).

**Verdict**: NEEDS_CHANGES

---

## High-level view

The service layer is well-structured with proper transaction boundaries, and the mapper correctly ignores parent associations as required by the MapStruct standards. The controller response envelope uses `Map<String, Object>` throughout — functional but loses type safety and diverges from the API standards (`PagedResponse<T>` pattern).

The prerequisite cycle detection is algorithmically correct for acyclic graph enforcement but issues one database query per BFS node, making it O(V) queries in the worst case for a deep prerequisite chain with no ceiling. The cross-listing code collision query is sound — it correctly excludes the course itself and checks only non-deleted courses in the target department.

Input validation on the request DTOs is solid for scalar fields (regex on code, min/max on LTP, pattern on courseType), but the `equipmentTags` list has no item-level validation and no size cap — an attacker could send thousands of entries or entries containing the comma delimiter used by `StringListConverter`, corrupting storage.

Test coverage is good for the happy path and primary error cases but has gaps around the update flow (no test for update of a non-existent course) and the cycle detection edge cases (transitive chains longer than 2).

---

<details>
<summary>Issues (9)</summary>

1. **Equipment tag comma injection** — Tags containing commas corrupt the comma-separated storage format. Add `@Pattern(regexp = "^[^,]+$")` on each tag element or reject commas, and add `@Size(max = 50)` on the list itself.
2. **Unbounded BFS queries in cycle detection** — Deep prerequisite chains issue one query per node with no depth limit. Add a depth guard (e.g., `if (visited.size() > 20) break;`) to fail fast on unexpectedly deep graphs.
3. **Missing GET endpoint for cross-listings** — POST and DELETE exist but no way to retrieve which departments a course is cross-listed to. Add `GET /{id}/cross-listings` returning department IDs/names.
4. **No size constraint on list fields** — `equipmentTags` and `prerequisiteCourseIds` in `CreateCourseRequest` accept unbounded lists. Add `@Size(max = 50)` for tags, `@Size(max = 20)` for prerequisite IDs to prevent payload abuse.
5. **Duplicate prerequisite IDs on create** — `CourseService.create` iterates `prerequisiteCourseIds` without deduplication. Sending `[2, 2]` attempts a double insert, caught by the duplicate-check in `CoursePrerequisiteService` with a confusing error message mid-loop. Deduplicate the list before iteration.
6. **No DB-level unique constraint on prerequisite pair** — The `course_prerequisites` table relies solely on the service-layer check for `(course_id, prerequisite_course_id)` uniqueness. A concurrent request could bypass the check. Add a composite unique index at the database level.
7. **Cross-listing collision scope is limited to natively-owned courses** — The `existsOtherCourseWithCodeInDepartment` query checks courses whose `department_id` matches the target, but does not check if another cross-listed course from a third department already brought the same code into the target. Clarify the intended semantics; if "visible code" matters, extend the check to include `CourseDepartmentLink` entries.
8. **Controller returns raw `Map<String, Object>` instead of typed response** — Diverges from the `PagedResponse<T>` / typed-DTO pattern in API standards. Creates inconsistency and loses compile-time safety.
9. **Missing tests for edge cases** — No test covers: update of non-existent course, update with LTP all-zero, prerequisite duplicate-add path, transitive cycle depth > 2 (A→B→C→A), or cross-listing add when course doesn't exist.

</details>

<details>
<summary>Details</summary>

## Equipment tag storage and validation gap

The `StringListConverter` joins tags with a comma delimiter and splits on comma when reading back. If a user submits a tag like `"projector,whiteboard"` as a single entry, it will be stored as one string segment but deserialized as two separate tags on read. This is a data corruption vector — round-trip integrity (store N tags, read back N tags) is not guaranteed.

```java
// StringListConverter.java
public String convertToDatabaseColumn(List<String> attribute) {
    return String.join(DELIMITER, attribute);  // comma
}
```

Neither `CreateCourseRequest` nor `UpdateCourseRequest` validates individual tag content. The fix: add element-level validation. Jakarta Bean Validation supports `List<@Pattern(...) String>` with `@Valid`:

```java
@Size(max = 50, message = "Maximum 50 equipment tags allowed")
private List<@Pattern(regexp = "^[A-Za-z0-9_ -]+$",
    message = "Equipment tags must be alphanumeric") String> equipmentTags;
```

Additionally, the entity column is limited to 2000 characters (`@Column(length = 2000)`), so with no item count limit a large payload could hit silent truncation at the Postgres layer (VARCHAR overflow raises an error, not truncation — but length validation should happen at the application layer for a clear error message).

## Prerequisite cycle detection — correctness and performance

The BFS algorithm is correct: starting from `prerequisiteId`, it walks existing prerequisites. If it reaches `courseId`, a cycle would be formed. This correctly handles direct cycles (A→B→A) and transitive cycles (A→B→C→A).

However, each BFS level issues a separate `findPrerequisiteIdsByCourseId` query. For a prerequisite chain of depth D, this means D database round-trips. There is no depth guard:

```java
while (!queue.isEmpty()) {
    Long current = queue.poll();
    List<Long> prereqs = coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(current);
    // ... no depth counter, no max iterations
}
```

In real academic data, prerequisite depth rarely exceeds 5-6. But there's no programmatic guarantee — a misconfigured import could create deep chains. Adding `if (visited.size() > MAX_PREREQ_DEPTH) throw new BusinessRuleViolationException("Prerequisite chain exceeds maximum depth");` with a constant of 20 would make this defensive without affecting real-world use.

The path reconstruction logic is correct — it traces through `parentMap` from the node where the cycle is detected back to `prerequisiteId`, producing a readable cycle path in the error message.

## No database-level unique constraint on prerequisite pair

The service checks for duplicates via `findByCourseIdAndPrerequisiteCourseId`, but there's no composite unique index on `(course_id, prerequisite_course_id)` in the entity or migration. Under concurrent requests, two threads could pass the check simultaneously and both insert. The fix is a `@Table(uniqueConstraints = @UniqueConstraint(...))` on the entity plus a corresponding migration:

```sql
ALTER TABLE utms.course_prerequisites
ADD CONSTRAINT uq_cp_course_prerequisite UNIQUE (course_id, prerequisite_course_id);
```

## Cross-listing code collision detection scope

The `existsOtherCourseWithCodeInDepartment` query:

```java
@Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Course c " +
       "WHERE c.code = :code AND c.department.id = :departmentId " +
       "AND c.id != :excludeCourseId AND c.deletedAt IS NULL")
```

This checks courses whose owning department is the target. It does NOT check whether another `CourseDepartmentLink` already introduced the same code into the target department from a third department. Example scenario:

1. Course "CS201" owned by Dept A is cross-listed to Dept B.
2. Course "CS201" owned by Dept C is cross-listed to Dept B.
3. The current query would NOT catch this — both courses have `department.id != B`.

Whether this is a bug depends on the business rule: is "code uniqueness" scoped to "visible in department" (including cross-listings) or "owned by department"? The current implementation covers "owned by department" only. If the broader scope is intended, the check needs to also query `CourseDepartmentLink` entries for the target department.

## Missing GET endpoint for cross-listings

The controller exposes `POST /{id}/cross-listings/{departmentId}` and `DELETE /{id}/cross-listings/{departmentId}` but no `GET /{id}/cross-listings` to retrieve which departments a course is currently cross-listed to. The frontend has no read path to display cross-listing state. The fix is a simple endpoint returning department IDs/DTOs from `CourseDepartmentLinkRepository.findByCourseId(courseId)`.

## Event emission on update

The LTP and type change detection pattern captures old values before `updateEntity` mutates the entity, then compares afterward. Events are published within the `@Transactional` boundary via `ApplicationEventPublisher`. Spring's default behavior is synchronous dispatch within the transaction — so if the transaction rolls back, the listener logic (if transactional itself) will also roll back. This is correct behavior for now. Future async listeners would need `@TransactionalEventListener(phase = AFTER_COMMIT)` to avoid processing events for rolled-back transactions.

## Test coverage observations

Covered well:
- Create happy path, duplicate code, invalid department, LTP-all-zero
- Update with LTP change and type change event verification
- Delete with active dependents blocked, delete with only soft-deleted dependents allowed
- Prerequisite: valid add, self-reference, direct cycle, soft-deleted course
- Cross-listing: valid add, code collision, owning department rejection, remove last/not-last link flag sync

Not tested:
- `findById` for a non-existent course (trivial but missing)
- `findAll` pagination behavior
- Update with LTP all-zero (should throw `BusinessRuleViolationException`)
- Update of a non-existent course (should throw `EntityNotFoundException`)
- Prerequisite duplicate-add (the "already exists" path in `addPrerequisite`)
- Cycle detection with depth > 2 (A→B→C→A pattern) to confirm BFS traversal works transitively
- Cross-listing add when course itself doesn't exist (entity-not-found path)
- Cross-listing add when target department doesn't exist
- `getPrerequisiteIds` return value correctness

</details>

<details>
<summary>File map</summary>

| File | Role |
|------|------|
| `CourseController.java` | REST endpoints for CRUD, prerequisites sub-resource, cross-listings sub-resource |
| `CourseService.java` | Business logic for create/update/delete with event emission and LTP validation |
| `Course.java` | JPA entity with LTP, credits, type, equipment tags (StringListConverter), cross-listed flag |
| `CourseDto.java` | Response DTO including department info and audit timestamps |
| `CourseMapper.java` | MapStruct mapper using BaseMapperConfig, ignores department association |
| `CreateCourseRequest.java` | Request DTO with Jakarta validation (code pattern, LTP min, courseType enum) |
| `UpdateCourseRequest.java` | Request DTO for mutable fields (no code/dept — immutable) |
| `CourseRepository.java` | Spring Data JPA with JpaSpecificationExecutor |
| `CoursePrerequisiteService.java` | Prerequisite add/remove with BFS cycle detection and path reconstruction |
| `CoursePrerequisite.java` | Junction entity (course_id, prerequisite_course_id) |
| `CoursePrerequisiteRepository.java` | Prerequisite queries including count of active dependents |
| `CourseCrossListingService.java` | Cross-listing add/remove with code collision check and isCrossListed flag sync |
| `CourseDepartmentLink.java` | Junction entity (course_id, department_id) |
| `CourseDepartmentLinkRepository.java` | Link queries including code collision detection via JPQL |
| `CourseLtpChangedEvent.java` | Domain event record for LTP changes |
| `CourseTypeChangedEvent.java` | Domain event record for course type changes |
| `CourseServiceTest.java` | 8 unit tests covering create/update/delete paths |
| `CoursePrerequisiteServiceTest.java` | 6 unit tests covering add/remove/cycle/self-ref |
| `CourseCrossListingServiceTest.java` | 5 unit tests covering add/remove/collision/owning-dept |

</details>
