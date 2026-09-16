# A4-410 — Frontend Campus Hierarchy (CRUD) — Code Review

| | |
|---|---|
| **Story** | A4-410 |
| **Code Review subtask** | A4-413 |
| **Reviewer** | AI semantic review (pre-human) + author fixes |
| **Verdict (initial)** | COMMENT (1 fix recommended + suggestions) |
| **Verdict (after fixes)** | Clean — blocking-ish issue #1 fixed; suggestions triaged |

## Scope reviewed

The new `features/master-data/campus-hierarchy/` feature: 5 Zod schemas (+tests), 5 API hook files, 5 entity tabs, the drill-down page, router + nav wiring. Reviewed behaviorally and cross-checked against the real A4-2 backend controllers/DTOs.

## What was verified as correct

- **Routing** — `CampusHierarchyPage` is lazy-loaded, wrapped in `Suspense` + `AppShell`, nav item path matches. Nothing unreachable.
- **API routes (no URL-mismatch bug)** — every hook BASE matches a real A4-2 controller route: `/campuses`, `/departments`, `/programs`, `/batches`, and the Section split (`/batches/{id}/sections` list/create, `/sections/{id}` single ops). `apiClient` baseURL `/api/v1`. The prior URL-mismatch bug class (that broke the config page) does NOT recur.
- **Envelope handling** — `list.data?.data ?? []` correct for both paginated `{data,meta}` and Section's non-paged `{data:[...]}`.
- **Reused-component contract** — `ConfigTable`/`ConfigFormModal`/`ConfirmDeleteDialog` props honored (columns key+header, `_select` header `''` is a valid string, `onSubmit(data,opts)` + `onError` mapping via `mapApiError`).
- **Parent-id validation** — the child schemas require the parent id; create seeds it via `initialValues`, edit gets it from the spread DTO row. Validation passes in both paths. Not a bug.
- **Accessibility / keys** — no regressions; reused dialogs give focus-trap + Escape, buttons have aria-labels, rows keyed by id.

## Issues and disposition

| # | Issue | Severity | Disposition |
|---|-------|----------|-------------|
| 1 | **Form reset on parent re-render**: tabs passed a fresh `initialValues` object literal each render; `ConfigFormModal`'s `useEffect([open, initialValues])` re-seeds `values` whenever that identity changes, so a parent re-render (e.g. `isPending` flip on submit) wiped in-progress input. | Blocking-ish | **FIXED** — memoized `initialValues` with `useMemo` keyed on `editing` (+ parent id) in all 5 tabs. Re-ran tests (14/14) + build (success). |
| 2 | Paginated lists send no `size`/`page` -> silently capped at backend default (20), `meta` discarded, no pager. | Suggestion | **Deferred** — acceptable for demo/first release; pagination-controls enhancement can be a follow-up. Requirement did not mandate pager UI. |
| 3 | Edit spreads immutable fields (code, parent id) into PUT body; backend Update DTOs ignore unknowns via Jackson. | Suggestion | **Accepted risk** — works today; coupled to `FAIL_ON_UNKNOWN_PROPERTIES` staying off (it is off across the codebase). Noted for future hardening. |
| 4 | `degreeType` select can blank a valid non-preset stored value on edit. | Suggestion | **Deferred** — seed data uses the six presets; edge case only if a program is created via API with an off-list degree. Follow-up: add current value as an option. |
| 5 | Delete failure has no error surface (dialog stays open silently). | Suggestion | **Deferred** — `ConfirmDeleteDialog` is the reused A4-340 component; adding an error path there is a shared-component change out of this story's scope. The failed mutation still rejects; the dialog simply stays open. Follow-up candidate. |
| 6 | `electiveBasket: ''` on create — confirm backend treats empty string as absent. | Nit | **Low risk** — backend field is optional `@Size(max=200)`; empty string is within bounds. |
| 7 | Thin component test coverage (only schemas). | Suggestion | **Accepted** — schemas (the logic-bearing new code) fully tested; tabs/hooks are declarative wiring over already-tested shared components; E2E covered in Testing (A4-414). |

## Outcome

The one issue with real user impact (#1) is **fixed and re-verified**. The remaining items are suggestions/deferred enhancements, none blocking, each with a rationale. Code compiles (vite build success), unit tests pass (14/14). Ready for human review / approval.

## Files changed by the fix
- `components/CampusTab.jsx`, `DepartmentTab.jsx`, `ProgramTab.jsx`, `BatchTab.jsx`, `SectionTab.jsx` — memoized `initialValues`.
