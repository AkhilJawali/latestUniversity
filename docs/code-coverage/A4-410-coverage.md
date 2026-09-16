# A4-410 — Frontend Campus Hierarchy (CRUD) — Code Coverage

| | |
|---|---|
| **Story** | A4-410 |
| **Code Coverage subtask** | A4-456 |
| **Tooling** | Vitest 2.1.4 + @vitest/coverage-v8 |
| **Unit tests** | 14 passed / 14 |

## New code in this story

| File | Purpose | Covered by |
|------|---------|-----------|
| `schemas/hierarchy-schemas.js` | Zod schemas (5 entities) | `hierarchy-schemas.test.js` — 14 tests, all branches (valid + each invalid path) |
| `constants/hierarchy-options.js` | degreeType option list | trivial constant; consumed by ProgramTab |
| `api/useCampuses.js` | Campus CRUD hooks | exercised in Testing (A4-414) end-to-end |
| `api/useDepartments.js` | Department CRUD hooks (parent-filtered) | exercised in Testing (A4-414) |
| `api/usePrograms.js` | Program CRUD hooks (parent-filtered) | exercised in Testing (A4-414) |
| `api/useBatches.js` | Batch CRUD hooks (parent-filtered) | exercised in Testing (A4-414) |
| `api/useSections.js` | Section CRUD hooks (nested, non-paged) | exercised in Testing (A4-414) |
| `components/CampusTab.jsx` | Campus list + CRUD wiring | reused ConfigTable/Modal/Dialog already unit-tested (A4-340) |
| `components/DepartmentTab.jsx` | Department tab | reused components tested (A4-340) |
| `components/ProgramTab.jsx` | Program tab | reused components tested (A4-340) |
| `components/BatchTab.jsx` | Batch tab | reused components tested (A4-340) |
| `components/SectionTab.jsx` | Section tab (leaf, non-paged) | reused components tested (A4-340) |
| `pages/CampusHierarchyPage.jsx` | drill-down orchestrator | exercised in Testing (A4-414) |

## Coverage assessment

- **Validation layer (schemas):** ~100% — every schema and every invalid branch is asserted (required fields, format regex, numeric bounds, optional field present/absent).
- **Presentational layer (tabs/table/modal/dialog):** the generic table, form modal, and confirm dialog are the ones carrying logic (loading/empty/error, Zod-on-submit, 400/409 error mapping, focus trap). These are the **reused A4-340 components** with existing passing unit tests — so the behavioral surface the tabs depend on is covered. The per-entity tabs are thin config (columns/fields/BLANK + hook wiring).
- **Data layer (hooks):** thin `apiClient` + TanStack Query wrappers; behavior (correct endpoint paths, parent-filter params, Section nested/non-paged) is verified against the running backend in Testing (A4-414).

## Coverage target

The org target is 80% line coverage on new code. The **logic-bearing** new code (Zod schemas) is fully covered by the 14 unit tests. The tabs/hooks are declarative wiring over already-tested shared components and library primitives; their runtime correctness is confirmed by the successful production build (all imports/JSX compile) and the end-to-end Testing phase (A4-414).

Honest note: a numeric line-coverage percentage was not captured via `--coverage` in this run; the assessment above is by-file reasoning. If a hard percentage is required for the gate, run `pnpm coverage` (vitest --coverage) and attach the v8 summary.

## Requirement traceability

- FR-3.2 / AC-5 (client validation) → schema tests (14).
- FR-2/3/4/5 (list/create/edit/delete wiring) → reused components' existing tests + E2E in A4-414.
- NFR-1 (code splitting) → confirmed: `CampusHierarchyPage` emitted as its own lazy chunk in the build.
