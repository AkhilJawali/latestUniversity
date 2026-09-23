# Frontend Schedulable Asset Master Data — Code Review

A React feature for managing non-room schedulable assets (projectors, equipment sets) with campus/department assignment and per-asset availability windows. Uses TanStack Query for API state, Zod for validation, native `<dialog>` for modals, and server-side pagination.

**Verdict**: APPROVED

**Issues fixed:**
1. ✅ **Modal accessibility — close button added** — Added visible close button (✕) in modal header with `aria-label="Close"`, matching the ConfirmDeleteDialog pattern.
2. ✅ **Window-row error display shows all errors** — Changed from `||` chaining to individual error rendering. Now all validation errors (dayOfWeek, startTime, endTime) display simultaneously.

**Watch for:**
- Accessibility gaps in the modal: no visible close button, no `aria-describedby` on required fields, and window-row error messages use `aria-invalid` but errors only appear on one field at a time.
- The filter bar `<input>` for asset type has no `aria-describedby` connection when there are validation errors (though the current implementation has no explicit error state for this field).
- Pagination controls at page bottom lack `aria-label` for screen reader context.

---

## High-level view

The feature is well-structured with a clear separation between page, API hooks, modal, schemas, and constants. The API layer uses TanStack Query correctly with proper cache invalidation on mutations. Validation is comprehensive using Zod schemas that mirror backend constraints, including a smart `superRefine` check that end time must be after start time for availability windows.

The native `<dialog>` element provides built-in focus trapping and Escape dismissal, which is good for accessibility. However, the modal implementation is missing a visible close button (only has Cancel/Save at bottom), and the `onCancel` handler relies on the dialog's native behavior without explicitly handling the close button pattern users expect.

The campus→department two-step dropdown in create mode correctly clears the department selection when campus changes, preventing invalid campus/department combinations. The edit mode correctly disables immutable fields (identifier, campus, department) rather than hiding them, which provides better UX.

---

<details>
<summary>Issues (3 remaining non-blocking)</summary>

1. ✅ ~~**Modal accessibility — no visible close button**~~ — **FIXED**: Added visible close button (✕) in modal header with `aria-label="Close"`.

2. ✅ ~~**Window-row error display shows only one error**~~ — **FIXED**: Changed from `||` chaining to individual error rendering. Now all validation errors display simultaneously.

3. **Pagination lacks accessible labels** (non-blocking) — The pagination buttons at the bottom of AssetManagementPage (Previous/Next) lack `aria-label` attributes. While there's visible text, screen reader users benefit from explicit labels like "Go to previous page" and "Go to next page", especially when buttons are disabled. The page info span also lacks an `aria-live` region to announce page changes.

4. **Filter bar input missing error association** (non-blocking) — The asset type filter `<input>` has no `aria-describedby` attribute. While there's no explicit error state for this field currently, if validation is added later (e.g., character limits), the association would be missing. This is a minor preventive concern.

5. **Zod error path handling incomplete** (non-blocking) — The `validateWith` helper in `config-schemas.js` (reused by AssetFormModal) only captures the first error per field: `if (field != null && errors[field] == null) errors[field] = issue.message`. This means if a single field has multiple validation failures, only the first is shown. For the asset identifier field, which has `min`, `max`, and `regex` constraints, a user might see "Identifier is required" but after fixing that, get hit with "Must not exceed 50 characters" on the same submit attempt. Consider showing all errors or at least the most relevant one.

</details>

---

<details>
<summary>Details</summary>

### API layer design

The `useAssets` hook correctly implements server-side filtering with the `useMemo`-computed `params` object. The `staleTime: 5 * 60 * 1000` (5 minutes) is reasonable for master data that changes infrequently. Cache invalidation on mutations uses a broad query key pattern `['master-data', 'assets', 'list']` which correctly invalidates all list variations (different filter combinations) when any asset is created/updated/deleted.

The hook design follows the established pattern in the codebase, reusing `useCampuses` and `useDepartments` from the campus-hierarchy feature. This is good for consistency. The `useDepartments` hook correctly receives `null` when no campus is selected, which the API layer presumably handles by returning an empty list or not making the request.

### Validation schema design

