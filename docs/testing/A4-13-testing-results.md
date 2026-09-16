# Testing Results — Fortnightly and Alternate-Week Scheduling Patterns

- **Story**: A4-13 — Fortnightly and Alternate-Week Scheduling Patterns
- **Testing Subtask**: A4-63 — Testing — Fortnightly and Alternate-Week Scheduling Patterns
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `com.utms.scheduling.engine` (recurrence services and resolvers)
- **Build**: Maven `test` — JDK 21 (Amazon Corretto 21.0.8), Spring Boot 3.3.2

---

## 1. Scope

Validation of fortnightly / alternate-week recurrence: configuring non-weekly
patterns, alternating sub-groups (Group A week 1 / Group B week 2) in the same
slot, conflict checks only on occurring weeks, and occurrence-date enumeration
for display/feed export.

---

## 2. Acceptance Criteria → Test Mapping

| AC | Criterion | Test(s) | Result |
|----|-----------|---------|--------|
| AC1 | Fortnightly/alternate-week pattern repeats on that pattern, not weekly | `WeekParityResolverTest` (5); `SessionRecurrenceServiceTest` (set/revert pattern) | PASS |
| AC2 | Group A week 1 / Group B week 2 in same slot without conflict | `RecurrenceOverlapEvaluatorTest` (4) — alternating parity does not overlap | PASS |
| AC3 | Conflict detection only on weeks the session actually occurs | `RecurrenceOverlapEvaluatorTest` — overlap only when weeks coincide | PASS |
| AC4 | Recurrence pattern clearly indicated on the timetable | `SessionRecurrenceServiceTest` — occurrence dates / pattern exposed | PASS |
| AC5 | Calendar feed shows events only on correct alternating weeks | `SessionRecurrenceServiceTest` — occurrence-date enumeration (consumed by feed) | PASS |

---

## 3. Test Execution Summary

Command: `mvn -Dtest="com.utms.scheduling.**" test`

- `WeekParityResolverTest` (5), `RecurrenceOverlapEvaluatorTest` (4),
  `SessionRecurrenceServiceTest` (12) — all PASS.
- Whole scheduling suite: **78 tests, 0 failures, 0 errors — BUILD SUCCESS.**

---

## 4. Issues Found and Fixed

None. Recurrence resolution, alternating-parity overlap, and occurrence-date
enumeration behave per spec. No regression.

---

## 5. Gaps / Follow-up

- **Calendar feed export (AC5)** occurrence generation is unit-tested; the end-to-end
  iCal feed that consumes it is a separate concern (calendar-export module) and is
  not exercised here.
- **Integration tests (Testcontainers)** not run (no Docker) — follow-up in CI.

---

## 6. Verdict

All five acceptance criteria pass at the unit/service level; scheduling suite green
(78/78). No issues found. Testing subtask A4-63 is ready for lead approval.
