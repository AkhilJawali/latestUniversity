# Unit Test Results — A4-9 Academic Calendar Management

**Story Key:** A4-9
**Generated:** 2026-08-26
**Framework:** JUnit 5 + Mockito
**Status:** Tests written — awaiting execution in IntelliJ

## Results Summary

| Test Class | Tests | Expected Pass | Notes |
|---|---|---|---|
| AcademicCalendarServiceTest | 8 | 8 | All assertions verified at code level |
| WorkingDayPatternServiceTest | 5 | 5 | All assertions verified |
| CalendarQueryServiceTest | 7 | 7 | All assertions verified |
| **Total** | **20** | **20** | Run `mvn test` in IntelliJ to confirm |

## Test Details

### AcademicCalendarServiceTest (8 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | create_validRequest_returnsCalendarDto | Happy path — valid campus, year, semester, dates | Calendar saved, DTO returned |
| 2 | create_duplicateCampusYearSemester_throwsConflict_KD40 | Same campus+year+semester already exists | ConflictException thrown (KD-40 uniqueness) |
| 3 | create_startDateAfterEndDate_throwsBusinessRule | Semester start > semester end | BusinessRuleViolationException thrown |
| 4 | addHoliday_validRequest_publishesImpactEvent_KD38 | Holiday added within semester bounds | Holiday saved, CalendarImpactEvent emitted (KD-38) |
| 5 | addExamWindow_validRequest_savesSuccessfully | Exam window within semester bounds | ExamWindow saved |
| 6 | addExamWindow_outsideSemesterBounds_throwsBusinessRule | Exam dates outside semester range | BusinessRuleViolationException thrown |
| 7 | addOrientationPeriod_validRequest_savesSuccessfully | Orientation period within semester bounds | OrientationPeriod saved |
| 8 | deleteCalendar_existing_softDeletesCalendarAndChildren | Calendar exists | Calendar + all children soft-deleted |

### WorkingDayPatternServiceTest (5 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | create_validFiveDayPattern_savesSuccessfully | FIVE_DAY pattern for campus | Pattern saved, DTO returned |
| 2 | create_duplicatePatternForCampus_throwsConflict | Campus already has a pattern | ConflictException thrown (one pattern per campus) |
| 3 | create_alternateSaturdayWithoutWorkingSaturdays_throwsBusinessRule | ALTERNATE_SATURDAY with null workingSaturdays | BusinessRuleViolationException (list required) |
| 4 | update_patternChanged_emitsImpactEvent_KD38 | Pattern type changed (SIX_DAY -> FIVE_DAY) | CalendarImpactEvent emitted for affected dates (KD-38) |
| 5 | delete_anyAttempt_throwsBusinessRuleViolation_KD39 | Any delete attempt | BusinessRuleViolationException (pattern required per campus, KD-39) |

### CalendarQueryServiceTest (7 tests)

| # | Test Method | Scenario | Expected Result |
|---|-------------|----------|-----------------|
| 1 | isWorkingDay_noPatternConfigured_throwsBusinessRule_KD39 | No pattern for campus | BusinessRuleViolationException (fail-loudly, KD-39) |
| 2 | isWorkingDay_dateIsHoliday_returnsFalse | Monday with holiday declared | false (holiday overrides pattern) |
| 3 | isWorkingDay_dateInExamWindow_returnsFalse | Working day within exam window | false (exam window blocks scheduling) |
| 4 | isWorkingDay_dateInOrientationPeriod_returnsFalse | Working day within orientation period | false (orientation blocks scheduling) |
| 5 | isWorkingDay_fiveDayPattern_weekdayWithNoBlockers_returnsTrue | Wednesday, FIVE_DAY pattern, no blockers | true |
| 6 | isWorkingDay_fiveDayPattern_saturdayIsNotWorking | Saturday, FIVE_DAY pattern | false |
| 7 | isWorkingDay_sixDayPattern_saturdayIsWorking | Saturday, SIX_DAY pattern | true |

## Issues Found

None identified at code level. Actual runtime verification pending.

## Notes

- Tests written using Mockito (service layer isolation — no database)
- KD-38: Calendar impact detection verified — adding holidays or changing patterns emits CalendarImpactEvent
- KD-39: Pattern deletion blocked unconditionally; isWorkingDay fails loudly if no pattern configured
- KD-40: Calendar uniqueness per (campus + academic year + semester) enforced
- CalendarQueryService checks 4 layers: pattern -> holiday -> exam window -> orientation period
- Actual pass/fail results require running `mvn test -Dtest=AcademicCalendarServiceTest,WorkingDayPatternServiceTest,CalendarQueryServiceTest`
