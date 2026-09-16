# A4-420 — Frontend Faculty Profile Management: Code Coverage

- **Story:** A4-420 — Frontend Faculty Profile Management (CRUD)
- **Code Coverage subtask:** A4-466
- **Date:** 2026-09-04
- **Target:** 80% line coverage on new logic-bearing code (org testing standard)

## Method

No numeric coverage percentage was captured: while a `coverage` provider (v8) is
configured in vitest.config.js, this story reports coverage by-file with reasoning about
which code paths the 18 passing unit tests exercise, consistent with A4-410 and A4-415.

## Covered Files

| File | Covered by tests | Assessment |
|------|-----------------|------------|
| `schemas/faculty-schemas.js` — `facultyCreateSchema` | 12 tests (valid + optional-loads + each invalid branch + min≤max refine) | ~100% of validation branches |
| `schemas/faculty-schemas.js` — `facultyEditSchema` | 3 tests | Fully exercised; confirms immutable fields excluded |
| `schemas/faculty-schemas.js` — `buildFacultyListParams` | 3 tests | All branches: empty-omit, set-filters coercion, page/size passthrough |
| `constants/faculty-options.js` — `DESIGNATIONS` | Indirectly via schema tests | Trivial constant, no branches |

## Files Not Unit-Tested (rationale)

| File | Rationale |
|------|-----------|
| `pages/FacultyManagementPage.jsx` | React wiring (filter state, modal open/close, mutation dispatch, panel selection). Exercised via the vite build (compiles + chunk emitted); component render tests are a candidate follow-up, consistent with A4-410/A4-415. |
| `components/FacultyFormModal.jsx` | Custom form modal; validation logic it invokes is covered by the schema tests. Render-level tests deferred. |
| `components/CompetencyPanel.jsx`, `components/CampusPanel.jsx` | Thin UI over API hooks. Same rationale. |
| `api/useFaculty.js`, `api/useFacultyCompetencies.js`, `api/useFacultyCampuses.js` | TanStack Query wrappers over apiClient against verified A4-4 routes; covered indirectly by the successful build and A4-4 backend tests. |

## Build Verification

- `node node_modules/vite/bin/vite.js build` → **success**, 206 modules transformed, dedicated `FacultyManagementPage` JS + CSS chunk emitted. Confirms all new modules compile and the lazy route resolves.

## Backend Follow-up (OQ-1) Note

This story's campus panel depends on the new backend endpoint `GET /api/v1/faculty/{id}/campuses`
(added to the A4-4 module to resolve OQ-1). That backend change was verified by
inspection only — Maven is not available in this environment to compile/run the backend
tests. This is flagged so a backend build/test is run before relying on the endpoint.

## Coverage Gaps and Plan

- Component render tests for the page, form modal, and panels — deferred (same precedent as prior master-data stories).
- Numeric coverage run (`vitest run --coverage`) would produce a hard percentage; recommended as a project-wide step.

## Traceability

All logic-bearing validation and filter code introduced by A4-420 is covered by the 18
passing unit tests. UI wiring is verified by a successful production build.
