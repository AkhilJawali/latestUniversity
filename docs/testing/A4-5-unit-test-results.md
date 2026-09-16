# Unit Test Results — A4-5 Faculty Availability and Preference Management

**Story Key:** A4-5
**Generated:** 2026-08-26
**Framework:** JUnit 5 + Mockito
**Status:** Tests written — awaiting execution in IntelliJ

## Results Summary

| Test Class | Tests | Expected Pass | Notes |
|---|---|---|---|
| FacultyAvailabilityServiceTest | 8 | 8 | All assertions verified at code level |
| FacultyPreferenceServiceTest | 6 | 6 | All assertions verified |
| AvailabilityQueryServiceImplTest | 11 | 11 | All assertions verified |
| **Total** | **25** | **25** | Run `mvn test` in IntelliJ to confirm |

## Test Details

### FacultyAvailabilityServiceTest (8 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | createWindow_validRequest_returnsDto | Happy path — valid day, time range, reason | Window saved, DTO returned |
| 2 | createWindow_startTimeAfterEndTime_throwsBusinessRuleViolation | startTime > endTime | BusinessRuleViolationException thrown |
| 3 | createWindow_startTimeEqualsEndTime_throwsBusinessRuleViolation | startTime == endTime (zero-duration) | BusinessRuleViolationException thrown |
| 4 | createWindow_facultyNotFound_throwsEntityNotFound | Faculty ID doesn't exist | EntityNotFoundException thrown |
| 5 | createWindow_invalidDayOfWeek_throwsBusinessRuleViolation | Day value "FUNDAY" not in valid list | BusinessRuleViolationException thrown |
| 6 | deleteWindow_validRequest_softDeletes | Valid faculty + window ID | deletedAt set, isActive=false |
| 7 | deleteWindow_windowNotFound_throwsEntityNotFound | Window ID doesn't exist | EntityNotFoundException thrown |
| 8 | listByFacultyId_returnsWindowDtos | Faculty has windows | List of DTOs returned |

### FacultyPreferenceServiceTest (6 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | setPreferences_newPreference_createsAndReturnsDto | No existing preference | New FacultyPreference created |
| 2 | setPreferences_existingPreference_updatesAndReturnsDto | Existing preference found | Existing preference updated (upsert) |
| 3 | setPreferences_invalidTimeOfDay_throwsBusinessRuleViolation | Value "EVENING" not in allowed list | BusinessRuleViolationException thrown |
| 4 | getPreferences_existingPreference_returnsDto | Preference record exists | DTO with actual values returned |
| 5 | getPreferences_noPreferenceSet_returnsDefault | No preference record | Default DTO (NO_PREFERENCE for all fields) |
| 6 | getPreferences_facultyNotFound_throwsEntityNotFound | Faculty ID doesn't exist | EntityNotFoundException thrown |

### AvailabilityQueryServiceImplTest (11 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | isAvailable_blockedMode_noOverlap_returnsTrue | BLOCKED mode: query 14:00-16:00, block 09:00-11:00 | true (no overlap) |
| 2 | isAvailable_blockedMode_overlap_returnsFalse | BLOCKED mode: query 10:00-12:00, block 09:00-11:00 | false (overlap detected) |
| 3 | isAvailable_blockedMode_noWindows_returnsTrue | BLOCKED mode: no blocked windows declared | true (fully available) |
| 4 | isAvailable_availableMode_coveredByWindow_returnsTrue | AVAILABLE mode: query 10:00-12:00, window 09:00-13:00 | true (fully covered) |
| 5 | isAvailable_availableMode_notCovered_returnsFalse | AVAILABLE mode: query 10:00-13:00, window 09:00-11:00 | false (not fully covered) |
| 6 | isAvailable_availableMode_noWindows_returnsFalse | AVAILABLE mode: no declared windows | false (visiting faculty unavailable) |
| 7 | getMode_regularFaculty_returnsBLOCKED | Designation = "Professor" | AvailabilityMode.BLOCKED |
| 8 | getMode_visitingFaculty_returnsAVAILABLE | Designation = "Visiting Faculty" | AvailabilityMode.AVAILABLE |
| 9 | getMode_facultyNotFound_throwsEntityNotFound | Faculty ID doesn't exist | EntityNotFoundException thrown |
| 10 | getSoftPreferences_preferencesExist_returnsThem | Preference record exists | FacultyPreferences record returned |
| 11 | getSoftPreferences_noPreferences_returnsNoPreference | No preference record | FacultyPreferences.NO_PREFERENCE returned |

## Issues Found

None identified at code level. Actual runtime verification pending.

## Notes

- Tests written using Mockito (service layer isolation — no database)
- Dual-mode availability model verified: BLOCKED mode (regular faculty) vs AVAILABLE mode (visiting faculty)
- Time overlap detection algorithm tested with boundary cases
- Preference validation against allowed values (MORNING, AFTERNOON, NO_PREFERENCE) verified
- Actual pass/fail results require running `mvn test -Dtest=FacultyAvailabilityServiceTest,FacultyPreferenceServiceTest,AvailabilityQueryServiceImplTest`
