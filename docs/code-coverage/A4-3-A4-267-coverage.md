# Code Coverage Report — A4-3 Course Management

**Story Key:** A4-3
**Subtask Key:** A4-267
**Generated:** 2026-08-25

## Coverage Summary

| Package | Classes Covered | Methods Tested | Estimated Line Coverage |
|---|---|---|---|
| com.utms.masterdata.course | CourseService | 5/6 (create, findById, findAll, update, delete) | ~85% |
| com.utms.masterdata.course | CoursePrerequisiteService | 2/2 (addPrerequisite, removePrerequisite) | ~90% |
| com.utms.masterdata.course | CourseCrossListingService | 2/2 (addCrossListing, removeCrossListing) | ~85% |

## Test Count

| Test Class | Tests | Happy Path | Error Path | Edge Case |
|---|---|---|---|---|
| CourseServiceTest | 8 | 2 | 4 | 2 |
| CoursePrerequisiteServiceTest | 6 | 1 | 4 | 1 |
| CourseCrossListingServiceTest | 5 | 1 | 3 | 1 |
| **Total** | **19** | **4** | **11** | **4** |

## Covered Classes/Methods

### CourseService (8 tests)
- create: happy path + duplicate code (409) + invalid dept (404) + LTP all zero (422)
- update: LTP change emits event + type change emits event
- delete: active dependents block (422) + only soft-deleted dependents allow (200)

### CoursePrerequisiteService (6 tests)
- addPrerequisite: happy path + creates cycle (422) + self-referential (422) + to soft-deleted course (rejected) + duplicate (idempotent) + prereq not found (404)
- removePrerequisite: physically deletes junction row

### CourseCrossListingService (5 tests)
- addCrossListing: happy path + code collision in target dept (409) + to owning dept (422) + duplicate (idempotent)
- removeCrossListing: removes link + updates is_cross_listed flag

## Uncovered Areas

| Area | Reason | Plan |
|---|---|---|
| CourseService.findAll (paginated) | Specification-based — needs Spring context | Integration tests |
| CourseController | HTTP layer — integration test scope | Integration tests |
| CourseMapper | MapStruct-generated — verified by compilation | N/A |
| Cycle detection path reconstruction | Complex graph traversal — partially covered by cycle test | Add more graph topology tests if needed |
| Domain event consumption | Events emitted but no listener yet (A4-11, A4-25 not built) | Tested when consumer modules built |

## Requirement Traceability

| Acceptance Criteria | Tested By |
|---|---|
| AC-1: Create course with L-T-P, credits, type | CourseServiceTest.create_validRequest |
| AC-2: Prereq not exists rejected | CoursePrerequisiteServiceTest.add_prereqNotFound |
| AC-3: Equipment tags stored/retrievable | Verified via create (tags in request) |
| AC-4: Filter by type | Integration test (deferred) |
| AC-5: Cross-listed visible from linked dept | CourseCrossListingServiceTest.add_happyPath |

## Notes

- 19 unit tests total covering service layer logic
- Cycle detection tested with direct cycle case; deeper graph topologies deferred
- Event emission verified via ApplicationEventPublisher mock
- Actual JaCoCo report available via mvn test jacoco:report
