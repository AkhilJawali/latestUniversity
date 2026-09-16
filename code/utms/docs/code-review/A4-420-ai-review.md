# A4-420 Frontend Faculty Profile Management — semantic review

Adds a faculty-management admin feature: a server-paginated list with department/designation/competency filters, a bespoke create/edit modal, and two sub-resource panels (competencies, campuses). A small backend addition exposes a new `GET /api/v1/faculty/{id}/campuses` read-back endpoint so the campus panel can display the current set. The feature reuses the established `ConfigTable`, `ConfirmDeleteDialog`, `validateWith`, and `mapApiError` primitives, and the A4-415 review lessons hold: delete `onError` surfaces into the dialog label, panels remount via `key`, initial values are memoized, and competency add+remove both surface errors.

Watch for: two edit-mode bugs in the modal — the home-department picker can't display or preserve the existing department because the campus step resets to blank (likely), and `mapApiError` never returns both `fields` and `message`, so a 400 field error on submit can be swallowed silently (confirmed). One accessibility gap: the custom modal doesn't restore focus to the trigger on close (likely). Everything else is COMMENT-level.

**Verdict**: NEEDS_CHANGES

<details>
<summary>Issues (9)</summary>

1. **Edit-mode home-department not preserved/displayed** — On edit, `initialValues.homeDepartmentId` is populated but `deptCampusId` resets to `''`, disabling the department select and never loading its options; the pre-existing department is invisible and any campus change forces a re-pick. (likely — `FacultyFormModal.jsx`)
2. **400 field error swallowed on submit** — `mapApiError` returns either `{fields}` or `{message}`, never both. `onError` sets field errors when `mapped.fields` exists; a 400 whose `details` name a field the edit form doesn't render (`identifier`/`campusIds`/`competencyCourseIds`) lands on a hidden key with no form-level message shown. 422s (no `details`) do surface. (confirmed — `FacultyFormModal.jsx`)
3. **No focus restoration on modal close** — Native `<dialog>` traps focus and handles Escape, but focus is not returned to the triggering button after close, failing WCAG 2.4.3. Affects `FacultyFormModal` and the reused `ConfirmDeleteDialog`. (likely — `FacultyFormModal.jsx`, `ConfirmDeleteDialog.jsx`)
4. **Checkbox group error not announced** — The `campusIds` `<fieldset>` renders its error as plain `<p>`, not associated via `aria-describedby` nor a live region, so screen-reader users tabbing the checkboxes don't hear "select at least one". (likely — `FacultyFormModal.jsx`)
5. **Brief stale picker window after competency/campus add** — `candidates` derive from the fetched list; between the `onSuccess` reset and the refetch settling, the just-added item can momentarily reappear in the picker. Self-heals on invalidation. Minor. (possible — `CompetencyPanel.jsx`, `CampusPanel.jsx`)
6. **Existence check loads entity instead of `existsBy`** — `getAssociationDtos` does `findByIdAndDeletedAtIsNull(...).isPresent()`; correct and safe (404, no leakage) but `existsByIdAndDeletedAtIsNull` avoids materializing the entity. Style only. (confirmed — `FacultyCampusAssociationService.java`)
7. **Pagination wired in params but no page controls** — `buildFacultyListParams`/`useFacultyList` thread `page`/`size`, but the page holds no page state and renders no controls, so only page 0 (size 20) is reachable. Functional gap against a paged contract. (confirmed — `FacultyManagementPage.jsx`)
8. **Duplicated campus→department two-step + checkbox-list** — The dependent-select logic exists in both the modal and the page; the checkbox-list block is duplicated within the modal. Extract shared controls. Non-blocking. (confirmed — `FacultyFormModal.jsx`, `FacultyManagementPage.jsx`)
9. **`FacultyFormModal` exceeds the 150-line budget** — ~300 lines; standards call for extracting sub-components/hooks past 150. (confirmed — `FacultyFormModal.jsx`)

</details>

## High-level view

Routing is complete and consistent with the existing pattern: the lazy route, Suspense fallback, and `AppShell` nav item all match the A4-410/A4-415 precedent, and the nav is reachable. Security is clean — no `dangerouslySetInnerHTML`, no secrets, all rendering goes through React's default escaping, and user text (course names, faculty names) flows into JSX text nodes only. No TypeScript, `.jsx`/`.js` extensions correct, PropTypes present on every component, import order follows the enforced React → external → `@/` → local → styles grouping.

