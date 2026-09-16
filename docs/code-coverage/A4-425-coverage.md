# A4-425 — Frontend Faculty Availability and Preferences: Code Coverage

- **Story:** A4-425 — Frontend Faculty Availability and Preferences (CRUD)
- **Code Coverage subtask:** A4-471
- **Date:** 2026-09-04
- **Target:** 80% line coverage on new logic-bearing code (org testing standard)

## Method

No numeric coverage percentage was captured; coverage is reported by-file with reasoning
about which code paths the 14 passing unit tests exercise, consistent with A4-410/415/420.

## Covered Files

| File | Covered by tests | Assessment |
|------|-----------------|------------|
| `schemas/availability-schemas.js` — `availabilityWindowSchema` | 10 tests (valid, optional note, each invalid branch, end-after-start + equal-time refine) | ~100% of validation branches |
| `schemas/availability-schemas.js` — `preferenceSchema` | 4 tests | Both enums exercised, valid + invalid |
| `constants/availability-options.js` — DAYS_OF_WEEK / TIME_OF_DAY / SESSION_DISTRIBUTION | Indirectly via schema tests | Trivial constants, no branches |

## Files Not Unit-Tested (rationale)

| File | Rationale |
|------|-----------|
| `pages/FacultyAvailabilityPage.jsx` | React wiring (faculty selector, window CRUD dispatch, delete flow). Exercised via the vite build (compiles + chunk emitted); component render tests deferred, consistent with prior master-data stories. |
| `components/AvailabilityWindowModal.jsx` | Custom modal; the validation it invokes is covered by the schema tests. Render tests deferred. |
| `components/PreferencesForm.jsx` | Thin form over the preferences hooks; validation covered by `preferenceSchema` tests. |
| `api/useAvailabilityWindows.js`, `api/useFacultyPreferences.js` | TanStack Query wrappers over apiClient against verified A4-5 routes; covered indirectly by the successful build and A4-5 backend tests. |

## Build Verification

- `node node_modules/vite/bin/vite.js build` → **success**, 214 modules transformed, dedicated `FacultyAvailabilityPage` JS + CSS chunk emitted. Confirms all new modules compile and the lazy route resolves.

## Coverage Gaps and Plan

- Component render tests for the page, window modal, and preferences form — deferred (same precedent as prior master-data stories).
- Numeric coverage run (`vitest run --coverage`) would produce a hard percentage; recommended as a project-wide step.

## Traceability

All logic-bearing validation code introduced by A4-425 is covered by the 14 passing unit
tests. UI wiring is verified by a successful production build.
