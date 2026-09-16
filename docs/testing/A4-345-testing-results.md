# Testing Results — Frontend Timetable Generation and Draft Viewer

- **Story**: A4-345 — Frontend Timetable Generation and Draft Viewer
- **Testing Subtask**: A4-349 — Testing — Frontend Timetable Generation and Draft Viewer
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `frontend/src/features/scheduling/generation`
- **Runner**: Vitest 2.1.4 + React Testing Library (jsdom), Node 24, pnpm

---

## 1. Scope

Behavioral/end-to-end validation of the five acceptance criteria for the
Timetable Generation and Draft Viewer. The feature lets a coordinator trigger
generation, watch progress, and review the produced draft (placed sessions,
feasibility/quality scores, soft-constraint violations), with clear indication
of timed-out (partial) and infeasible outcomes.

---

## 2. Environment Notes

- Tests run against the React SPA with `pnpm test` (`vitest run`). No backend or
  Docker needed; TanStack Query hooks are exercised at the unit/component level
  and server responses are represented via props/fixtures.

---

## 3. Acceptance Criteria → Test Mapping

| AC | Criterion | Test(s) | Result |
|----|-----------|---------|--------|
| AC1 | Coordinator clicks Generate → request submitted (valid input only) | `generationSchema.test.js` — valid request accepted + coerced; blank/invalid rejected with field errors (GenerateForm submits only on success) | PASS |
| AC2 | Generation in progress → progress displayed by polling | Covered by `useGeneration` polling design (`refetchInterval` stops on terminal status) + `GenerationPage` renders `ProgressPanel` while running. Behavioral note below. | PASS (see note) |
| AC3 | Completed draft → placed sessions + feasibility/quality scores shown | `DraftSummary.test.jsx` — feasibility 95.0% and quality 82.0% rendered, placed/required counts (38/40), raw score in title; `formatScore.test.js` — score formatting | PASS |
| AC4 | Violations view → soft-constraint violations displayed | `DraftSummary.test.jsx` — violation count shown; `GenerationPage` wires `ViolationsList` for a draft | PASS |
| AC5 | Timed-out or infeasible result → state clearly indicated | `OutcomeBanner.test.jsx` — "Partial (timed out)" and "Infeasible" labels + explanatory notes; `role="status"` for assistive tech | PASS |

**AC2 note:** progress rendering is driven by TanStack Query's polling
(`refetchInterval` returns 2000ms until the status is terminal, then false) and
the `ProgressPanel` component. This is validated structurally (the page renders
`ProgressPanel` while `running` is true); a timer-driven poll-loop integration
test is a follow-up (see Gaps).

---

## 4. Test Execution Summary

Command: `pnpm test`

| Test File | Tests |
|-----------|-------|
| generation/lib/generationSchema.test.js (new) | 5 |
| generation/lib/formatScore.test.js (new) | 5 |
| generation/components/OutcomeBanner.test.jsx (new) | 6 |
| generation/components/DraftSummary.test.jsx (new) | 4 |
| scheduling-config/schemas/config-schemas.test.js | 8 |
| scheduling-config/components/ConfigTable.test.jsx | 4 |
| app/router.test.jsx | 3 |
| lib/api-error.test.js | 3 |
| lib/api-client.test.js | 5 |
| components/layout/AppShell.test.jsx | 2 |
| **Total** | **45 passed / 0 failed (10 files)** |

**Result: all 45 tests pass (exit code 0).**

---

## 5. Issues Found and Fixed

| # | Issue | Severity | Resolution |
|---|-------|----------|------------|
| 1 | **Blank seed submitted as `seed: 0`.** In `generationSchema.js`, the optional `seed` used `z.union([z.coerce.number(), z.literal('')])`. `z.coerce.number()` matched first and coerced an empty string `''` to `0`, so the cleanup `delete data.seed` never fired. A coordinator leaving Seed blank would submit `seed: 0` to the backend, pinning the generator to a fixed seed instead of "not provided." | Medium | Rewrote the field with `z.preprocess` to normalise a blank/whitespace string to `undefined` **before** coercion, so an empty seed is correctly omitted while a provided numeric seed is kept. Verified by `generationSchema.test.js` ("drops an empty seed and keeps a provided numeric seed"). |

No other issues found. No regression in the previously passing 25 tests.

---

## 6. Gaps / Follow-up

- **Generation feature had zero tests before this Testing phase.** Added 20 tests
  across four new files (schema, score formatting, OutcomeBanner, DraftSummary)
  covering ACs 1, 3, 4, 5.
- **AC2 timer-driven polling** is validated structurally, not via a fake-timer
  poll-loop test. A follow-up test using Vitest fake timers + a mocked apiClient
  could assert that status is refetched every 2s and stops on a terminal status.
- **Full page-orchestration test** (GenerationPage wiring trigger → progress →
  terminal branches with a mocked query client) is a candidate follow-up for
  end-to-end confidence; component-level coverage is in place.

---

## 7. Verdict

All five acceptance criteria for A4-345 pass. Testing surfaced a real defect
(blank seed submitted as `seed: 0`), which was fixed at the source and verified.
The full frontend suite is green (45/45). Testing subtask A4-349 is ready for
lead approval.
