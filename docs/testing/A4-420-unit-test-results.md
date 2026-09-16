# A4-420 — Frontend Faculty Profile Management: Unit Test Results

- **Story:** A4-420 — Frontend Faculty Profile Management (CRUD)
- **Unit Test subtask:** A4-465
- **Date:** 2026-09-04
- **Test runner:** Vitest 2.1.4 (jsdom environment)
- **Scope:** Client-side Zod validation schemas and the list-filter param builder for the faculty-management feature.

## Test Command

```
node node_modules/vitest/vitest.mjs run src/features/master-data/faculty-management/schemas/faculty-schemas.test.js
```

Full-suite regression check:

```
node node_modules/vitest/vitest.mjs run
```

## Results Summary

| Scope | Test Files | Tests | Result |
|-------|-----------|-------|--------|
| faculty-schemas.test.js (this story) | 1 | 18 | ✅ all passed |
| Full frontend suite (regression) | 15 | 102 | ✅ all passed |

> Note on exit code: the full-suite run reports OS exit code 1 due to a pre-existing
> intentional PropType warning in the unrelated `OutcomeBanner.test.jsx` (A4-345). The
> Vitest summary is the source of truth: "102 passed (102)". No failures.

## Test Coverage by Suite

### `facultyCreateSchema` (12 tests) — FR-2.2 / AC-5
Mirrors the A4-4 `CreateFacultyRequest` bounds:
- accepts a valid faculty and coerces numbers (homeDepartmentId, loads, campusIds)
- accepts a valid faculty with the optional loads omitted (blank → undefined)
- rejects a missing name / a name over 200 characters
- rejects a missing identifier / an identifier over 50 characters
- rejects an invalid designation (outside the 7 fixed values)
- rejects a missing qualification
- rejects a missing home department
- rejects an empty campus list (campusIds must have ≥1)
- rejects a max load below 0.1
- rejects min load greater than max load (cross-field refine)

### `facultyEditSchema` (3 tests) — FR-3 / PD-6
Mirrors the A4-4 `UpdateFacultyRequest` (identifier/campuses/competencies absent):
- accepts a valid edit payload and confirms identifier/campusIds/competencyCourseIds are not in the parsed output
- rejects an empty name
- rejects min load greater than max load

### `buildFacultyListParams` (3 tests) — FR-1.2 / FR-1.3
- omits empty filters
- includes only set filters, coercing ids to numbers (departmentId, competencyCourseId)
- passes page and size through when provided

## Issues Found

None. All 18 unit tests pass on first green run. No regressions (full suite 84 → 102 with this story's additions).

## Traceability

| Requirement | Covered by |
|-------------|-----------|
| FR-1.2/1.3 (filters) | `buildFacultyListParams` suite |
| FR-2.2 / AC-5 (create validation blocks invalid submit) | `facultyCreateSchema` suite |
| FR-3 / PD-6 (edit omits immutable identifier/campuses/competencies) | `facultyEditSchema` suite |
