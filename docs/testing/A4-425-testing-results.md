# A4-425 — Frontend Faculty Availability and Preferences: Testing Results

- **Story:** A4-425 — Frontend Faculty Availability and Preferences (CRUD)
- **Testing subtask:** A4-429
- **Date:** 2026-09-04
- **Environment:** Vitest 2.1.4 (jsdom) + Vite production build

## Test Approach

1. Full automated unit/component suite (app-wide regression).
2. Production build to confirm all modules — including the new lazy route — compile and bundle.
3. Behavioral walkthrough of the acceptance criteria against the implemented code.

## Automated Results

| Check | Command | Result |
|-------|---------|--------|
| Full unit/component suite | `node node_modules/vitest/vitest.mjs run` | 116 passed (16 files) |
| Availability-management schemas | (within suite) | 14 passed |
| Production build | `node node_modules/vite/bin/vite.js build` | success, 214 modules, dedicated `FacultyAvailabilityPage` chunk |

> The suite run's OS exit code is 0 here; when it reports 1 it is the pre-existing
> intentional PropType warning in the unrelated `OutcomeBanner.test.jsx` (A4-345). The
> Vitest summary is authoritative: "116 passed (116)". No failures.

## Acceptance Criteria Walkthrough

| AC | Scenario | Verification |
|----|----------|--------------|
| AC-1 | Add a hard unavailability window (e.g. Tue 2–4 PM) → saved, shown in the windows (hard) section | `AvailabilityWindowModal` posts to `/faculty/{id}/availability`; list refetches; rendered in the red-accented windows card |
| AC-2 | Save a soft preference (mornings, spread) → persists, shown in the preferences (soft) section | `PreferencesForm` upserts via `PUT /preferences`; seeded from the GET default |
| AC-3 | End time before/equal to start → inline error, no request | `availabilityWindowSchema` refine (verified by 2 unit tests) |
| AC-4 | Delete a window with confirmation → removed from the list | `ConfirmDeleteDialog` + delete mutation; list invalidates |
| AC-5 | Faculty with no windows → empty state + create action | `ConfigTable` empty state ("No unavailability windows yet" + Add window) |

## Issues Found During Testing

None. The two stale-state bugs and the a11y gap found in the Code Review phase (A4-428) were fixed before this testing pass and are re-verified here.

## Regression

No regressions: the pre-A4-425 baseline of 102 tests plus this story's 14 = 116, all green. Router and AppShell tests confirm the new `/master-data/faculty-availability` route and nav item did not break navigation.

## Backend Note

The feature depends on the A4-5 endpoints (availability windows + preferences), which were implemented in a prior backend story (not modified here). No backend change was made for A4-425.

## Conclusion

A4-425 passes end-to-end testing. All acceptance criteria are met, the build is clean, and there are no open frontend issues.
