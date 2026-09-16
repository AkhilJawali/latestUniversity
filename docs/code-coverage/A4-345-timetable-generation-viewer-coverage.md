# Code Coverage — A4-345 Frontend Timetable Generation and Draft Viewer

**Story:** A4-345
**Code Coverage Subtask:** A4-377
**Date:** 2026-09-01

## Coverage Approach

Instrumented line/branch coverage is **not available** for this story: the A4-335 frontend foundation ships no test runner (no Vitest/Jest, no coverage tool such as `@vitest/coverage-v8`). Adding one is a foundation follow-up (design Section 10), out of scope here. In lieu of instrumented coverage, this document records **build verification** and **requirement-to-code traceability** so the delivered surface area is auditable.

- `pnpm build`: PASS (zero errors; GenerationPage code-split into its own chunk).
- `pnpm lint`: not runnable (ESLint absent from the foundation).

## Delivered Units (all reachable from the `/scheduling/generation` route)

| Unit | File | Covers |
|---|---|---|
| `useTriggerGeneration` | api/useGeneration.js | FR-1 |
| `useGenerationStatus` (2s poll, stops on terminal) | api/useGeneration.js | FR-2 |
| `useDraft` | api/useGeneration.js | FR-3 |
| `useSessions` (page/size/sort) | api/useGeneration.js | FR-4 |
| `useViolations` | api/useGeneration.js | FR-5 |
| `useUnplaced` | api/useGeneration.js | FR-6 |
| `useInfeasibility` (gated on INFEASIBLE) | api/useGeneration.js | FR-7 |
| `isTerminal` / `TERMINAL_STATUSES` | api/useGeneration.js | FR-2/8 routing |
| `generationSchema` / `validateGenerationForm` | lib/generationSchema.js | FR-1, Validation |
| `formatScorePercent` / `formatScoreRaw` | lib/formatScore.js | FR-3, PD-4 |
| `GenerateForm` | components/GenerateForm.jsx | FR-1, FR-1.4 |
| `ProgressPanel` | components/ProgressPanel.jsx | FR-2, UI-1, NFR-4 |
| `OutcomeBanner` | components/OutcomeBanner.jsx | FR-8, UI-2, NFR-4 |
| `DraftSummary` | components/DraftSummary.jsx | FR-3 |
| `SessionsTable` | components/SessionsTable.jsx | FR-4, PD-5/6, NFR-4/5 |
| `ViolationsList` | components/ViolationsList.jsx | FR-5, FR-5.2 |
| `UnplacedList` | components/UnplacedList.jsx | FR-6 |
| `InfeasibilityReport` | components/InfeasibilityReport.jsx | FR-7 |
| `GenerationPage` | GenerationPage.jsx | orchestration, AC-1..AC-7 |
| Router lazy route | app/router.jsx | FR-9, NFR-5 |

## Requirement Coverage

- Every FR (FR-1..FR-9), UI rule (UI-1..UI-3), and AC (AC-1..AC-7) maps to at least one delivered unit above (full mapping in design Section 11).
- Uncovered by automation: runtime behaviour (polling loop, error paths, empty states) — validated via the manual matrix in `docs/testing/A4-345-unit-test-results.md` and to be exercised in the Testing subtask A4-349.

## Coverage Target Note

The org standard targets 80% line coverage on new code. That target is **not measurable** without a test runner in the foundation. Recommendation: add Vitest + `@vitest/coverage-v8` + React Testing Library in a foundation story, then backfill component tests for this feature to meet the 80% target. Tracked as a carried-forward item.
