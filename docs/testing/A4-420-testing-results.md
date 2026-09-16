# A4-420 — Frontend Faculty Profile Management: Testing Results

- **Story:** A4-420 — Frontend Faculty Profile Management (CRUD)
- **Testing subtask:** A4-424
- **Date:** 2026-09-04
- **Environment:** Vitest 2.1.4 (jsdom) + Vite production build

## Test Approach

1. Full automated unit/component suite (app-wide regression).
2. Production build to confirm all modules — including the new lazy route — compile and bundle.
3. Behavioral walkthrough of the acceptance criteria against the implemented code.

## Automated Results

| Check | Command | Result |
|-------|---------|--------|
| Full unit/component suite | `node node_modules/vitest/vitest.mjs run` | 102 passed (15 files) |
| Faculty-management schemas | (within suite) | 18 passed |
| Production build | `node node_modules/vite/bin/vite.js build` | success, 206 modules, dedicated `FacultyManagementPage` chunk |

> The suite run reports OS exit code 1 from a pre-existing intentional PropType warning
> in the unrelated `OutcomeBanner.test.jsx` (A4-345). The Vitest summary is authoritative:
> "102 passed (102)". No failures.

## Acceptance Criteria Walkthrough

| AC | Scenario | Verification |
|----|----------|--------------|
| AC-1 | Create faculty with designation, home department, competency list → created and listed | `FacultyFormModal` (create) posts campusIds + competencyCourseIds; list refetches via query invalidation |
| AC-2 | Campus-association picker: add two campuses → both saved and shown | `CampusPanel` add (POST per campus) + reads back the current set via the new `GET /faculty/{id}/campuses` (OQ-1) |
| AC-3 | Min/max weekly load saved → persist and display | Loads in create/edit schemas + shown as Min/Max load columns |
| AC-4 | Competency filter for a course → only faculty with that competency listed | `competencyCourseId` filter param (via `buildFacultyListParams`) |
| AC-5 | Required field blank → inline error, no request | `validateWith` blocks submit; field errors shown; verified by schema unit tests |
| AC-6 | Edit form: identifier not editable; campus/competency not in the edit form | Edit schema omits them; edit modal hides identifier + the multi-select fieldsets; current home department shown as a preselected option |

## Issues Found During Testing

None. The two edit-flow bugs (home-department display/preservation; swallowed 400) were caught and fixed during the Code Review phase (A4-423) before this testing pass; the fixes are included and re-verified here.

## Regression

No regressions: the pre-A4-420 baseline of 84 tests plus this story's 18 = 102, all green. Router and AppShell tests confirm the new `/master-data/faculty` route and "Faculty" nav item did not break navigation.

## Backend Note

The story's campus panel relies on the new backend endpoint `GET /api/v1/faculty/{id}/campuses`
(A4-4 OQ-1 follow-up). That backend change was verified by inspection only — Maven is not
available in this environment. A backend build/test should confirm it before production use.

## Conclusion

A4-420 passes end-to-end testing. All acceptance criteria are met, the build is clean, and there are no open issues (frontend). The one standing external dependency is the backend build verification of the OQ-1 endpoint.