The schema layer is the strongest part of the change. `optionalLoad` correctly treats blank as "omitted" via a union with `z.literal('')` and a transform to `undefined`, the cross-field `min <= max` refine fires only when both are non-null, and the edit schema correctly drops the immutable `identifier`/`campusIds`/`competencyCourseIds` fields to match `UpdateFacultyRequest`. The 18 tests cover the bounds, the optional-blank path, min>max rejection, and the param builder's omit-empty behavior. The list-params builder correctly sends `designation` as an exact string (matching the A4-4 exact-match contract, no free-text search) and coerces ids to numbers.

The two real defects are both in the edit flow of the custom modal. The home-department picker is a campus→department two-step, but on edit it resets the campus step to blank while keeping the stored `homeDepartmentId`; the department `<select>` stays disabled with no options, so the existing department is neither shown nor selectable. Separately, the submit error handler relies on `mapApiError` returning a message when there are no field details, but for a 400 whose `details` reference a field the edit form doesn't render, the error lands on a hidden key and nothing is shown. 422s surface correctly.

Null-guarding across the feature is otherwise sound: every list read uses `?? []`, every controlled input uses `?? ''`, `useFacultyCampuses`/`useFacultyCompetencies` are `enabled: facultyId != null`, and the page's `selectedFaculty` recomputes from the refetched rows with a fallback to the last selection — so a filtered-out or deleted selection degrades gracefully (and delete explicitly clears `selected` when the deleted row was selected). The `key={comp-/camp-${id}}` remount resets panel local state on faculty switch as intended.

Error handling is complete across all six mutations: create/update route backend errors through the modal's `onError` (with the caveat in issue 2), delete surfaces `mapApiError().message` into the `ConfirmDeleteDialog` label, and both panels surface add **and** remove errors into a `role="alert"` message. The backend addition is correct and safe: `getAssociationDtos` validates faculty existence first (404 via `EntityNotFoundException`), returns only active associations, and maps to a flat DTO exposing just `campusId`/`name`/`code` — no entity leakage. The controller follows the module's `Map.of("data", ...)` envelope and naming conventions exactly.

<details>
<summary>Details</summary>

### Edit-mode home department: reset campus step orphans the stored department

`FacultyManagementPage` builds `initialValues` for edit with `homeDepartmentId: editing.homeDepartmentId`. The modal's open-reset effect then does `setDeptCampusId('')`. The department `<select>` is `disabled={!deptCampusId || departments.isLoading}` and its options come from `useDepartments(deptCampusId ? ... : null)` — so with `deptCampusId` blank, the control is disabled and empty. The bound `value={values.homeDepartmentId}` points at a department id that has no matching `<option>`, so the browser shows the empty "Select department…" entry.

Two user-visible consequences:
- The coordinator opening Edit cannot see which department the faculty currently belongs to.
- If they touch nothing and Save, `values.homeDepartmentId` still holds the original id and passes the schema, so it round-trips — but the moment they pick a campus to change anything, the campus `onChange` sets `homeDepartmentId` to `''`, forcing a re-pick.

The list row carries `homeDepartmentName` (a column) but not the campus id needed to seed the two-step. Options: seed `deptCampusId` from the faculty's campus if the row can carry it, replace the two-step with a single department picker for edit mode keyed by the known department id, or fetch the department to derive its campus. This is the issue most likely to generate a "the edit form looks broken" report.

### Submit error mapping can silently swallow a 400

```jsx
onError: (error) => {
  const mapped = mapApiError(error);
  if (mapped.fields) setErrors(mapped.fields);
  else setFormMessage(mapped.message);
}
```

`mapApiError` returns `{ fields }` **or** `{ message }`, never both. A 422 `BusinessRuleViolationException` (min>max, duplicate-identifier surfaced as a business rule) has no `details`, so it maps to `{ message }` and displays — good. But a 400 whose `details[].field` names something the edit form doesn't render (edit omits `identifier`, `campusIds`, `competencyCourseIds`) sets `errors['identifier']` on a field with no visible input, and no form-level message appears. The user sees the submit rejected with nothing explaining why. Falling back to a form-level message when none of the mapped field keys correspond to a rendered field closes the gap.

### Focus restoration on close

Both dialogs rely on native `<dialog>` for the focus trap and Escape (correctly noted in comments). Neither restores focus to the element that opened them. After Save/Cancel/Delete, focus lands on `<body>`, a WCAG 2.4.3 (Focus Order) regression for keyboard and screen-reader users. Capturing the trigger (via `document.activeElement` at open) and calling `.focus()` on close addresses it. Since this also affects the shared `ConfirmDeleteDialog`, fixing it in that primitive benefits the campus-hierarchy and course features too.

