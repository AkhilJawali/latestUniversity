# Code Coverage Report — A4-5 Faculty Availability and Preference Management

## Summary

| Metric | Value |
|--------|-------|
| **Story Key** | A4-5 |
| **Subtask Key** | A4-281 |
| **Test Classes** | 3 |
| **Total Tests** | 25 |
| **Tests Passed** | 25 |
| **Tests Failed** | 0 |
| **Estimated Coverage** | ~88% |

## Covered Classes and Methods

### FacultyAvailabilityService (8 tests)
| Method | Covered Scenarios |
|--------|-------------------|
| `createWindow` | Valid request, start >= end rejected, start == end rejected, invalid day of week, faculty not found |
| `deleteWindow` | Valid soft-delete (sets deletedAt + isActive=false), window not found |
| `listByFacultyId` | Returns DTO list |

### FacultyPreferenceService (6 tests)
| Method | Covered Scenarios |
|--------|-------------------|
| `setPreferences` | New preference (create), existing preference (update/upsert), invalid time of day |
| `getPreferences` | Existing preference returns DTO, no preference returns default (NO_PREFERENCE), faculty not found |

### AvailabilityQueryServiceImpl (11 tests)
| Method | Covered Scenarios |
|--------|-------------------|
| `isAvailable` | BLOCKED mode: no overlap (true), overlap (false), no windows (true). AVAILABLE mode: covered (true), not covered (false), no windows (false) |
| `getMode` | Regular faculty (BLOCKED), visiting faculty (AVAILABLE), faculty not found |
| `getSoftPreferences` | Preferences exist (returns them), no preferences (returns NO_PREFERENCE) |

## Uncovered Areas

| Class | Gap | Plan |
|-------|-----|------|
| `FacultyAvailabilityController` | Not unit-tested (integration test scope) | Will be covered by integration tests in Phase 20 |
| `FacultyDesignationChangedEventListener` | Not unit-tested (event listener) | Will be covered by integration tests verifying event flow |
| `FacultyAvailabilityService.updateWindow` | Not explicitly tested | Low risk — same validation as createWindow; would add in next iteration |
| `AvailabilityQueryServiceImpl.getHardBlockedSlots` | Not independently tested | Logic tested via isAvailable; direct test recommended for future |

## Requirement Traceability

| Requirement | Covered By |
|-------------|------------|
| Faculty availability window CRUD | FacultyAvailabilityServiceTest |
| Time validation (start < end) | `createWindow_startTimeAfterEndTime_throwsBusinessRuleViolation`, `createWindow_startTimeEqualsEndTime_throwsBusinessRuleViolation` |
| Soft-delete semantics | `deleteWindow_validRequest_softDeletes` |
| Preference upsert | `setPreferences_newPreference_createsAndReturnsDto`, `setPreferences_existingPreference_updatesAndReturnsDto` |
| Default NO_PREFERENCE | `getPreferences_noPreferenceSet_returnsDefault` |
| BLOCKED mode availability logic | `isAvailable_blockedMode_*` (3 tests) |
| AVAILABLE mode availability logic | `isAvailable_availableMode_*` (3 tests) |
| Mode resolution from designation | `getMode_regularFaculty_returnsBLOCKED`, `getMode_visitingFaculty_returnsAVAILABLE` |

## Test Execution Evidence

```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

All tests executed via: `mvn clean test -Dtest="FacultyAvailabilityServiceTest,FacultyPreferenceServiceTest,AvailabilityQueryServiceImplTest"`
