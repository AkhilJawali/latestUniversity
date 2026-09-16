# A4-420 — Frontend Faculty Profile Management: Code Review

- **Story:** A4-420 — Frontend Faculty Profile Management (CRUD)
- **Code Review subtask:** A4-423
- **Date:** 2026-09-04
- **Reviewer:** AI semantic review (`semantic_reviewer`) — pre-human-review pass per squad rules
- **Scope:** `features/master-data/faculty-management/**`, `app/router.jsx`, `components/layout/AppShell.jsx`, and the A4-4 backend OQ-1 addition (`FacultyCampusDto`, `FacultyCampusAssociationService.getAssociationDtos`, `FacultyController` GET `/{id}/campuses`).

## Final Verdict

**APPROVED** (after fixes). Initial verdict NEEDS_CHANGES with 2 blocking issues; both fixed and the re-review confirmed APPROVED with no new blocking issues.

## Issues Found and Resolution

### Blocking (fixed)

| # | Issue | File | Fix | Status |
|---|-------|------|-----|--------|
| 1 | Edit-mode home department neither displayed nor safely preserved — the two-step reset the campus step to blank, disabling the department select so the stored `homeDepartmentId` had no matching option (invisible; a campus change forced a re-pick) | `FacultyFormModal.jsx`, `FacultyManagementPage.jsx` | Page passes `currentDepartment={{id, name}}` on edit; the department select renders a synthetic current-department option (matching the bound value) plus a "Current: X — pick a campus to change it" hint and a "Keep current…" campus option. No-op save preserves the department; choosing a campus switches to the live list. | Fixed |
| 2 | A 400 field error could be swallowed — `mapApiError` returns `{fields}` OR `{message}`; the modal showed a form-level message only when `fields` was absent, so a 400 naming an omitted/immutable field set an error on a hidden key with nothing visible | `FacultyFormModal.jsx` | `onError` now computes `renderedFieldNames(isEditing)`; if none of the returned field keys are rendered, it also sets a form-level fallback message (`role="alert"`). | Fixed |

### Non-blocking (addressed)

| # | Issue | Fix |
|---|-------|-----|
| 4 | Checkbox-group (`campusIds`) error not associated for screen readers | Added `aria-describedby` on the `<fieldset>` → `<p id="campusIds-error" role="alert">` |

### Non-blocking (accepted / tracked as follow-ups)

| # | Observation | Disposition |
|---|-------------|-------------|
| 3 | No focus restoration to the trigger on dialog close (WCAG 2.4.3) — affects the shared `ConfirmDeleteDialog` too | Tracked follow-up; best fixed in the shared primitive so campus-hierarchy/course features benefit. Not introduced by this story. |
| 5 | Brief stale-picker window after competency/campus add (self-heals on refetch) | Accepted — cosmetic, self-correcting |
| 6 | Backend existence check loads the entity (`findBy...isPresent()`) instead of `existsBy...`; possible N+1 on lazy `campus` | Accepted — correct + safe; micro-optimization only, negligible at the handful-of-campuses scale |
| 7 | Pagination params are wired (`buildFacultyListParams`, `useFacultyList`) but no page controls render — only page 0 (size 20) reachable | Tracked follow-up — functional gap against the paged contract; add controls when `ConfigTable` pagination is available |
| 8 | Duplicated campus→department two-step + checkbox-list markup | Tracked follow-up — extract `<CampusDepartmentPicker>` / `<CheckboxList>` before the pattern is copied again |
| 9 | `FacultyFormModal` ~300 lines exceeds the 150-line budget | Tracked follow-up — resolved by the #8 extraction |

## Confirmed Positives

- API hooks match the A4-4 contract exactly (paged list with exact filters, CRUD, competency + campus sub-resources); query-key invalidation correct.
- Schemas: `optionalLoad` blank→undefined handling, `min ≤ max` refine (fires only when both present), exact-designation enum, edit schema drops immutable identifier/campuses/competencies. 18 tests cover these.
- Error handling complete across all six mutations (create/update/delete + competency add/remove + campus add/remove).
- Delete surfaces `mapApiError().message` into the confirm dialog; panels remount via `key` on faculty switch; page memoizes `initialValues`.
- Backend `getAssociationDtos`: validates faculty existence (404), returns active-only, maps to a flat DTO (no entity leakage); controller uses the module `Map.of("data", ...)` envelope and conventional naming.
- Routing complete/consistent; security clean (no `dangerouslySetInnerHTML`, no secrets, escaped JSX); no TypeScript; PropTypes present; import order correct.

## Backend Verification Caveat

The A4-4 OQ-1 endpoint (`GET /faculty/{id}/campuses`, `FacultyCampusDto`, `getAssociationDtos`) was reviewed **by inspection only** — Maven is not available in this environment to compile/run the backend. A backend `mvn verify` should be run before relying on it.

## Verification After Fixes

- `node node_modules/vite/bin/vite.js build` → success, 206 modules, dedicated `FacultyManagementPage` chunk.
- Faculty schema tests green (18); full suite unaffected (102).
