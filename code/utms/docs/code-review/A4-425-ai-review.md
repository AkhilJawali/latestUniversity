# Frontend Faculty Availability & Preferences (A4-425)

Adds a per-faculty admin screen for declaring unavailability windows (hard) and scheduling preferences (soft), backed by the verified A4-5 routes under `/api/v1/faculty/{facultyId}`. Two TanStack Query hook modules (`useAvailabilityWindows`, `useFacultyPreferences`) wrap the plain-list windows CRUD and the upsert-only preferences. A bespoke `AvailabilityWindowModal` supplies `<input type="time">` fields the generic `ConfigFormModal` lacks; `PreferencesForm` is an inline upsert form; the page composes a faculty selector, the reused `ConfigTable`, and `ConfirmDeleteDialog`. The route is lazy-loaded and a nav item was added.

The contract is respected accurately: the windows list is read as a plain `{data:[...]}` (not paged), preferences use GET+PUT with no delete, the create/update request shapes match, and the constants mirror the backend allowlists (no `EVENING`). Zod validation mirrors the backend bounds and blocks invalid submits. All mutations have `onError` handling. No `dangerouslySetInnerHTML`, no secrets, no TypeScript.

Watch for: (1) the preferences form has no `onCancel`/Escape or dirty-reset path, and its success message persists across faculty switches (confirmed, minor); (2) the modal's day `<select>` and the two selects in the prefs form lack `aria-describedby` wiring to their error text (confirmed, a11y); (3) a genuine stale-state bug — the delete-confirm error and the preferences success message survive a faculty switch (confirmed). None are blocking.

**Verdict**: COMMENT

<details>
<summary>Issues (7)</summary>

1. **Prefs form stale success message on faculty switch** (confirmed) — the page renders `PreferencesForm` in a fixed tree slot with no `key`, so React reuses the instance across a faculty change; `message` ("Preferences saved.") and `errors` are not cleared, so the old status lingers against the new faculty until re-render. Clear `message`/`errors` on `facultyId` change, or add `key={selectedId}`.
2. **Delete-confirm error leaks across rows/faculty** (confirmed) — `deleteError` is reset only in `confirmDelete`/`cancelDelete`, not when a new `toDelete` is set or faculty switches; a failed delete then opening a different row's dialog shows the prior error string. Reset `deleteError` when opening a new delete target.
3. **Error text not programmatically associated** (confirmed) — fields set `aria-invalid` but the `<p className="field-error">` is not linked via `aria-describedby`, so screen readers don't announce the reason on focus. Applies to modal day/start/end/reason/note and both prefs selects.
4. **Prefs form has no cancel/reset affordance** (confirmed) — unlike the modal (Escape + Cancel), the inline prefs form can't revert edits to the loaded values. Minor UX; a reset button or reliance on refetch would help.
5. **Faculty selector capped at first page (~20)** (confirmed) — `useFacultyList({})` requests the default paginated page; faculties past page 1 are unreachable in the selector. Pre-existing shared pattern (faculty-management does the same), so out of scope for A4-425 — flagged: a typeahead or higher `size` is needed for real faculty counts.
6. **Third bespoke modal — duplication** (confirmed) — `AvailabilityWindowModal` repeats the `<dialog>` open/close effect, initialValues-seeding effect, `setField`/`err` helpers, and error-mapping submit flow already in `FacultyFormModal` and `ConfigFormModal`. Extract a shared modal shell / form-state hook before a 4th copy.
7. **`onCancel` vs `onClose` naming inconsistency** (confirmed) — the modal exposes `onClose` (correctly wired to `<dialog onCancel>`) while `ConfirmDeleteDialog` uses `onCancel`; inconsistent prop naming across two dialogs in the same feature. Cosmetic.

</details>

<details>
<summary>Details</summary>

## Contract conformance — accurate

The two hook modules match the verified A4-5 surface precisely. `useAvailabilityWindows` reads `GET /faculty/{id}/availability` as `.data` and the page consumes `windows.data?.data ?? []` as a plain array — correctly treating it as a non-paged list. `useCreateWindow`/`useUpdateWindow`/`useDeleteWindow` hit `POST`/`PUT {windowId}`/`DELETE {windowId}` and all invalidate the same list key. `useUpdateWindow` destructures `{ id, ...body }` so the id rides the URL, not the payload — matching the identical create/update request shape. `useFacultyPreferences` is GET + PUT only (no delete), consistent with the upsert-one-per-faculty rule, and the query is `enabled: facultyId != null` so it doesn't fire before a faculty is chosen.

The constants (`DAYS_OF_WEEK`, `TIME_OF_DAY` without `EVENING`, `SESSION_DISTRIBUTION`) mirror the backend allowlists, and the Zod enums are derived from those same arrays, so client validation can't drift from the mirrored list. There's no per-window hard/soft field anywhere — the hard/soft distinction is purely presentational (the red left-accent on the windows card, the "(soft)" heading on prefs), which is what the lead-approved design asked for.

## HH:mm end-after-start compare — correct

The concern about string-comparing times is worth stating precisely: it's safe *here* specifically because both operands are constrained to zero-padded `HH:mm` by `TIME_REGEX` before the compare runs, and the `superRefine` guards with `TIME_REGEX.test(...)` on both fields first. Lexicographic order equals chronological order for fixed-width `HH:mm`, so `endTime <= startTime` correctly rejects both "before" and "equal". The tests cover both the reversed and the equal case. This holds only within a single day (no overnight windows), which matches the backend's same-day model.

## Preferences seeding from the GET default — mostly correct, one stale-state edge

