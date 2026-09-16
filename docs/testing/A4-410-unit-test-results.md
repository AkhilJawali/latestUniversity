# A4-410 — Frontend Campus Hierarchy (CRUD) — Unit Test Results

| | |
|---|---|
| **Story** | A4-410 |
| **Unit Test subtask** | A4-455 |
| **Framework** | Vitest 2.1.4 + @testing-library/react (jsdom) |
| **Command** | `node node_modules/vitest/vitest.mjs run src/features/master-data/campus-hierarchy` |
| **Result** | PASS — 14 passed / 14 total, 0 failures |

## Summary

Unit tests cover the client-side Zod validation for all five hierarchy entities — the layer that enforces FR-3.2 (validate before submit) and AC-5 (invalid input blocked, no request sent). Validation is exercised through the shared `validateWith` helper (from A4-340), matching the established convention.

## Test file

`frontend/src/features/master-data/campus-hierarchy/schemas/hierarchy-schemas.test.js`

## Test breakdown (14 tests)

| Entity | Test | Type |
|--------|------|------|
| Campus | accepts a valid campus | happy path |
| Campus | rejects a missing name (AC-5) | error |
| Campus | rejects a code with illegal characters | error/edge |
| Campus | rejects a code over 20 characters | boundary |
| Department | accepts a valid department | happy path |
| Department | rejects a missing parent campus (AC-5) | error |
| Program | accepts a valid program | happy path |
| Program | rejects a non-positive duration | boundary |
| Program | rejects a missing degree type | error |
| Batch | accepts a valid batch (no elective basket) | happy path |
| Batch | accepts a valid batch with elective basket | happy path |
| Batch | rejects strength below 1 | boundary |
| Section | accepts a valid section | happy path |
| Section | rejects missing identifier + non-positive sub-strength | error/edge |

## Requirement coverage

- **FR-3.2 / AC-5** (Zod validation blocks invalid submits): covered for every entity (name/identifier required, numeric bounds, code format, parent-id required).
- **FR-3 field sets**: each schema matches the A4-2 `Create*Request` bounds (campus code regex/<=20, batch strength >=1, program duration >0, etc.).

## Build verification

- `vite build` — success, 187 modules transformed, `CampusHierarchyPage` chunk emitted (14.43 kB). All feature imports/JSX compile.

## Notes

- The reused presentational components (`ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog`) already carry their own unit tests under A4-340 (`ConfigTable.test.jsx`, `ConfigFormModal.test.jsx`, `ConfirmDeleteDialog.test.jsx`) — unchanged and still passing, so their behavior (loading/empty/error states, Zod-on-submit, 400->field / 409->message mapping, focus-trap dialog) is covered by the existing suite and consumed as-is here.
- API hooks are thin wrappers over `apiClient` + TanStack Query; their correctness (endpoint paths, parent-filter params, Section non-paged path) is validated end-to-end in the Testing phase (A4-414) against the running backend.
