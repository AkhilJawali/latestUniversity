# A4-415 — Frontend Course Management: Unit Test Results

- **Story:** A4-415 — Frontend Course Management (CRUD)
- **Unit Test subtask:** A4-460
- **Date:** 2026-09-04
- **Test runner:** Vitest 2.1.4 (jsdom environment)
- **Scope:** Client-side Zod validation schemas and the client-side filter helper for the course-management feature.

## Test Command

```
node node_modules/vitest/vitest.mjs run src/features/master-data/course-management/schemas/course-schemas.test.js
```

Full-suite regression check:

```
node node_modules/vitest/vitest.mjs run
```

## Results Summary

| Scope | Test Files | Tests | Result |
|-------|-----------|-------|--------|
| course-schemas.test.js (this story) | 1 | 18 | ✅ all passed |
| Full frontend suite (regression) | 14 | 84 | ✅ all passed |

> Note on exit code: the full-suite run reports OS exit code 1 due to a pre-existing
> intentional PropType warning logged by `OutcomeBanner.test.jsx` (an unrelated A4-345
> test that deliberately renders an invalid `status` to assert the fallback). This is a
> PowerShell stderr-capture artefact, not a test failure — the Vitest summary reports
> "84 passed (84)". This matches the A4-410 baseline behaviour.

## Test Coverage by Suite

### `courseCreateSchema` (9 tests) — FR-2.2 / AC-6
Mirrors the A4-3 `CreateCourseRequest` bounds:
- accepts a valid course and coerces string form inputs to numbers (departmentId, L/T/P, credits)
- rejects a missing name
- rejects a name over 200 characters
- rejects a code with illegal characters (regex `^[A-Za-z0-9_-]+$`)
- rejects a code over 20 characters
- rejects a missing department
- rejects negative L-T-P hours
- rejects credits below 0.1
- rejects an invalid course type (outside CORE/ELECTIVE/AUDIT)

### `courseEditSchema` (3 tests) — FR-3 / PD-5
Mirrors the A4-3 `UpdateCourseRequest` (code + departmentId immutable, absent from schema):
- accepts a valid edit payload and confirms `code`/`departmentId` are not present in parsed output
- rejects an empty name
- rejects credits below 0.1

### `filterCourses` (6 tests) — FR-1.2 / PD-1
Client-side filtering over the fetched global course list:
- returns all rows when no filter is applied
- filters by course type
- filters by a case-insensitive name search
- filters by a code search
- applies type and search together
- tolerates an undefined search term

## Issues Found

None. All 18 unit tests pass on first green run. No regressions in the existing 66-test baseline (now 84 with this story's additions).

## Traceability

| Requirement | Covered by |
|-------------|-----------|
| FR-1.2 (type + search filter) | `filterCourses` suite |
| FR-2.2 / AC-6 (create validation blocks invalid submit) | `courseCreateSchema` suite |
| FR-3 / PD-5 (edit omits immutable code + departmentId) | `courseEditSchema` suite |
