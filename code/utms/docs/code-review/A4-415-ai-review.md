# Course Management admin page (A4-415)

Adds a Course Management feature to the master-data area: a global course table with client-side type/search filtering, create/edit/delete through the reused scheduling-config modal + confirm dialog, and two sub-panels (prerequisites, cross-listings) that appear when a course is selected. Data access goes through three new TanStack Query hook modules against the verified A4-3 routes. A lazy route (`/master-data/courses`) and a "Courses" nav item wire it into the shell. The edit path correctly drops `code`/`departmentId` (immutable in A4-3), and cross-listing removal is intentionally not surfaced because no list endpoint exists.

Watch for: two silent mutation failures — delete-course and remove-prerequisite both lack `onError`, so a rejected request gives the user no feedback (confirmed); transient panel state (`picked`/`message`/`departmentId`) persists when the selected course changes (likely); and a redundant `nameOf`/candidate computation that reruns every render (minor). No XSS vectors, no secrets, no TypeScript; routing and accessibility are solid.

**Verdict**: NEEDS_CHANGES

## High-level view

The feature is well structured and reuses the established scheduling-config building blocks (`ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog`, `validateWith`, `mapApiError`) rather than reinventing them. The API layer matches the A4-3 contract precisely: global list with no server filter, create/update/delete on `/courses`, sub-resource POST/DELETE for prerequisites and cross-listings, and course-list invalidation to refresh the `isCrossListed` boolean. Schemas mirror the backend bounds and are well tested.

The blocking theme is error-handling asymmetry across the mutation surfaces. Create/update route backend errors into the modal via `mapApiError`, and add-prerequisite/add-cross-listing surface an inline message — but **delete-course** and **remove-prerequisite** have no `onError`, so a rejected request (409 in-use, 404, 500) does nothing visible. Two silent failure sites, same root cause.

A second, subtler behavioral concern is transient panel state when the user switches the selected course. The panels stay mounted across a course switch, so `picked`, `departmentId`, and a lingering `message` carry from one course into another. The prerequisite *query* itself is correctly keyed by `course.id` and refetches; this is purely local form/message state.

Accessibility is genuinely good: every control has an associated label, icon-only remove buttons carry `aria-label`, panels use `aria-label`ed `<section>`s, messages use `role="alert"`/`role="status"`, and both dialogs rely on native `<dialog>` for focus trap + Escape. Routing is complete and consistent with the existing lazy-route pattern. No security issues.

<details>
<summary>Issues (7)</summary>

1. **Delete-course has no error handler** — `useDeleteCourse` is called with only `onSuccess`; a failed delete leaves the confirm dialog open with no message and no toast. Add `onError` surfacing `mapApiError(err).message`. (confirmed, blocking)
2. **Remove-prerequisite has no error handler** — `remove.mutate(id)` in `PrerequisitePanel` passes no `onError`; a rejected removal (e.g., 409/500) is silent while add-prerequisite errors are shown. Add symmetric `onError` → `setMessage`. (confirmed, blocking)
3. **Stale panel state on course switch** — `PrerequisitePanel`/`CrossListingPanel` keep `picked`/`departmentId`/`message` when the selected course changes, so a message or half-entered selection carries into a different course's panel. Add `key={course.id}` on the panels (or reset on `course.id` change). (likely)
4. **`nameOf` / `candidates` recomputed every render** — `allCourses.find(...)` runs per prerequisite chip per render and the candidate `.filter` runs every render. Fine at current scale but wasteful; memoize `candidates` and build an id→course map. (confirmed, minor / non-blocking)
5. **Add-panel 400 falls back to generic message** — `mapApiError` returns `{ fields }` (no `message`) for a field-level 400, so `mapApiError(err).message ?? '…'` shows the generic fallback instead of the field message. Acceptable if these endpoints don't return 400 details; note the limitation. (possible)
6. **Success message never clears on cross-listing** — `CrossListingPanel` sets `"Cross-listing added."` and leaves it until the next submit; switching campus/department does not clear it. Minor polish; clear `message` on select change. (confirmed, minor)
7. **No toast on mutation success** — ui-standards calls for save/delete confirmations via toast; the feature relies on modal close + list refresh only. Consistent with the reused scheduling-config pages, so acceptable as-is, but note the standards gap. (possible, non-blocking)

