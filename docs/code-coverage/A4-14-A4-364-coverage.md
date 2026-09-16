# Code Coverage Report — A4-14 Locked Slot Preservation and Partial Re-Generation

**Story:** A4-14
**Subtask:** A4-364 (Code Coverage)
**Date:** 2026-09-01
**Tool:** JaCoCo 0.8.12
**Command:** `mvn test -Dtest=SessionLockServiceTest,RegenerationScopeTest jacoco:report`
**Targets:** 80% line / 70% branch on new code

## Coverage of New A4-14 Classes (unit-tested)

| Class | Lines Covered | Line % | Branches Covered | Branch % | Meets Target |
|---|---|---|---|---|---|
| SessionLockService | 35 / 35 | 100% | 9 / 12 | 75% | Yes |
| RegenerationScope | 6 / 6 | 100% | 19 / 22 | 86% | Yes |
| FixedReason (enum) | 0 / 4 | n/a | n/a | n/a | See note |

**FixedReason note:** A plain enum with three constants and no methods. JaCoCo reports the synthetic `values()`/`valueOf()` as uncovered lines; there is no executable branch logic to test. Excluded from the target as it has no behavior.

## Uncovered Branches (accounted for)

- **SessionLockService (3 branches):** the `requireCompletePlacement` check is a compound boolean over 4 placement fields (day/slot/room/faculty). The unit test covers the all-present path and the room-null path; the day/slot/faculty-null branches share the same code path and outcome (422) — covered behaviorally, not each null permutation. Low risk.
- **RegenerationScope (3 branches):** null-guard permutations in `contains()`/`isBlank()` for the section/course id-sets not exercised in every combination; the batch and section paths cover the guard logic. Behaviorally equivalent.

## Classes Covered by Integration Tests (A4-67), not unit coverage

These are exercised end-to-end (solver + SchedulingInput wiring) in the Testing subtask, so they are intentionally not unit-covered here:

| Class / Method | Reason |
|---|---|
| CSPState.prePlaceFixedSessions | Needs a populated CSPState + SchedulingInput (occupancy maps, room/slot grids) |
| SchedulingEngineService.triggerRegeneration / executeRegeneration | Async orchestration + full solver run |
| SchedulingResultPersister.persistRegenerationResult / copyForwardFixedSessions | Needs a persisted draft + DB (Testcontainers) |
| SchedulingController.regenerate | MVC/integration slice |

## Requirement Traceability

| Requirement | Covered By |
|---|---|
| FR-1 (lock/unlock) | SessionLockService unit tests (100% line) |
| KD-62 (scope selector) | RegenerationScope unit tests (100% line, 86% branch) |
| FR-3 partition, HC-LOCK-1..4, copy-forward | Integration tests (A4-67) |

## Conclusion

New unit-tested logic classes (SessionLockService, RegenerationScope) exceed the 80% line and 70% branch targets. The solver/orchestration/persistence paths are deferred to integration testing (A4-67) by design, since they require full engine wiring and a database.