### Checkbox-group error association

The `campusIds` `<fieldset>` renders its error as `<p className="field-error">` after the list, but there's no `aria-describedby` from the fieldset to that message and it isn't a live region. A sighted user sees it; a screen-reader user tabbing the checkboxes won't hear it. Adding `aria-describedby` on the `<fieldset>` (or `role="alert"` on the message) resolves it. The single-input fields are fine — they pair `aria-invalid` with adjacent error text tied by proximity, and every input has an explicit `htmlFor`/`id` label.

### Backend read-back is correct and safe

`getAssociationDtos` is a read-only transactional method that first asserts faculty existence:

```java
if (!facultyRepository.findByIdAndDeletedAtIsNull(facultyId).isPresent()) {
    throw new EntityNotFoundException("Faculty", facultyId);
}
```

then maps only active associations to `FacultyCampusDto` (a flat `campusId`/`name`/`code` builder DTO — no entity exposed, matching the "entities never leave the service layer" rule). 404 for a missing/soft-deleted faculty is the right status and matches the competency sub-resource's behavior. The controller wraps it in `Map.of("data", campuses)`, consistent with every other endpoint in the class, and the `@Operation` summary + path (`/{id}/campuses`, plural, nested) follow module and API-standard conventions. The only nit: the existence check loads the entity just to check presence — `existsByIdAndDeletedAtIsNull` would be marginally cheaper. One thing to keep an eye on: `getAssociationDtos` walks `fca.getCampus()` per association, so if `FacultyCampusAssociation.campus` is lazy this is one query per row; for the handful of campuses a faculty has this is fine, but a `JOIN FETCH` variant would remove the N+1 if association counts grow.

### Pagination gap

The list contract is paged and `buildFacultyListParams`/`useFacultyList` thread `page`/`size` through (with tests). But `FacultyManagementPage` holds no page state and renders no next/prev controls, so only page 0 (size 20) is reachable. With a roster over 20, later records are unreachable from this screen. Either wire `ConfigTable` pagination (if it supports it) or add simple controls. Flagging as functional completeness against the paged contract, not a crash.

### Duplication and component size

The campus→department dependent-select appears in both the page filter bar and the modal; the checkbox-list markup is repeated within the modal for campuses and competencies. A small `<CheckboxList>` and a `<CampusDepartmentPicker>` would remove the repetition and pull `FacultyFormModal` (~300 lines) under the 150-line budget. Non-blocking, but worth doing before this pattern is copied into the next master-data feature.

</details>

<details>
<summary>File map</summary>

- `constants/faculty-options.js` — 7 fixed designation strings mirroring the server allowlist.
- `schemas/faculty-schemas.js` — create/edit Zod schemas + `buildFacultyListParams`; correct optional-load, min<=max, exact-designation handling.
- `schemas/faculty-schemas.test.js` — 18 tests covering bounds, blank loads, min>max, param builder.
- `api/useFaculty.js` — list/create/update/delete hooks; correct query-key invalidation.
- `api/useFacultyCompetencies.js` — competency list/add-bulk/remove hooks; `enabled` guard.
- `api/useFacultyCampuses.js` — campus list (new GET)/add/remove hooks; `enabled` guard.
- `components/FacultyFormModal.jsx` — bespoke create/edit modal; **edit-mode dept bug**, **swallowed-400 bug**, focus-restore + checkbox-error a11y gaps, over size budget.
- `components/CompetencyPanel.jsx` — add/remove with both errors surfaced; minor stale-picker window.
- `components/CampusPanel.jsx` — add/remove with both errors surfaced; reads new GET.
- `pages/FacultyManagementPage.jsx` — filters + table + panels + modal wiring; **no pagination UI**; memoized initialValues; sound null guards; delete error into dialog label.
- `faculty-management.css` — feature styles (not reviewed for behavior).
- `app/router.jsx` — lazy `/master-data/faculty` route, matches precedent.
- `components/layout/AppShell.jsx` — "Faculty" nav item added.
- `FacultyCampusDto.java` — flat response DTO (campusId/name/code).
- `FacultyCampusAssociationService.java` — `getAssociationDtos`: existence check + active-only map, safe; possible N+1 on lazy campus.
- `FacultyController.java` — new `GET /{id}/campuses`, correct envelope + naming.

Full diff: `git diff main` under `frontend/src/features/master-data/faculty-management/` and `code/utms/.../masterdata/faculty/`.

</details>
