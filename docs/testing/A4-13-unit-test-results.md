# Unit Test Results — A4-13 Fortnightly and Alternate-Week Scheduling Patterns

**Story:** A4-13
**Unit Test Subtask:** A4-355
**Date:** 2026-09-01
**Framework:** JUnit 5 + Mockito (per testing standards)
**Command:** `mvn -Dtest="WeekParityResolverTest,RecurrenceOverlapEvaluatorTest,SessionRecurrenceServiceTest" test`
**Build:** Java 21 (LTS) · Maven 3.9.9

## Summary

| Metric | Result |
|---|---|
| Test classes | 3 |
| Tests run | 18 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Result | **BUILD SUCCESS** |

## Test Classes and Scenarios

### WeekParityResolverTest (5 tests) — design §5.1, KD-61 / PD-81
- `resolve_semesterStartWeek_isWeekA` — first teaching week = WEEK_A
- `resolve_oneWeekAfterStart_isWeekB` — alternation to WEEK_B
- `resolve_twoWeeksAfterStart_isWeekA` — alternation back to WEEK_A
- `resolve_midWeekDate_resolvesByItsMonday` — a mid-week date resolves by its Monday (calendar-week index)
- `resolve_semesterStartMidWeek_firstWeekStillWeekA` — semester starting mid-week: whole first calendar week is WEEK_A

### RecurrenceOverlapEvaluatorTest (4 tests) — design §5.2, HC-FN-1 / HC-FN-2
- `everCoOccur_bothWeekly_true` — two weekly sessions always co-occur
- `everCoOccur_weeklyVsFortnightly_true_HCFN2` — weekly vs fortnightly overlaps (both orderings)
- `everCoOccur_fortnightlyOppositeGroups_false_HCFN1` — opposite groups never conflict (alternate-week non-conflict)
- `everCoOccur_fortnightlySameGroup_true_HCFN2` — same group co-occurs

### SessionRecurrenceServiceTest (9 tests) — design §5.3–5.4, HC-FN-4 / PD-82
- `setRecurrence_weeklyWithWeekGroup_throwsBusinessRule` — HC-FN-4 (WEEKLY must not carry a week group) → 422
- `setRecurrence_fortnightlyWithoutWeekGroup_throwsBusinessRule` — HC-FN-4 (FORTNIGHTLY requires a group) → 422
- `setRecurrence_sessionNotFound_throwsNotFound` — 404 on missing session
- `setRecurrence_validFortnightly_persistsAndAudits` — persists pattern and publishes audit event
- `revertToWeekly_clearsWeekGroup` — KD-64 revert clears the week group
- `getOccurrenceDates_fortnightlyWeekA_returnsOnlyWeekAMondays` — parity filtering (only WEEK_A occurrences)
- `getOccurrenceDates_holidayOccurrence_isSkipped` — PD-82 (holiday occurrence skipped, no shift)
- `getOccurrenceDates_weekly_returnsAllWorkingOccurrences` — FR-6.2 (weekly feed unchanged)
- `getOccurrenceDates_noCalendarAnchor_throwsBusinessRule` — 422 CALENDAR_ANCHOR when no calendar

## Requirement / Constraint Traceability

| Item | Covering test(s) |
|---|---|
| FR-1.5 / KD-64 (revert, day/slot untouched) | revertToWeekly_clearsWeekGroup |
| FR-2 / KD-61 / PD-81 (week parity) | all WeekParityResolverTest |
| FR-3 / HC-FN-1 / HC-FN-2 (non-conflict rule) | all RecurrenceOverlapEvaluatorTest |
| FR-4.2 / PD-82 (occurring-weeks only, holiday skip) | getOccurrenceDates_holidayOccurrence_isSkipped, _fortnightlyWeekA_ |
| FR-6.2 (weekly feed unchanged) | getOccurrenceDates_weekly_returnsAllWorkingOccurrences |
| HC-FN-4 (validation) | setRecurrence_weeklyWithWeekGroup_, _fortnightlyWithoutWeekGroup_ |
| Audit NFR | setRecurrence_validFortnightly_persistsAndAudits |
| CALENDAR_ANCHOR 422 | getOccurrenceDates_noCalendarAnchor_throwsBusinessRule |

## Notes

- Pre-existing compilation defects in inherited A4-11/A4-12 code were blocking the module build and test build; fixed under user-approved scope: `SchedulingInput` lookup helpers, two "effectively final" lambda captures (`CSPState`, `HardConstraintValidator`), and `SessionType.LAB` -> `PRACTICAL` in `InfeasibilityCollectorTest`. These are flagged for the A4-11/A4-12 owner.
- Controller and request-DTO layers are validated by integration tests (design §11, Testcontainers), not unit tests.
