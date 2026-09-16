# Testing Results — Real-Time Conflict Detection Engine (A4-16)

- **Story**: A4-16 — Real-Time Conflict Detection Engine
- **Testing Subtask**: A4-75
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `com.utms.scheduling.conflict`
- **Build**: Maven `test` — JDK 21 (Amazon Corretto 21.0.8), Spring Boot 3.3.2

---

## 1. Scope

End-to-end testing of the real-time conflict detection engine after Code Review
approval (A4-74). Validates the story's firm acceptance criteria and confirms no
regression across the whole project with all code-review fixes in place.

Gate: Code Review (A4-74) verified **Approved** before testing.

---

## 2. Acceptance Criteria → Test Mapping

| AC | Criterion | Test | Result |
|----|-----------|------|--------|
| AC1 | Faculty double-booking detected | `PlacementRuleCheckerTest.check_facultyAlreadyBookedInSlot_returnsFacultyDoubleBooking` | PASS |
| AC2 | Room double-booking detected | `check_roomAlreadyBookedInSlot_returnsRoomDoubleBooking` | PASS |
| AC3 | Batch clash (batch/section) | `check_batchAlreadyBookedInSlot_returnsBatchClash` | PASS |
| AC4 | Room capacity < batch strength | `check_roomCapacityLessThanBatchStrength_returnsCapacityConflict` | PASS |
| AC5 | Consecutive hours (distinct type) | `check_consecutiveHoursExceedLimit_returnsConsecutiveConflict` (with limits) + data-pending companion | PASS |
| AC6 | No conflict → empty | `check_freeSlotNoRuleViolation_returnsEmpty`; `checkPlacement_noConflicts_returnsEmpty` | PASS |
| AC7 | Multiple conflicts returned | `check_placementViolatesTwoRules_returnsAllConflicts` | PASS |
| AC8 | Nonexistent draft rejected | `checkPlacement_draftNotFound_throwsAndDoesNotLoadOccupancy`; `checkDraft_draftNotFound_throws` | PASS |
| AC9 | Full-draft aggregate | `checkDraft_aggregatesConflictsAcrossPlacedSessions` | PASS |
| KD-64 | Recurrence gate | `check_weeklyProposalVsFortnightlyOccupant_reportsClash`; `recurrenceGate_oppositeFortnightlyGroups_neverCoOccur` | PASS |

All 9 firm acceptance criteria pass.

---

## 3. Test Execution Summary

Command: `mvn test`

- Conflict package: `PlacementRuleCheckerTest` (10) + `ConflictDetectionServiceTest` (5) — all PASS.
- **Whole project suite: 248 tests, 0 failures, 0 errors — BUILD SUCCESS.** No regression.
- Run includes the four code-review fixes (doc-accuracy on ROOM_HARD_BLOCK/FACULTY_HARD_BLOCK, WARN log on slot fallback, removed redundant `Math.max`).

---

## 4. Issues Found and Fixed

None during this testing phase. (The code-review phase found and fixed 4 non-blocking
items — see `docs/code-review/A4-16-code-review.md`; all re-verified green here.)

---

## 5. Gaps / Follow-up (carried from design + code review, not blocking)

- **AC5 / workload rules are data-pending:** consecutive/daily/weekly detection is
  implemented and unit-verified with supplied limits, but does not fire live until the
  faculty-limits master-data source exists (A4-4/A4-32). Documented in the unit-test
  and code-review docs.
- **Deferred conflict types** (per approved design): TRAVEL_TIME (PD-98),
  PREREQUISITE_SEQUENCE (PD-96), FACULTY_HARD_BLOCK and ROOM_HARD_BLOCK — enum values
  defined, detection deferred; not asserted here by design.
- **Integration tests (Testcontainers + REST Assured)** for the DB loader, REST
  controller (HTTP 200/400/404), and STOMP round-trip were NOT run — Docker is
  unavailable in this environment. Recommended follow-up in a Docker-enabled CI, which
  would also validate the 2s conflict SLA under a realistic draft.
- **Student-level clash (AC3 original story text)** deferred to the elective-registration
  story (PD-95); batch/section clash is what is detected today.

---

## 6. Verdict

All 9 firm acceptance criteria pass; the full suite is green (248/248) with the
code-review fixes in place and no regression. Data-pending (AC5) and deferred types
are documented and accepted per the approved design. Testing subtask A4-75 is ready
for lead approval.
