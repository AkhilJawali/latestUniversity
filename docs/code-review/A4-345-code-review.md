# Code Review — A4-345 Frontend Timetable Generation and Draft Viewer

**Story:** A4-345
**Code Review Subtask:** A4-348
**Reviewer (AI pre-review):** semantic_reviewer
**Date:** 2026-09-01
**Final Verdict:** APPROVED (after fixes)

## Process

Per squad rules, an AI semantic review was run before human review. The first pass returned **NEEDS_CHANGES** (1 blocking bug + 6 polish/minor findings). Blocking and cheap high-value items were fixed, the build was re-verified, and a second pass returned **APPROVED**.

Note: the workspace is not a git repository, so the reviewer read the 14 changed files directly on disk and cross-checked `SchedulingController.java` (backend response shapes) and `package.json` (`@tanstack/react-query` 5.59.16).

## Findings and Resolutions

| # | Severity | Finding | Resolution |
|---|---|---|---|
| 1 | **Blocking** | `keepPreviousData: true` is a no-op in TanStack Query v5 (`useSessions`), so paging/sorting would flash an empty table — contrary to the code comment. | FIXED — import `keepPreviousData` and use `placeholderData: keepPreviousData` (v5 API). |
| 2 | Minor (a11y) | Sort glyph `▲/▼` not `aria-hidden`, announced as stray punctuation. | FIXED — wrapped in `<span aria-hidden="true">`; `aria-sort` on `<th>` still conveys state. |
| 3 | Minor (a11y) | Sessions table skeleton loading had no busy cue. | FIXED — `aria-busy={isLoading}` on the table. |
| 4 | Minor (maintainability) | Repeated isError/isLoading/empty scaffolding across ViolationsList, UnplacedList, SessionsTable, InfeasibilityReport. | DEFERRED — non-blocking; consistent and correct in each copy; each file under the 150-line limit. Follow-up refactor (shared AsyncSection helper). |
| 5 | Minor (standards) | `generation.css` hardcoded status hex instead of theme tokens. | FIXED — added semantic tokens to `global.css` (`--color-destructive/-bg`, `--color-success/-bg`, `--color-warning/-bg`); `generation.css` now references them. |
| 6 | Possible (minor) | Seed field Zod message on non-numeric input. | ACCEPTED — in practice `type="number"` yields empty string (treated as not provided); a decimal triggers the specific "whole number" message. Not generic. |
| 7 | Likely (minor UX) | No retry affordance on status-poll error. | DEFERRED — non-blocking; the GenerateForm is always mounted and re-runnable, so the flow is not dead-ended. Follow-up: add a refetch/retry button. |

## Verified Clean (no findings)

- **Routing:** page reachable at `/scheduling/generation`, lazy-loaded behind Suspense in AppShell (NFR-5).
- **Security (XSS):** no `dangerouslySetInnerHTML`; all backend strings (violation descriptions, infeasibility explanations, unplaced reasons) render as escaped JSX text.
- **Response handling:** backend returns a bare Spring `Page` and bare DTOs/lists, so `r.data` / `data.content` reads are correct (no `{data, meta}` envelope for these endpoints).
- **TanStack Query v5:** `refetchInterval` uses the v5 `(query) => ...` signature and stops on terminal status; `enabled` gating correct everywhere, including the infeasibility query gated to avoid the backend 404; mutation uses `isPending`.
- **Standards:** PropTypes on all components; no TypeScript; import order per ESLint grouping; all files <= 150 lines.

## Verification

- `pnpm build` — PASS (zero errors) before and after fixes; GenerationPage code-splits into its own chunk.
- `pnpm lint` — not runnable (ESLint absent from the A4-335 foundation; flagged as a foundation gap, not a defect in this story).

## Recommended Follow-ups (tracked, non-blocking)

1. Extract a shared async-section helper for the four list/report components (Issue 4).
2. Add a retry/refetch affordance on status-poll error (Issue 7).
3. Add a frontend test runner (Vitest + RTL) + ESLint at the foundation level so lint/coverage gates become enforceable.
4. Resolve PD-5 (sessions table shows raw IDs) via a name-resolution source or backend DTO enrichment.
