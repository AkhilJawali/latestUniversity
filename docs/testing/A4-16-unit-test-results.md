# Unit Test Results — Real-Time Conflict Detection Engine (A4-16)

- **Story**: A4-16 — Real-Time Conflict Detection Engine
- **Unit Test Subtask**: A4-408
- **Date**: 2026-09-04
- **Module**: `com.utms.scheduling.conflict`
- **Framework**: JUnit 5 + Mockito + AssertJ; Maven `test` on JDK 21 (Corretto 21.0.8)

---

## 1. Summary

New unit tests for the real-time conflict detection engine: **15 tests, 0 failures,
0 errors**. Full project suite re-run green: **248 tests, 0 failures, 0 errors —
BUILD SUCCESS** (no regression).

| Test class | Tests | Focus |
|------------|-------|-------|
| `PlacementRuleCheckerTest` | 10 | Rule logic (AC1–AC7) + recurrence gate (KD-64) + AC5 workload |
| `ConflictDetectionServiceTest` | 5 | Draft validation (AC8), no-conflict (AC6), full-draft aggregate (AC9), propagation |

---

## 2. Acceptance Criteria → Test Mapping

| AC | Criterion | Test | Result |
|----|-----------|------|--------|
| AC1 | Faculty double-booking detected | `check_facultyAlreadyBookedInSlot_returnsFacultyDoubleBooking` | PASS |
| AC2 | Room double-booking detected | `check_roomAlreadyBookedInSlot_returnsRoomDoubleBooking` | PASS |
| AC3 | Batch clash detected (batch/section) | `check_batchAlreadyBookedInSlot_returnsBatchClash` | PASS |
| AC4 | Room capacity < batch strength | `check_roomCapacityLessThanBatchStrength_returnsCapacityConflict` | PASS |
| AC5 | Consecutive hours — distinct type | `check_consecutiveHoursExceedLimit_returnsConsecutiveConflict` (with limits) + `..._noLimitsLoaded_noWorkloadConflict` (data-pending) | PASS |
| AC6 | No conflict → empty | `check_freeSlotNoRuleViolation_returnsEmpty`; `checkPlacement_noConflicts_returnsEmpty` | PASS |
| AC7 | Multiple conflicts returned | `check_placementViolatesTwoRules_returnsAllConflicts` | PASS |
| AC8 | Nonexistent draft rejected | `checkPlacement_draftNotFound_throwsAndDoesNotLoadOccupancy`; `checkDraft_draftNotFound_throws` | PASS |
| AC9 | Full-draft aggregate | `checkDraft_aggregatesConflictsAcrossPlacedSessions` | PASS |
| KD-64 | Recurrence gate (fortnightly non-overlap) | `check_weeklyProposalVsFortnightlyOccupant_reportsClash`; `recurrenceGate_oppositeFortnightlyGroups_neverCoOccur` | PASS |

---

## 3. Command

```
mvn -Dtest="PlacementRuleCheckerTest,ConflictDetectionServiceTest" test   (15/15 pass)
mvn test                                                                  (248/248 pass)
```

---

## 4. Important Coverage Caveat — AC5 and the deferred types

**AC5 (consecutive hours) and the daily/weekly workload rules are fully implemented
and unit-tested, but do not fire against live data yet.** Faculty workload limits
have no loaded master-data source (the engine's `SchedulingDataLoader.loadFacultyLimits`
is a stub). The rules are wired behind a `FacultyLimitProvider` seam; the default
`EmptyFacultyLimitProvider` returns no limits, so the workload rules degrade to
"no violation" — the same behavior as the engine's `CSPState`. The unit test
`check_consecutiveHoursExceedLimit_returnsConsecutiveConflict` proves the logic is
correct **when limits are supplied**; the companion test proves it stays silent when
they are not. Live firing awaits the faculty-limits data source (A4-4 / A4-32).

The following ConflictType values are DEFINED but detection is DEFERRED per the
approved design: `TRAVEL_TIME` (PD-98), `PREREQUISITE_SEQUENCE` (PD-96),
`FACULTY_HARD_BLOCK` (PD-99 — under-reports until the engine stub is fixed). No unit
tests assert their live detection, by design.

---

## 5. Verdict

All 9 firm acceptance criteria are exercised at the unit level and pass; the full
suite is green with no regression. AC5's live firing is data-pending (documented,
verified via supplied-limits test). Ready for the coverage report and code review.
