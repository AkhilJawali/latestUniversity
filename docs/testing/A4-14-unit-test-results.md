# Unit Test Results — A4-14 Locked Slot Preservation and Partial Re-Generation

**Story:** A4-14
**Subtask:** A4-363 (Unit Test)
**Date:** 2026-09-01
**Framework:** JUnit 5 + Mockito + AssertJ
**Command:** `mvn test -Dtest=SessionLockServiceTest,RegenerationScopeTest`

## Summary

| Metric | Value |
|---|---|
| Test classes | 2 |
| Tests run | 13 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Result | BUILD SUCCESS |

## Test Classes

### SessionLockServiceTest (6 tests)
Covers A4-14 FR-1 (session locking/unlocking) with mocked repository + audit publisher.

| Test | Scenario | Verifies |
|---|---|---|
| lock_validSession_setsLockedAndAudits | Lock a fully-placed session | isLocked=true, UPDATED audit event emitted (FR-1.1, FR-1.4) |
| lock_sessionNotFound_throwsEntityNotFound | Lock a non-existent session | 404, no save |
| lock_sessionInDifferentDraft_throwsEntityNotFound | Session belongs to a different draft | "subset belongs to draft" rule -> 404, no save |
| lock_incompletePlacement_throwsBusinessRuleViolation | Lock a session missing a room | "lock captures full placement" rule -> 422, no save (FR-1.3) |
| unlock_lockedSession_clearsLockedAndAudits | Unlock a locked session | isLocked=false, UPDATED audit event (FR-1.2, AC#6) |
| unlock_sessionNotFound_throwsEntityNotFound | Unlock a non-existent session | 404, no save |

### RegenerationScopeTest (7 tests)
Covers A4-14 KD-62 scope selector logic (isEmpty / matches).

| Test | Scenario | Verifies |
|---|---|---|
| isEmpty_allBlank_returnsTrue | Empty and null id-sets | isEmpty=true (drives 422 SCOPE_EMPTY) |
| isEmpty_hasBatchIds_returnsFalse | Non-empty batch ids | isEmpty=false |
| matches_byBatchId_returnsTrue | Session batch in scope | matches by batch |
| matches_bySectionId_returnsTrue | Session section in scope | matches by section |
| matches_byCourseId_returnsTrue | Session course in scope | matches by course |
| matches_anyIdSet_returnsTrue | Matches on one id-set only | OR semantics across id-sets |
| matches_nullSectionId_returnsFalse | Session with null section vs section scope | null-safety |

## Coverage of Acceptance Criteria (unit level)

| AC | Covered by | Notes |
|---|---|---|
| AC#1 (locked sessions never move) | Integration (A4-67) | Unit: lock sets isLocked; preservation is solver-level |
| AC#3 (only subset regenerated) | RegenerationScopeTest (partition logic) | Full flow is integration-level |
| AC#6 (unlock frees a session) | unlock_lockedSession_clearsLockedAndAudits | Unit: unlock clears flag |
| "lock without full placement -> 422" | lock_incompletePlacement_throwsBusinessRuleViolation | |
| "scope selects nothing -> 422" | isEmpty tests + service partition | |

## Notes / Deferred to Integration (A4-67)

The following require the full solver + SchedulingInput wiring and are covered by integration tests in the Testing subtask, not unit tests:
- `CSPState.prePlaceFixedSessions` occupancy behavior (needs a populated CSPState/SchedulingInput).
- End-to-end partial re-generation (AC#1, AC#2, AC#4, AC#5): locked/approved preservation, infeasibility surfacing, copy-forward zero-drift.

All authored unit tests pass with zero failures/errors.
