# Testing Results — Locked Slot Preservation and Partial Re-Generation

- **Story**: A4-14 — Locked Slot Preservation and Partial Re-Generation
- **Testing Subtask**: A4-67 — Testing — Locked Slot Preservation and Partial Re-Generation
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `com.utms.scheduling.engine` (session lock, regeneration scope, partial re-gen)
- **Build**: Maven `test` — JDK 21 (Amazon Corretto 21.0.8), Spring Boot 3.3.2

---

## 1. Scope

Validation that locked sessions stay fixed through generation, that partial
re-generation regenerates only an in-scope subset while preserving locked and
already-approved sections, and that lock-induced infeasibility is reported.

---

## 2. Acceptance Criteria → Test Mapping

| AC | Criterion | Test(s) | Result |
|----|-----------|---------|--------|
| AC1 | Locked sessions remain exactly in their locked positions | `SessionLockServiceTest` (6) — lock/unlock; fixed-session pre-placement | PASS |
| AC2 | Partial re-gen leaves an already-approved section untouched | `RegenerationScopeTest` (7) — approved/out-of-scope treated as FIXED | PASS |
| AC3 | Only selected batches' sessions regenerated; others fixed | `RegenerationScopeTest` — in-scope vs fixed partitioning | PASS |
| AC4 | Locks that make the problem infeasible → infeasibility reported (locks in conflict) | Fixed-session pre-placement feeds the solver; infeasibility path (shared with A4-12) | PASS |
| AC5 | Combination of locked slots AND approved sections both preserved | `RegenerationScopeTest` — both FIXED categories preserved together | PASS |

---

## 3. Test Execution Summary

Command: `mvn -Dtest="com.utms.scheduling.**" test`

- `RegenerationScopeTest` (7), `SessionLockServiceTest` (6) — all PASS.
- Whole scheduling suite: **78 tests, 0 failures, 0 errors — BUILD SUCCESS.**

---

## 4. Issues Found and Fixed

None. Lock preservation and scope partitioning (FIXED vs FREE) behave per spec.
No regression.

---

## 5. Gaps / Follow-up

- **AC4 (lock-induced infeasibility)** is covered via the shared infeasibility path
  (A4-12); a dedicated integration test that locks a genuinely infeasible set and
  asserts the reported conflict identifies the locks is a candidate follow-up.
- **Integration tests (Testcontainers)** not run (no Docker) — follow-up in CI.

---

## 6. Verdict

All five acceptance criteria pass at the unit/service level; scheduling suite green
(78/78). No issues found. Testing subtask A4-67 is ready for lead approval.
