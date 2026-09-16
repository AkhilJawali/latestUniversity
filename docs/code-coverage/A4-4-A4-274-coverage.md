# Code Coverage Report — A4-4 Faculty Profile Management

**Story Key:** A4-4
**Subtask Key:** A4-274
**Generated:** 2026-08-25

## Coverage Summary

| Package | Classes Covered | Methods Tested | Estimated Coverage |
|---|---|---|---|
| com.utms.masterdata.faculty | FacultyService | 6/7 | ~85% |
| com.utms.masterdata.faculty | FacultyCompetencyService | 2/2 | ~90% |
| com.utms.masterdata.faculty | FacultyCampusAssociationService | 2/2 | ~85% |

## Test Count: 19 total (9 + 5 + 5)

## Covered Scenarios

- FacultyService: create (happy/dupId/invalidDept/loadMinGtMax), update (deptTransfer/designationEvent), delete (noRefs/hasRefs), findById (notFound)
- FacultyCompetencyService: add (happy/courseNotFound/duplicate), remove (happy/notFound)
- FacultyCampusAssociationService: add (happy/campusNotFound/duplicate), remove (happy/lastBlocked)

## Notes
- 19 unit tests, service layer isolation with Mockito
- Audit events published (verified via mock)
- Actual JaCoCo report via mvn test jacoco:report