</details>

<details>
<summary>Details</summary>

### Silent mutation failures — the blocking theme

`CourseManagementPage.confirmDelete`:

```js
const confirmDelete = () => {
  deleteMut.mutate(toDelete.id, {
    onSuccess: () => {
      if (selected?.id === toDelete.id) setSelected(null);
      setToDelete(null);
    },
  });
};
```

There is no `onError`. If the backend rejects the delete — 409 because the course is referenced as a prerequisite or is scheduled, 404, or 500 — the mutation settles in error, `onSuccess` never runs, and `toDelete` is never cleared. The Delete button re-enables (isPending flips back) but nothing tells the user what happened.

The same pattern repeats in `PrerequisitePanel`:

```js
onClick={() => remove.mutate(id)}
```

Remove-prerequisite passes no handlers at all, so a failed removal is equally silent — and it sits right next to add-prerequisite, which *does* surface `mapApiError(err).message`. That asymmetry within a single component is the clearest signal this is an oversight rather than a decision.

The create/update path shows the pattern to follow: `submitForm` spreads the modal's `handlers` (which carries `onError`) and only overrides `onSuccess`, so the modal's `mapApiError`-based error rendering stays intact:

```js
updateMut.mutate({ id: editing.id, ...data }, { ...handlers, onSuccess: closeModal });
```

Delete should surface `mapApiError(err).message` in a `role="alert"` region (keeping the dialog open), and remove-prerequisite should set its panel `message` the same way add does. Fixing both moves the review toward APPROVED.

### Selected-course state and freshness

The page keeps `selected` as a captured row and re-derives the live row each render:

```js
const selectedCourse = selected ? allCourses.find((c) => c.id === selected.id) ?? null : null;
```

That's the right instinct — it keeps the panel course fresh across list refetches and collapses the panels if the selected course disappears after a refetch. Good.

The gap is the panels' own local state. `PrerequisitePanel` holds `picked` and `message`; `CrossListingPanel` holds `campusId`, `departmentId`, and `message`. When the user picks a different course in the "Manage … for" selector, `selectedCourse` changes identity but the panels stay mounted, so that transient state persists. Concretely: add a prerequisite that the backend rejects with "would create a cycle", see the message, then switch courses — the message still shows against a course it doesn't apply to. Adding `key={course.id}` to each panel forces a clean remount on switch.

One related note: the "Manage … for" selector iterates `visibleRows` (the filtered set) while the panels operate on `allCourses`. If a user selects a course, then narrows the filter so that course drops out of `visibleRows`, the selector's value falls back to empty but `selected` still points at it, so the panels remain visible for a course no longer in the dropdown. Not a bug per se (the panels correctly track the live row), but the selector and panel visibility can disagree. Worth a deliberate decision.

### Redundant per-render computation in PrerequisitePanel

