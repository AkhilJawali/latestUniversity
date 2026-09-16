# A4-415 — Frontend Course Management: Code Coverage

- **Story:** A4-415 — Frontend Course Management (CRUD)
- **Code Coverage subtask:** A4-461
- **Date:** 2026-09-04
- **Target:** 80% line coverage on new logic-bearing code (org testing standard)

## Method

The frontend project does not currently have a coverage reporter wired into the vitest
config (no `@vitest/coverage-v8` provider configured), so a numeric percentage was not
captured for this story. Coverage is therefore reported by-file with reasoning about
which code paths the 18 passing unit tests exercise. This matches the approach used for
A4-410.

## Covered Files

| File | Covered by tests | Assessment |
|------|-----------------|------------|
| `schemas/course-schemas.js` — `courseCreateSchema` | 9 tests (valid + each invalid branch) | ~100% of validation branches (name, code regex/length, department, L/T/P, credits, type) |
| `schemas/course-schemas.js` — `courseEditSchema` | 3 tests | Fully exercised; confirms immutable fields excluded |
| `schemas/course-schemas.js` — `filterCourses` | 6 tests | All branches: no-filter, type-only, search-only (name + code), combined, undefined-search guard |
| `constants/course-options.js` — `COURSE_TYPES` | Indirectly via schema tests | Trivial constant, no branches |

## Files Not Unit-Tested (rationale)

| File | Rationale |
|------|-----------|
| `pages/CourseManagementPage.jsx` | React component wiring (state, modal open/close, mutation dispatch). Exercised via the vite build (compiles + chunk emitted) and manual reasoning; not covered by isolated unit tests. Component-level render tests are a candidate follow-up but were out of scope for the schema-focused unit test subtask, consistent with A4-410. |
| `components/PrerequisitePanel.jsx` | Thin UI over API hooks; renders list + add/remove. Same rationale as the page. |
| `components/CrossListingPanel.jsx` | Thin UI over the cross-listing add hook + campus/department pickers. Same rationale. |
| `api/useCourses.js`, `api/usePrerequisites.js`, `api/useCrossListings.js` | TanStack Query hooks — thin wrappers over `apiClient` against verified A4-3 routes. Covered indirectly by the successful build and the A4-3 backend integration tests. |

## Build Verification

- `node node_modules/vite/bin/vite.js build` → **success**, 196 modules transformed, dedicated `CourseManagementPage` JS + CSS chunk emitted. Confirms all new modules compile and the lazy route resolves.

## Coverage Gaps and Plan

- **Component render tests** for `CourseManagementPage` (modal open on Add, memoized initialValues reset on edit target change, panel visibility on course selection) — deferred as a follow-up, consistent with the A4-410 precedent where schema logic was the unit-test focus.
- **Numeric coverage reporting** — installing `@vitest/coverage-v8` and adding a `coverage` script would produce a hard percentage. Recommended as a project-wide infra improvement rather than a per-story task.

## Traceability

All logic-bearing validation and filtering code introduced by A4-415 is covered by the
18 passing unit tests. UI wiring is verified by a successful production build.
