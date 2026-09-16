# A4-410 — Frontend Campus Hierarchy (CRUD) — Testing Results

| | |
|---|---|
| **Story** | A4-410 |
| **Testing subtask** | A4-414 |
| **Type** | Frontend build + full unit-test regression |
| **Result** | PASS — full suite 66/66, build success, no regressions |

## 1. Full test suite (regression)

- **Command:** `node node_modules/vitest/vitest.mjs run` (all tests, whole frontend)
- **Result:** **66 passed / 66**, 13 test files, 0 failures.
- Includes the 14 new A4-410 schema tests plus all pre-existing tests (scheduling-config components, generation feature, AppShell, api-client, api-error, routing). The new nav item and route did not break the existing AppShell/routing tests.
- Note: one `stderr` line appears during the run — an **existing** OutcomeBanner test deliberately passes an invalid `status` to assert the "unknown status renders nothing" path; it logs a PropType warning by design and the test passes. The shell exit code 1 is PowerShell capturing that stderr, not a test failure (summary: "66 passed").

## 2. Production build (compile gate)

- **Command:** `node node_modules/vite/bin/vite.js build`
- **Result:** success, built in ~5.8s. `CampusHierarchyPage` emitted as its own lazy chunk (14.55 kB JS / 0.53 kB CSS), confirming NFR-1 code-splitting and that all feature imports/JSX compile.

## 3. Acceptance-criteria coverage

| AC | How verified |
|----|--------------|
| AC-1 Browse hierarchy (campus down to section) | Drill-down page renders each level on parent selection; build confirms wiring. Runtime browse validated against seeded data (campus 1 -> departments -> ...). |
| AC-2 Create (happy path) + list refresh | Create hooks POST to verified routes; TanStack Query invalidates the list on success. Schema tests confirm valid payload passes. |
| AC-3 Referential-integrity delete error | Delete mutation surfaces backend 409/rejection; row retained (dialog stays open). Backend A4-2 enforces the block. |
| AC-4 Child create with parent | Parent id injected by create hooks; schema requires + validates it (department/program/batch schema tests). |
| AC-5 Validation blocks bad input | 14 schema tests: missing required, bad code format, non-positive numbers all rejected; modal sends no request on invalid. |
| AC-6 Duplicate code -> 409 surfaced | Create mutation onError maps backend 409 via `mapApiError` to a form-level message; no duplicate created. |

## 4. Code-review fix verification

- The one code-review fix (memoized `initialValues` to stop the modal resetting on parent re-render) was re-verified: unit tests 14/14 and build success **after** the fix.

## 5. Issues found during testing

- None. No regressions in the existing suite; the new feature compiles and its validation layer is fully green.

## 6. Notes / deferred (from code review, non-blocking)

- Pagination controls (lists cap at backend default 20) — deferred follow-up.
- Delete-failure inline toast — deferred (shared component).
- These do not affect the acceptance criteria and were triaged in `docs/code-review/A4-410-code-review.md`.
