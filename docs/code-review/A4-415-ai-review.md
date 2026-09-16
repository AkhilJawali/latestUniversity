# Error surfacing for course delete and prerequisite/cross-listing panels (re-review)

This pass re-checks the two blocking issues from v1 (silent failure on course delete, silent failure on prerequisite removal) plus the two non-blocking items (stale panel state when switching selected course, stale cross-listing success message). All four are addressed. Delete failures now keep the confirm dialog open with a mapped reason; prerequisite removal now mirrors the add path's error handling; both sub-panels remount on course change via `key`; and the cross-listing message clears when the campus changes.

Watch for: one minor asymmetry remains — the prerequisite *remove* path does not clear its error `message` on success (confirmed), so a failed-then-succeeded removal leaves a stale error line until the next action. Non-blocking.

**Verdict**: APPROVED

## High-level view

The delete flow is now fail-closed on the UI: `confirmDelete` clears prior error, fires the mutation, and only tears down `toDelete` in `onSuccess`. On error it maps via `mapApiError` and writes `deleteError`, which the dialog appends to its label — so the dialog stays open and explains itself. A dedicated `cancelDelete` clears both pieces of state, so a dismissed-then-reopened dialog never shows a stale error. This resolves blocking issue 1.

Prerequisite removal now passes an `onError` to `remove.mutate` that maps the error into the panel's `message`, symmetric with the add path, and clears `message` before firing. This resolves blocking issue 2. The remaining rough edge is that success on removal doesn't clear a prior error message, unlike the add path which clears up-front on every submit.

The two sub-panels are keyed by the selected course id (`prereq-${id}`, `xl-${id}`), so switching courses remounts them and drops transient `picked` / `campusId` / `departmentId` / `message` state instead of carrying it across courses. The cross-listing panel also clears its message when the campus select changes, so a "Cross-listing added." confirmation doesn't linger while the user sets up a different target.

<details>
<summary>Issues (1)</summary>

1. **Prerequisite remove success doesn't clear stale error** — on remove, `onError` sets `message` but there is no `onSuccess` clearing it; a later successful remove leaves the old error line visible. Mirror the add path by clearing `message` on success (or the list re-render). Non-blocking.

</details>

<details>
<summary>Details</summary>

### Delete failure now keeps the dialog open with a reason

`confirmDelete` resets `deleteError` up front, then mutates. `onSuccess` is the only path that clears `toDelete` (and deselects the course if it was the one being managed); `onError` maps the error and stores the message. Because `toDelete` survives an error, `ConfirmDeleteDialog` stays open, and its `label` switches to the `${code} — ${name}. ${deleteError}` branch to show why. `cancelDelete` clears `toDelete` and `deleteError` together, which is what `onCancel` is wired to — so dismissing and reopening starts clean. This is a correct fail-closed pattern: an FK-constrained course (referenced as a prerequisite or cross-listing) will report the backend reason instead of silently doing nothing.

One display note (possible, cosmetic): `deleteError` is surfaced through the dialog's `label`, so the mapped message renders wherever that dialog composes the delete-target label. If the message is long, it lands inline with the target text rather than as a separate error line. That's a presentation nuance, not a correctness issue, and doesn't block.

### Prerequisite removal error handling is now symmetric with add

The remove button's `onClick` clears `message` then calls `remove.mutate(id, { onError })`, matching `onAdd`'s mapped-error approach. A backend rejection (e.g., a constraint that blocks removal) now surfaces in the same `role="alert"` region as add errors, instead of being swallowed. The gap is only on the success side: `onAdd` clears `message` on every submit (so a prior error can't persist past the next add), but the remove success path relies on nothing clearing the message. If a remove fails, shows an error, and a subsequent remove succeeds, the error text remains until some other action clears it. Mirroring the add path — clearing `message` in an `onSuccess` — would close this. Minor.

### Panel remount on course switch

Keying `PrerequisitePanel` and `CrossListingPanel` on the selected course id means React unmounts the old instance and mounts a fresh one when the selection changes, discarding `picked`, `campusId`, `departmentId`, and `message`. This is the intended fix for the v1 observation that a half-filled cross-listing form or a stale message could carry from one course to the next. The tradeoff — remounting also resets the TanStack Query hook instances — is harmless here since the query keys are course-scoped and cached data is reused.

### Cross-listing message clears on campus change

The campus `onChange` now resets `departmentId` and `setMessage(null)` alongside `setCampusId`, so a "Cross-listing added." status doesn't sit stale while the user picks a different campus/department. Consistent with clearing `message` at the start of `onAdd`.

</details>

<details>
<summary>Files reviewed</summary>

- `frontend/src/features/master-data/course-management/pages/CourseManagementPage.jsx` — added `deleteError` state, `onError` on delete mutation, `cancelDelete`, `mapApiError` import, dialog label branch, `key` props on both panels.
- `frontend/src/features/master-data/course-management/components/PrerequisitePanel.jsx` — remove `onClick` clears message and passes mapped `onError`.
- `frontend/src/features/master-data/course-management/components/CrossListingPanel.jsx` — campus `onChange` clears message.

Full diff: `git diff main` on the three files above.

</details>
