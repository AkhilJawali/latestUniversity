# A4-425 — Frontend Faculty Availability and Preferences: Unit Test Results

- **Story:** A4-425 — Frontend Faculty Availability and Preferences (CRUD)
- **Unit Test subtask:** A4-470
- **Date:** 2026-09-04
- **Test runner:** Vitest 2.1.4 (jsdom environment)
- **Scope:** Client-side Zod validation schemas for the faculty-availability feature.

## Test Command

```
node node_modules/vitest/vitest.mjs run src/features/master-data/faculty-availability/schemas/availability-schemas.test.js
```

Full-suite regression check:

```
node node_modules/vitest/vitest.mjs run
```

## Results Summary

| Scope | Test Files | Tests | Result |
|-------|-----------|-------|--------|
| availability-schemas.test.js (this story) | 1 | 14 | ✅ all passed |
| Full frontend suite (regression) | 16 | 116 | ✅ all passed |

> Note on exit code: the full-suite run reports OS exit code 1 due to a pre-existing
> intentional PropType warning in the unrelated `OutcomeBanner.test.jsx` (A4-345). The
> Vitest summary is authoritative: "116 passed (116)". No failures.

## Test Coverage by Suite

### `availabilityWindowSchema` (10 tests) — FR-3.1 / AC-3
Mirrors the A4-5 Create/UpdateAvailabilityWindowRequest bounds:
- accepts a valid window; accepts a valid window with the note omitted
- rejects an invalid day of week (outside the 7 values)
- rejects a malformed start time (e.g. 25:00) and a missing start time
- rejects end time before start time, and end time equal to start time (must be strictly after)
- rejects a missing reason code and a reason code over 50 characters
- rejects a note over 500 characters

### `preferenceSchema` (4 tests) — FR-4
Mirrors the A4-5 SetPreferenceRequest allowlists:
- accepts valid preferences; accepts NO_PREFERENCE for both
- rejects an unsupported time-of-day (EVENING — not in the backend allowlist, A425-OQ-4)
- rejects an invalid session distribution

## Issues Found

None. All 14 unit tests pass on first green run. No regressions (full suite 102 → 116 with this story's additions).

## Traceability

| Requirement | Covered by |
|-------------|-----------|
| FR-3.1 / AC-3 (window validation, end-after-start) | `availabilityWindowSchema` suite |
| FR-4 (preference allowlists) | `preferenceSchema` suite |