```js
const nameOf = (id) => {
  const c = allCourses.find((x) => x.id === id);
  return c ? `${c.code} — ${c.name}` : `Course #${id}`;
};
const candidates = allCourses.filter((c) => c.id !== course.id && !prereqIds.includes(c.id));
```

`nameOf` is called twice per chip (label + `aria-label`), each a linear scan, and `candidates` filters the full list every render with an `includes` per element (O(n·m)). Harmless at master-data scale, but a memoized id→course `Map` plus a memoized `candidates` would remove the repeated scans. Non-blocking.

### Error-shape mismatch for the add panels

Both panels do:

```js
onError: (err) => setMessage(mapApiError(err).message ?? 'Could not add prerequisite.'),
```

`mapApiError` returns `{ fields: {...} }` (no `message`) when the envelope has `details`, and `{ message }` otherwise. Prereq/cross-listing errors are most likely 409/422 (cycle, non-existent, duplicate), which land in the `{ message }` branch, so this works today. If these endpoints ever return a 400 with `details`, `.message` is `undefined` and the user sees the generic fallback. If field detail matters here, also read `mapped.fields`.

### API hooks match the A4-3 contract

The three hook modules are clean and correct against the stated contract:

- `useCourses` hits `/courses` (baseURL already `/api/v1`), returns `.data` (the `{data, meta}` envelope), 5-min stale time, no server filter — matches PD-1 client-side filtering.
- `usePrerequisites` is `enabled: courseId != null` and keys on `courseId`; add/remove invalidate that course's prereq key.
- `useCrossListings` invalidates the shared course-list key on add/remove so `isCrossListed` refreshes — the correct compensation for there being no list endpoint. `useRemoveCrossListing` is exported but unused, which is the documented add-only limitation; leaving the hook in place for the follow-up is reasonable.

Consistency nit: `useCrossListings` defines `courseListKey` as a local literal `['master-data','courses','list']` duplicating `useCourses`' `keys.list()`. A shared key factory would prevent the two drifting apart. Minor.

### Schemas and tests

`course-schemas.js` mirrors the A4-3 bounds: name ≤200, code ≤20 with `^[A-Za-z0-9_-]+$`, L/T/P coerced non-negative integers, credits ≥0.1, `courseType` enum from the single source `COURSE_TYPES`. The create/edit split correctly encodes the immutability of `code`/`departmentId`. `filterCourses` null-guards `search`, `name`, and `code`. The test file covers coercion, each rejection path, edit-schema omission of code/departmentId, and filter combinations including undefined search. Good coverage for the validation surface.

### Routing, nav, security, standards

The lazy route follows the exact existing pattern (`lazy(() => import(...))` wrapped in `AppShell` + `Suspense` fallback), and the nav item is added in the same `NAV_ITEMS` array the sidebar renders, so `/master-data/courses` is reachable and highlights correctly. No `dangerouslySetInnerHTML`, no `eval`, no inlined secrets; all dynamic text is rendered as JSX children (auto-escaped). All files are `.jsx`/`.js` with PropTypes, no TypeScript. Import order matches the enforced grouping (React → external → `@/` internal → feature-local → styles). Component sizes are within limits.

</details>

<details>
<summary>File map</summary>

- `pages/CourseManagementPage.jsx` — page: table, filters, campus-scoped dept picker, create/edit/delete, panel selector. Delete lacks `onError`; selector iterates `visibleRows` while panels use `allCourses`.
- `components/PrerequisitePanel.jsx` — prereq add/remove UI; remove-prereq lacks `onError`; per-render `nameOf`/`candidates`; local state not reset on course switch.
- `components/CrossListingPanel.jsx` — add-only cross-listing UI (documented limitation); success message not cleared on select change.
- `api/useCourses.js` — course CRUD hooks; global list, client-side filter.
- `api/usePrerequisites.js` — prereq list/add/remove hooks, keyed by courseId.
- `api/useCrossListings.js` — add/remove cross-listing; invalidates course list; `useRemoveCrossListing` exported but unused (intentional).
- `schemas/course-schemas.js` — Zod create/edit schemas + `filterCourses`.
- `constants/course-options.js` — `COURSE_TYPES` enum (single source).
- `schemas/course-schemas.test.js` — validation + filter tests, good branch coverage.
- `app/router.jsx` — added `/master-data/courses` lazy route (matches existing pattern).
- `components/layout/AppShell.jsx` — added "Courses" nav item.

Full diff: `git diff main -- frontend/src/features/master-data/course-management frontend/src/app/router.jsx frontend/src/components/layout/AppShell.jsx`

</details>
