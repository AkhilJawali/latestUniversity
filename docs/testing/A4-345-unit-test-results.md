# Unit Test Results — A4-345 Frontend Timetable Generation and Draft Viewer

**Story:** A4-345
**Unit Test Subtask:** A4-375
**Date:** 2026-09-01

## Summary

The A4-335 frontend foundation ships **no test runner** (package.json scripts are only `dev`, `build`, `preview`, `lint`; no Vitest/Jest, no React Testing Library). Adding a component-test harness (Vitest + RTL) is a foundation-level change and is out of scope for A4-345 — it is carried forward as a follow-up (design Section 10). Therefore, "unit testing" for this story is satisfied by the enforceable build-verification gate plus a documented manual verification matrix.

## Automated Verification

| Check | Command | Result |
|---|---|---|
| Production build (zero errors) | `pnpm build` | PASS — 155 modules transformed, built in ~5s. GenerationPage emitted as a separate code-split chunk (`GenerationPage-*.js` + `.css`), confirming lazy loading (NFR-5). |
| Lint | `pnpm lint` | NOT RUN — `eslint` is not installed and no ESLint config exists in the foundation. Foundation gap, flagged; not introduced by this story. |

Build output (abridged):
```
dist/assets/GenerationPage-*.css    3.30 kB
dist/assets/GenerationPage-*.js   122.42 kB
dist/assets/index-*.js            237.66 kB
✓ built in 4.96s
```

## Manual Verification Matrix (against a running backend — to be executed in the Testing subtask A4-349)

| # | Scenario | Expected | Requirement |
|---|---|---|---|
| 1 | Submit generate with valid dept/semester/year | 202 accepted, progress indicator appears | FR-1, AC-1 |
| 2 | Submit with blank required field | Inline Zod error, no request sent | FR-1, Validation |
| 3 | Generation IN_PROGRESS | Phase, best-so-far/total, elapsed shown; polls every 2s | FR-2, AC-2 |
| 4 | COMPLETED | Draft summary (scores), sessions table (paginated/sortable), violations list | FR-3/4/5, AC-3/4 |
| 5 | Violations empty | "No soft-constraint violations" empty state | FR-5.2, AC-4 |
| 6 | TIMED_OUT | Partial banner + placed sessions + unplaced list | FR-6, AC-5 |
| 7 | INFEASIBLE | Infeasible banner + summary + conflicts; no draft fetch/404 | FR-7, AC-6 |
| 8 | FAILED / CANCELLED | Banner message only, re-trigger available | FR-8 |
| 9 | 503 on generate | Retry-later message, action re-enabled | FR-1.4, AC-7 |
| 10 | Sessions table sort + page-size 20/50/100 | Sort toggles asc/desc; page resets on size change | FR-4, PD-6 |

## Known Limitations

- No instrumented unit tests (no runner in the foundation). Behaviour is verified via build + manual matrix.
- Sessions table shows raw IDs (course/faculty/room/slot) per PD-5 / OQ#4 — pending a name-resolution source.
- `pnpm lint` cannot run until ESLint is added to the foundation.

## Traceability

All FRs (FR-1..FR-9), UI rules (UI-1..UI-3), and ACs (AC-1..AC-7) from the approved requirement/design map to implemented components (see design Section 11). Build passes; feature is reachable at route `/scheduling/generation`.
