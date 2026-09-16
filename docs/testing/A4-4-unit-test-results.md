# Unit Test Results — A4-4 Faculty Profile Management

**Story Key:** A4-4
**Generated:** 2026-08-26
**Framework:** JUnit 5 + Mockito
**Status:** Tests written — awaiting execution in IntelliJ

## Results Summary

| Test Class | Tests | Expected Pass | Notes |
|---|---|---|---|
| FacultyServiceTest | 9 | 9 | All assertions verified at code level |
| FacultyCompetencyServiceTest | 5 | 5 | All assertions verified |
| FacultyCampusAssociationServiceTest | 5 | 5 | All assertions verified |
| **Total** | **19** | **19** | Run `mvn test` in IntelliJ to confirm |

## Test Details

### FacultyServiceTest (9 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | create_validRequest_returnsFacultyDto | Happy path with valid department and campus | Faculty created, campus association added |
| 2 | create_duplicateIdentifier_throwsConflict | Identifier already exists | ConflictException thrown |
| 3 | create_invalidDepartment_throwsNotFound | Home department ID doesn't exist | EntityNotFoundException thrown |
| 4 | create_loadMinGreaterThanMax_throwsBusinessRuleViolation | minWeeklyLoad > maxWeeklyLoad | BusinessRuleViolationException thrown |
| 5 | create_invalidDesignation_throwsBusinessRuleViolation | Designation not in allowed list | BusinessRuleViolationException thrown |
| 6 | update_departmentTransfer_setsNewDepartment | Change homeDepartmentId | Department updated on entity |
| 7 | update_designationChange_emitsFacultyDesignationChangedEvent | Designation changed | FacultyDesignationChangedEvent published with old/new values |
| 8 | delete_existingFaculty_softDeletes | Valid faculty ID | deletedAt set, isActive=false |
| 9 | delete_nonExistentFaculty_throwsNotFound | Faculty ID doesn't exist | EntityNotFoundException thrown |

### FacultyCompetencyServiceTest (5 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | addCompetencies_newCompetency_createsLink | New course competency | FacultyCompetency saved |
| 2 | addCompetencies_alreadyActive_skips | Competency already active | No save, no error |
| 3 | addCompetencies_softDeletedExists_reactivates | Soft-deleted competency exists | Reactivated (deletedAt=null, isActive=true) |
| 4 | removeCompetency_existing_softDeletes | Active competency | deletedAt set, isActive=false |
| 5 | removeCompetency_notFound_throwsNotFound | No link exists | EntityNotFoundException thrown |

### FacultyCampusAssociationServiceTest (5 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | addAssociation_newAssociation_createsLink | New campus link | FacultyCampusAssociation saved |
| 2 | addAssociation_alreadyActive_skips | Association already active | No save, no error |
| 3 | removeAssociation_multipleExist_softDeletes | Faculty has 2+ campus associations | Soft-deleted successfully |
| 4 | removeAssociation_lastAssociation_throwsBusinessRuleViolation | Only 1 association remains | BusinessRuleViolationException (must keep at least one) |
| 5 | removeAssociation_notFound_throwsNotFound | No association exists | EntityNotFoundException thrown |

## Issues Found

None identified at code level. Actual runtime verification pending.

## Notes

- Tests written using Mockito (service layer isolation — no database)
- Designation validation verified against allowlist (Professor, Associate Professor, Assistant Professor, Lecturer, Visiting Faculty, Lab Technician)
- FacultyDesignationChangedEvent emission verified via ArgumentCaptor
- Actual pass/fail results require running `mvn test -Dtest=FacultyServiceTest,FacultyCompetencyServiceTest,FacultyCampusAssociationServiceTest`
