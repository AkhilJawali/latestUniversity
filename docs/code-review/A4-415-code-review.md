# A4-415 — Frontend Course Management: Code Review

- **Story:** A4-415 — Frontend Course Management (CRUD)
- **Code Review subtask:** A4-418
- **Date:** 2026-09-04
- **Reviewer:** AI semantic review (`semantic_reviewer`) — pre-human-review pass per squad rules
- **Scope:** `features/master-data/course-management/**`, `app/router.jsx`, `components/layout/AppShell.jsx`

## Final Verdict

**APPROVED** (after fixes). Initial verdict was NEEDS_CHANGES with 2 blocking issues; both were fixed and the re-review confirmed APPROVED with no blocking issues remaining.

## Issues Found and Resolution

### Blocking (fixed)

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| 1 | Delete-course had no `onError` — a rejected delete (409 in-use / 404 / 500) was silent; dialog stayed open with no message | `pages/CourseManagementPage.jsx` (`confirmDelete`) | Added `onError` that sets a new `deleteError` state via `mapApiError(err).message`; the ConfirmDeleteDialog `label` appends the error so the dialog stays open and explains the failure; added `cancelDelete` to clear both `toDelete` and `deleteError` | Fixed |
| 2 | Remove-prerequisite had no `onError` — a failed removal was silent, asymmetric with add-prerequisite which showed the mapped message | `components/PrerequisitePanel.jsx` (chip remove) | Remove handler now clears `message` then calls `remove.mutate(id, { onError })` surfacing `mapApiError(err).message`, symmetric with add | Fixed |

### Non-blocking (addressed)

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| 3 | Transient panel state (`picked`/`departmentId`/`message`) persisted across a selected-course switch | `pages/CourseManagementPage.jsx` | Added `key` on each panel bound to `selectedCourse.id` so panels remount and reset local state on course change | Fixed |
| 6 | Cross-listing success message never cleared on select change | `components/CrossListingPanel.jsx` | Campus `onChange` now also `setMessage(null)` | Fixed |

### Non-blocking (accepted / deferred)

| # | Observation | Disposition |
|---|-------------|-------------|
| 4 | `nameOf` / `candidates` recomputed each render in `PrerequisitePanel` (O(n·m)) | Accepted — harmless at master-data scale; memoization is a candidate micro-optimization, not needed now |
| 5 | Add-panel 400-with-`details` would fall back to a generic message because `mapApiError` returns `{fields}` (no `message`) for field errors | Accepted — these sub-resource endpoints return 409/422 (cycle / non-existent / duplicate), which land in the `{message}` branch; documented |
| 7 | No success toast (ui-standards mentions toasts) | Accepted — consistent with the reused scheduling-config pages (modal close + list refresh); project-wide toast adoption is a separate concern |
| — | a11y parity: `CrossListingPanel` routes both success and error into `role="status"` (polite), `PrerequisitePanel` uses `role="alert"` for errors | Minor; acceptable — cross-listing has add-only success/soft-error messaging |

## Documented Follow-up (not a defect)

- **Cross-listing removal UI is intentionally absent.** A4-3 exposes no "list cross-listings" endpoint and `CourseDto` carries only a boolean `isCrossListed` (not the set of cross-listed department IDs). The panel can therefore add a cross-listing and reflect the boolean status, but cannot render a removable list of specific departments. `useRemoveCrossListing` exists in the hook layer for when a list endpoint is added. This is noted in a header comment in `CrossListingPanel.jsx` and flagged here as a backend follow-up.

## Confirmed Positives

- API hooks match the A4-3 contract exactly: global list (no server filter), CRUD on `/courses`, prerequisite and cross-listing sub-resources, course-list invalidation to refresh the `isCrossListed` boolean.
- Edit schema correctly omits the immutable `code` and `departmentId` (per A4-3 `UpdateCourseRequest`).
- Create/update error threading via spread `handlers` preserves the modal's `mapApiError`-based field rendering.
- Accessibility: labels on all controls, `aria-label` on icon-only buttons, `aria-label`ed `<section>`s, `role="alert"`/`role="status"` messages, native `<dialog>` focus trap + Escape.
- Routing complete and consistent with the existing lazy-route pattern; nav item wired.
- Security: no `dangerouslySetInnerHTML`, no `eval`, no secrets; all dynamic text auto-escaped as JSX children.
- Standards: all `.jsx`/`.js` (no TypeScript), PropTypes present, enforced import order.

## Verification After Fixes

- `node node_modules/vite/bin/vite.js build` -> success, 196 modules, dedicated `CourseManagementPage` chunk emitted.
- Unit suite unchanged and green (18 course-schema tests; 84 full suite).
