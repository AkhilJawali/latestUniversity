# A4-425 — Frontend Faculty Availability and Preferences: Code Review

- **Story:** A4-425 — Frontend Faculty Availability and Preferences (CRUD)
- **Code Review subtask:** A4-428
- **Date:** 2026-09-04
- **Reviewer:** AI semantic review (`semantic_reviewer`) — pre-human-review pass per squad rules
- **Scope:** `features/master-data/faculty-availability/**`, `app/router.jsx`, `components/layout/AppShell.jsx`

## Final Verdict

**COMMENT** — no blocking issues. Two confirmed stale-state bugs and one a11y gap were fixed proactively (they match the reviewer's recommended pre-review fixes); the remaining items are accepted or tracked as follow-ups.

## Issues Found and Resolution

### Fixed (confirmed bugs + a11y)

| # | Issue | File | Fix |
|---|-------|------|-----|
| 1 | PreferencesForm kept its "saved" message / errors across a faculty switch (rendered in a fixed tree slot, no key) | `FacultyAvailabilityPage.jsx` | Added `key={selectedId}` so the form remounts (clean state) on faculty change |
| 2 | `deleteError` leaked across delete targets / faculty (reset only in confirm/cancel) | `FacultyAvailabilityPage.jsx` | Reset `deleteError` when a new delete target is opened |
| 3 | Field error text not programmatically associated (screen readers don't announce the reason on focus) | `AvailabilityWindowModal.jsx` | Added `aria-describedby` on day/start/end/reason/note inputs → their `id`'d error `<p>` |

### Accepted / tracked as follow-ups

| # | Observation | Disposition |
|---|-------------|-------------|
| 4 | Preferences form has no cancel/reset affordance | Accepted — minor UX; refetch reverts on reload. Optional enhancement. |
| 5 | Faculty selector shows only the first paginated page (~20) | Tracked follow-up — pre-existing shared pattern (faculty-management does the same); a typeahead / higher size is a cross-story improvement, out of scope for A4-425. |
| 6 | Third bespoke `<dialog>` modal (AvailabilityWindowModal / FacultyFormModal / ConfigFormModal) — duplication | Tracked follow-up — extract a shared modal shell / `useModalForm` hook before a 4th copy. |
| 7 | `onClose` vs `onCancel` prop-naming inconsistency between the two dialogs | Cosmetic; accepted. |
| — | Delete-error surfaced via the dialog `label` produces slightly awkward punctuation on error | Accepted — inherited pattern; a dedicated error line in the shared dialog would read better (ties into #6). |

## Confirmed Positives

- Contract fidelity: windows read as a plain non-paged `{data:[...]}`; preferences GET+PUT upsert with no delete; identical create/update request shapes with the id in the URL; constants mirror the backend allowlists (no EVENING).
- The `HH:mm` end-after-start string compare is safe — `TIME_REGEX` forces fixed-width zero-padded values before the compare, and the refine guards both fields; rejects both "before" and "equal".
- Preferences seeding from the backend's NO_PREFERENCE default is correct (form never empty).
- Error handling complete across all four mutations (window create/update/delete + preferences upsert); none can fail silently.
- Delete surfaces `mapApiError().message`; page memoizes `initialValues`; window times trimmed to `HH:mm` on edit.
- Security clean (no `dangerouslySetInnerHTML`, no secrets, escaped JSX); no TypeScript; PropTypes present; import order correct; routing complete.

## Verification After Fixes

- `node node_modules/vite/bin/vite.js build` → success, 214 modules, dedicated `FacultyAvailabilityPage` chunk.
- Availability schema tests green (14); full suite unaffected (116).