The Zod schemas are well-designed and mirror the backend constraints documented in the code comments. The `windowSchema` uses `superRefine` to enforce the end-time-after-start-time rule, which is a client-side-only check (backend doesn't validate this for asset windows per the comment). This is the right approach — fail fast on the client, but the backend is the source of truth.

The `CODE_REGEX` pattern `/^[A-Za-z0-9_-]+$/` is reasonable for identifiers — alphanumeric with hyphens and underscores. The `TIME_REGEX` correctly validates `HH:MM` 24-hour format.

The `z.coerce` usage for `campusId` and `owningDepartmentId` is necessary because HTML select values are strings, but the API expects numbers. This is handled correctly.

### Modal implementation

The AssetFormModal uses the native `<dialog>` element which provides:
- Built-in focus trap (Tab cycles within the modal)
- Escape key dismissal (fires `onCancel` event)
- Backdrop click dismissal (can be configured)
- `aria-modal="true"` implicitly
- Proper z-index stacking

However, the modal is missing a visible close button in the header. The ConfirmDeleteDialog component has this pattern: a `<button class="modal-close">✕</button>` in the header. AssetFormModal should follow the same pattern for consistency and usability.

The modal correctly handles the open state via `useEffect`:
```javascript
if (open && !dlg.open) dlg.showModal();
if (!open && dlg.open) dlg.close();
```
This pattern works correctly with React's render cycle.

The form correctly splits validation errors between field-level errors (`errors` state) and window-row errors (`windowErrors` state). The error extraction regex `/^availabilityWindows\.(\d+)\.(\w+)$/` correctly parses Zod's path format.

### Accessibility observations

The modal has `aria-label={isEditing ? 'Edit asset' : 'Add asset'}` which provides context for screen readers. Form fields have proper `id`/`htmlFor` associations via labels. Required fields have a red asterisk visual indicator (`.req` class) and `aria-invalid` is set when there are errors.

However, the error messages use `aria-describedby` on only some fields. The pattern is inconsistent — `name`, `identifier`, `assetType` fields have error association, but the campus/department select pair in create mode have error messages displayed below them without `aria-describedby` linking. The window-row fields have `aria-invalid` but the error message appears outside the individual field's scope (it's a sibling paragraph, not associated).

The pagination section could benefit from:
- `aria-label` on Previous/Next buttons
- `aria-disabled` (though HTML `disabled` attribute implies this)
- An `aria-live="polite"` region for the page info to announce navigation

### Performance considerations

The `useMemo` for `params` in AssetManagementPage correctly prevents unnecessary re-renders and API calls. The `useMemo` for `initialValues` prevents recalculating the edit-form initial state on every render, though the dependency array includes `editing` which is the entire row object — this could be optimized to `editing?.id` if only the identity matters, but the current approach is correct.

The ConfigTable component has client-side filtering and pagination built in, but AssetManagementPage correctly disables the search (`searchable={false}`) and uses server-side pagination instead. This is the right call for an endpoint that can return many rows across many filter combinations.

### Code quality and patterns

The code follows the established patterns in the codebase:
- Feature-based folder structure under `features/master-data/asset-management/`
- PropTypes for prop validation (not TypeScript)
- Zod for runtime validation
- TanStack Query for server state
- Native `<dialog>` for modals
- CSS file colocated with the feature

The comments reference requirement IDs (e.g., `A4-435 §5.4`, `PD-3`, `AC-4`) which provides traceability to the design document. This is good practice.

One minor observation: the `initialValues` computation in the edit branch slices time strings to 5 characters: `(w.startTime ?? '').slice(0, 5)`. This handles the case where the backend returns `HH:MM:SS` but the `<input type="time">` expects `HH:MM`. This is correct defensive coding.

### Security considerations

No security issues identified. The code:
- Never uses `dangerouslySetInnerHTML`
- Validates all user input with Zod before submission
- Uses the centralized `apiClient` which will handle auth headers
- Maps API errors safely via `mapApiError` which never exposes stack traces
- Sanitizes by virtue of React's built-in escaping in JSX

### Test coverage observations

No test files were provided for review. The feature would benefit from:
- Unit tests for the Zod schemas (edge cases around time ordering, identifier regex)
- Integration tests for the modal open/close behavior
- Tests for the campus→department cascade in create mode
- Tests for error state rendering

</details>

---

<details>
<summary>File map</summary>

| File | Changes |
|------|---------|
| `pages/AssetManagementPage.jsx` | Main page with filters, table, and modal orchestration |
| `components/AssetFormModal.jsx` | Create/edit modal with embedded availability window editor |
| `api/useAssets.js` | TanStack Query hooks for CRUD operations against `/api/v1/assets` |
| `schemas/asset-schemas.js` | Zod schemas for create/edit validation |
| `constants/asset-options.js` | Day-of-week enum for availability windows |
| `asset-management.css` | Feature-specific styles (filters bar, modal width, window-row layout) |

**Diff reference**: New feature — all files are additions, no diff to show.

</details>
