# Unit Test Results — A4-3 Course Management

**Story Key:** A4-3
**Generated:** 2026-08-25
**Framework:** JUnit 5 + Mockito
**Status:** Tests written — awaiting execution in IntelliJ

## Results Summary

| Test Class | Tests | Expected Pass | Notes |
|---|---|---|---|
| CourseServiceTest | 8 | 8 | Covers create, update (events), delete |
| CoursePrerequisiteServiceTest | 6 | 6 | Covers cycle detection, validation |
| CourseCrossListingServiceTest | 5 | 5 | Covers collision check, flag sync |
| **Total** | **19** | **19** | Run mvn test in IntelliJ to confirm |

## Test Details

### CourseServiceTest (8 tests)
- create_validRequest_returnsCourseDto
- create_duplicateCodeSameDept_throwsConflict
- create_invalidDept_throwsEntityNotFoundException
- create_ltpAllZero_throwsBusinessRuleViolation
- update_ltpChanged_emitsCourseLtpChangedEvent
- update_typeChanged_emitsCourseTypeChangedEvent
- delete_activePrereqDependents_throwsBusinessRuleViolation
- delete_onlySoftDeletedDependents_softDeletes

### CoursePrerequisiteServiceTest (6 tests)
- addPrerequisite_validRequest_persists
- addPrerequisite_createsCycle_throwsBusinessRuleViolation
- addPrerequisite_selfReferential_throwsBusinessRuleViolation
- addPrerequisite_softDeletedPrereq_rejected
- addPrerequisite_duplicate_idempotent
- addPrerequisite_prereqNotFound_throwsEntityNotFoundException

### CourseCrossListingServiceTest (5 tests)
- addCrossListing_validRequest_persists
- addCrossListing_codeCollisionInTargetDept_throwsConflict
- addCrossListing_toOwningDept_throwsBusinessRuleViolation
- addCrossListing_duplicate_idempotent
- removeCrossListing_updatesIsCrossListedFlag

## Issues Found

None identified at code level. Actual runtime verification pending.

## Notes

- Cycle detection tested with direct cycle case
- Event emission verified via ApplicationEventPublisher mock
- Actual pass/fail requires running in IntelliJ
- Integration tests planned for Testing subtask (A4-223)
