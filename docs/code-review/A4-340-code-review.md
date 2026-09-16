# A4-340 — Scheduling Config Admin Panel — Code Review

| | |
|---|---|
| **Story** | A4-340 |
| **Code Review subtask** | A4-343 |
| **Reviewer (AI)** | semantic_reviewer |
| **Verdict** | COMMENT — no blockers |
| **Build/Tests** | `pnpm build` exit 0; 25 unit tests pass |

Full AI review detail: `docs/code-review/A4-340-ai-review.md`.

---

## Confirmed strengths
- Routing complete: `/scheduling/config` is lazy + Suspense inside the shell.
- State-management steering compliant: TanStack Query for server state, Zustand for the UI-only campus id, structured query keys, invalidate-on-success, `enabled` gate, staleTime.
- Consistent envelope access: hooks return axios `.data` (`{data, meta}`); tabs read `list.data?.data`.
- Security: JSX text escaping only, no `dangerouslySetInnerHTML`/`eval`/secrets; `mapApiError` never leaks stack traces/internal paths.
- AC-5 met: invalid submit sets field errors and returns before any mutation.
- Loading / error / empty / populated states all handled. No TypeScript in source.
- The 6 design->implementation deviations are reasonable and internally consistent.

## Fixed before submission

| # | Finding | Severity | Resolution |
|---|---------|----------|------------|
| 3 | `isActive` displayed + schema-accepted but no form control could set it | Medium | Added an "Active" checkbox to all three forms (derivation, weight, common slot); create defaults `isActive: true`. |
| 1 | Tab widget missing `id`/`aria-controls`/`aria-labelledby` wiring | Medium | Wired `tab-{id}` / `panel-{id}` with `aria-controls` + `aria-labelledby`. |
| 2 | No keyboard navigation between tabs | Medium | Added ArrowLeft/ArrowRight roving-tabindex navigation. |

Verified after fixes: `pnpm build` exit 0, 25 tests pass.

## Accepted / deferred

| # | Finding | Severity | Decision |
|---|---------|----------|----------|
| 4 | Unguarded `{data}` envelope access could render empty on shape drift | Medium | Already defensive (`list.data?.data ?? []`); the tab render path is guarded. Full shape-validation deferred to A4-344 integration. |
| 5 | Edit spreads whole row into PUT body | Low | Backend (A4-390) allowlists non-updatable fields (campusId/type immutable per PD-84/KD-2); acceptable. Trim in a follow-up if desired. |
| 6 | NaN campusId from corrupt sessionStorage | Low | Low-risk; the picker only sets positive integers. Hardening deferred. |
| 7 | No explicit modal close (X) button | Low | Cancel button + Escape (native dialog) cover dismissal; X is cosmetic. |
| 8 | No focus-visible / reduced-motion CSS | Low | Deferred to a broader a11y polish pass across the app. |

## Design->implementation deviations (documented, accepted)
1. Real A4-390 endpoints, not the design's provisional `/scheduling-config/*`.
2. `componentType` LECTURE/TUTORIAL/PRACTICAL (A4-390), not `L/T/P`.
3. Plain CSS + native `<dialog>`/tabs (A4-335 PD-F1), not Shadcn/Radix.
4. Plain `useState` + Zod (matches generation feature), not react-hook-form (not installed).
5. Page-level campus picker instead of shell header selector (TODO in code).
6. `slotDefinitionId` number input, not a lookup dropdown (follow-up).

**Author verdict after fixes: ready for human review (COMMENT — no blockers).**