The backend always returns a `NO_PREFERENCE` default object, so the form is never empty. `PreferencesForm` initializes state to `NO_PREFERENCE`/`NO_PREFERENCE` and re-seeds in an effect keyed on `query.data`. That's the right shape. The gap is the surrounding transient state: `message` (the "Preferences saved." status) and `errors` are not cleared when `facultyId` changes. Because the page renders `PreferencesForm` in the same tree position for every faculty, React keeps the instance and its state across a selector change; the seeding effect updates `values` on the new faculty's data, but the old success message lingers until something else re-renders it away. Keying the form (`<PreferencesForm key={selectedId} ... />`) or resetting `message`/`errors` in a `facultyId` effect closes this cleanly.

## Stale state on faculty switch — the page side

The windows side is fine: the query key includes `selectedId`, and `rows` derives straight from the query, so switching faculty swaps the data. The mutation hooks are re-created per `selectedId` too. The one real leak is `deleteError`: it's page-level state reset only inside `confirmDelete` and `cancelDelete`. Sequence that shows it — attempt a delete that 422s (error stored), cancel, switch faculty, open a different window's delete dialog: the dialog label still carries the previous `deleteError` string because opening a target only sets `toDelete`, never clears `deleteError`. Clearing `deleteError` in the `onDelete`/`setToDelete` path (and on faculty change) fixes it.

## Delete confirm label — works, slightly awkward on error

The label composition is correct against `ConfirmDeleteDialog`'s `label` prop (verified: the dialog renders "Are you sure you want to delete {label}?"). On the happy path it reads naturally: "…delete the monday 09:00–11:00 window?". On error it becomes "…delete this window. Could not delete this window.?" — the trailing `?` after a period reads oddly. Functional, but the error-surfacing-through-the-label pattern (inherited from the prior story) produces awkward punctuation here; a dedicated error line in the dialog would read better. Not blocking.

## Error handling — complete across all four mutations

Window create/update route backend errors through the modal's `onError`: `mapApiError` field errors land on the inputs, non-field messages go to a `role="alert"` form message. Delete routes through the page's `onError` into the dialog label. Preferences upsert handles both `fields` (unlikely for 422 enum errors, but harmless) and `message`, defaulting to "Could not save preferences." The 422 allowlist path returns a `message` (no `details`), so `mapApiError` yields `{ message }` and it surfaces inline — correct. Every mutation has an error path; none can fail silently.

## Accessibility

Strengths: native `<dialog>` gives focus trap + Escape for free; labels are associated via `htmlFor`/`id` on every control; required fields marked; icon-free text buttons; `aria-invalid` toggled on error; the success message uses `role="status"` and the form error uses `role="alert"`.

Gaps, all non-blocking: error `<p>` nodes aren't linked with `aria-describedby`, so a screen reader focusing an invalid field won't read the reason. The prefs form's two selects have the same gap. The modal's `aria-label` on `<dialog>` duplicates the visible `<h2>`; using `aria-labelledby` pointing at the heading would be marginally better but the current form is acceptable.

## Security & standards

No `dangerouslySetInnerHTML`; all dynamic content renders as escaped text (including table cells via `ConfigTable`). No secrets or hardcoded URLs — everything goes through `apiClient` with the `/api/v1` baseURL. Plain JSX throughout, PropTypes on both components, correct import ordering (react → external → `@/` internal → feature-local → styles), UPPER_SNAKE_CASE constants, `use`-prefixed hook files. Component sizes: the modal is ~185 lines and the page ~180 — over the 150-line guideline but consistent with the sibling faculty-management files; the excess is form markup, not logic. The duplication note (issue 6) is the more actionable standards concern: this is the third hand-rolled `<dialog>` modal with the same open/close effect and seeding effect, and they've begun to diverge (prop naming, message handling). A shared modal shell or `useModalForm` hook would consolidate them.

## Routing

The lazy route `/master-data/faculty-availability` is registered with a `Suspense` fallback matching the sibling routes, and the nav item is added to `AppShell` in a sensible position after "Faculty". The page is reachable and the nav link participates in active-state styling. Routing is complete.

</details>

<details>
<summary>Files reviewed</summary>

- `features/master-data/faculty-availability/constants/availability-options.js` — allowlist mirrors; matches backend
- `features/master-data/faculty-availability/schemas/availability-schemas.js` — Zod schemas; HH:mm compare safe
- `features/master-data/faculty-availability/schemas/availability-schemas.test.js` — 14 tests, cover reversed/equal time + bounds
- `features/master-data/faculty-availability/api/useAvailabilityWindows.js` — plain-list CRUD, correct keys
- `features/master-data/faculty-availability/api/useFacultyPreferences.js` — GET + PUT upsert, enabled-guarded
- `features/master-data/faculty-availability/components/AvailabilityWindowModal.jsx` — bespoke time modal; a11y describedby gap; duplication
- `features/master-data/faculty-availability/components/PreferencesForm.jsx` — seeding correct; stale message on faculty switch
- `features/master-data/faculty-availability/pages/FacultyAvailabilityPage.jsx` — composition; `deleteError` stale-state leak; selector page cap
- `features/master-data/faculty-availability/faculty-availability.css` — layout only
- `app/router.jsx` — lazy route added correctly
- `components/layout/AppShell.jsx` — nav item added

Supporting (reused, unchanged) verified: `scheduling-config/schemas/config-schemas.js` (`validateWith`), `scheduling-config/components/ConfirmDeleteDialog.jsx` (`label` prop), `scheduling-config/components/ConfigTable.jsx`, `lib/api-error.js` (`mapApiError`), `faculty-management/api/useFaculty.js` (`useFacultyList` — paginated).

</details>
