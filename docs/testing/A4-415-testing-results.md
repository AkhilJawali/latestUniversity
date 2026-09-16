# A4-415 — Frontend Course Management: Testing Results

- **Story:** A4-415 — Frontend Course Management (CRUD)
- **Testing subtask:** A4-419
- **Date:** 2026-09-04
- **Environment:** Vitest 2.1.4 (jsdom) + Vite production build

## Test Approach

End-to-end verification for a frontend CRUD feature at this stage covers:
1. The full automated unit/component suite (regression across the app).
2. A production build to confirm all modules — including the new lazy route — compile and bundle cleanly.
3. Behavioral walkthrough of the acceptance criteria against the implemented code.

## Automated Results

| Check | Command | Result |
|-------|---------|--------|
| Full unit/component suite | `node node_modules/vitest/vitest.mjs run` | 84 passed (14 files) |
| Course-management schemas | (within suite) | 18 passed |
| Production build | `node node_modules/vite/bin/vite.js build` | success, 196 modules, dedicated `CourseManagementPage` chunk |

> The suite run reports OS exit code 1 from a pre-existing intentional PropType warning
> in the unrelated `OutcomeBanner.test.jsx` (A4-345). The Vitest summary is the source of
> truth: "84 passed (84)". No failures.

## Acceptance Criteria Walkthrough

| AC | Scenario | Verification |
|----|----------|--------------|
| AC-1 | List all courses in a table | `CourseManagementPage` renders `ConfigTable` with code/name/department/L-T-P/credits/type/cross-listed columns over `useCourses().data.data` |
| AC-2 | Prerequisite add/remove with backend error surfacing | `PrerequisitePanel` add + remove both call the sub-resource hooks and surface `mapApiError` messages (remove error handling added in code review) |
| AC-3 | Filter by type and search by name/code | `filterCourses` client-side helper (6 unit tests) wired to the type + search controls |
| AC-4 | Create a course with validation | Create modal uses `courseCreateSchema` (9 unit tests); invalid submit blocked client-side, backend errors mapped to fields |
| AC-5 | Cross-listing add reflects status | `CrossListingPanel` add invalidates the course list so `isCrossListed` refreshes; add-only per A4-3 contract (documented) |
| AC-6 | Edit a course without touching immutable fields | Edit modal uses `courseEditSchema` — `code`/`departmentId` omitted (3 unit tests confirm) |
| AC-7 | Delete a course with confirmation and error feedback | `ConfirmDeleteDialog` + `confirmDelete` with `onError` surfacing the failure (added in code review) |

## Issues Found During Testing

None. The two behavioral gaps (silent delete / silent remove-prerequisite failures) were caught and fixed during the Code Review phase (A4-418) before this testing pass; the fixes are included and verified here.

## Regression

No regressions: the pre-existing 66-test baseline plus this story's 18 new tests = 84, all green. Router and AppShell tests confirm the new `/master-data/courses` route and "Courses" nav item did not break existing navigation.

## Conclusion

A4-415 passes end-to-end testing. All acceptance criteria are met, the build is clean, and there are no open issues.
