# Semantic Code Review — A4-435 Asset Management Frontend

**Review Date:** 2026-09-21
**Reviewer:** Kiro (AI)
**Files Changed:** 8 (API hooks, components, schemas, constants, tests)

---

## Summary

This feature implements a schedulable asset management UI with CRUD operations, a two-step campus→department selector on create, and an embedded availability-windows editor. The code follows established patterns from other master-data features (campus hierarchy, courses, faculty) and reuses shared components (`ConfigTable`, `ConfirmDeleteDialog`, `validateWith`). Form validation uses Zod schemas with proper client-side feedback before API calls. The implementation is clean, well-documented with design-section references, and avoids common security pitfalls.

**Watch for:** The `validateWith` function only surfaces the first error per field, which may hide additional validation issues from users. Accessibility is generally solid but the window-row remove buttons use text instead of an icon, which could be improved for consistency. No XSS vectors found — the code avoids `dangerouslySetInnerHTML` and relies on React's escaping.

**Verdict:** APPROVED

---

## High-level view

The feature is architected as a standard master-data CRUD module following the established pattern in this codebase. API hooks in `useAssets.js` wrap TanStack Query for list/create/update/delete operations with proper query invalidation. The main page component (`AssetManagementPage.jsx`) manages filter state and delegates table rendering to the shared `ConfigTable`, which handles loading/error/empty states and client-side pagination consistently. The `AssetFormModal` is a bespoke component (not reused from elsewhere) because it requires the campus→department two-step cascade and the embedded availability-windows editor, both of which are specific to asset creation.

Zod schemas in `asset-schemas.js` mirror backend validation rules with clear bounds (identifier pattern, field length limits, time format validation). The availability-window validation includes a `superRefine` check that end time must be after start time — this is enforced client-side per design note A435-OQ-6 since the backend doesn't check it for asset windows. Tests cover all major paths including validation, error handling, and accessibility attributes.

---

<details>
<summary>Issues (3)</summary>

1. **Validation error flattening hides multiple issues** — The `validateWith` helper only captures the first error per field. If a user makes multiple mistakes on the same field (e.g., both format and length), they only see one message. Consider whether this is acceptable UX or if showing all errors would be better.

2. **Window remove buttons use text, inconsistent with icon pattern** — The availability-window "Remove" buttons are text-based, while the rest of the app uses icon buttons for row actions. This is a minor consistency issue but may affect visual coherence.

3. **No maximum limit on availability windows** — Users can add unlimited window rows without client-side validation. While the backend may enforce a limit, the UI should provide earlier feedback if there's a business constraint.

</details>

<details>
<summary>Details</summary>

### API hooks structure and caching strategy

The `useAssets.js` file follows the established TanStack Query pattern with a 5-minute stale time for the list query. Query keys are properly structured as `['master-data', 'assets', 'list', params]` enabling cache isolation per filter combination. Mutations correctly invalidate the list query on success, ensuring the table reflects changes immediately.

One minor observation: the mutation hooks don't expose `onSuccess` callbacks themselves — they only invalidate the cache. The caller (the page component) provides its own `onSuccess: closeModal` handler. This is correct and avoids duplicate cache invalidation, but it means the hook's return type doesn't include the standard `isSuccess` reset behavior that some features rely on. The tests verify this works, so it's not a bug, just a pattern difference from some other hooks in the codebase.

### Form modal and validation flow

The `AssetFormModal` component manages its own state for form values, availability windows, and errors. On open, it resets to `initialValues` and clears errors. The submit handler runs the Zod schema via `validateWith` and returns early on validation failure (AC-4: no request on invalid input), which is correct.

Error handling distinguishes between field-level errors (keyed by field name) and form-level errors (displayed in a `role="alert"` paragraph). Backend errors are mapped via `mapApiError`, which extracts field errors from the `details` array or surfaces a generic message. This is consistent with the API error envelope standard.

The window validation logic correctly splits errors keyed like `availabilityWindows.0.endTime` into a separate `windowErrors` object, enabling per-row error display. The `superRefine` in `windowSchema` checks that `endTime > startTime` only when both times are valid, avoiding false positives on malformed input.

### Accessibility implementation

The modal uses the native `<dialog>` element for automatic focus trapping and Escape-to-dismiss. All interactive elements have proper labels: text inputs use `<label htmlFor>`, selects use `aria-label` for the two-step cascade, window inputs use `aria-label={Window ${i + 1} day/start/end}`, remove buttons use `aria-label={Remove window ${i + 1}}`, and form-level errors use `role="alert"`. Invalid fields correctly set `aria-invalid="true"` and `aria-describedby` pointing to their error message element.

**Gap:** The modal doesn't auto-focus the first input when opened. Adding `autoFocus` on the name input would improve keyboard UX, though this is an enhancement rather than a WCAG AA compliance issue.

### Security posture

The code correctly avoids `dangerouslySetInnerHTML` everywhere. All user input flows through controlled React inputs, which automatically escape values. The Zod schemas validate identifier format with a regex (`/^[A-Za-z0-9_-]+$/`) before submission, and the backend will re-validate. The `mapApiError` function only surfaces the `message` field from backend errors, never stack traces or internal details.

No secrets or API keys are present in the frontend code. The `apiClient` handles CSRF and JWT via interceptors, not exposed in this feature's code.

### Test coverage analysis

The test suite covers all CRUD operations, validation for required fields and identifier format, availability windows add/remove/validation, error handling for both field-level and form-level backend errors, and accessibility attributes. Tests correctly mock the API hooks and use `waitFor` for async assertions with the `hidden: true` option for dialog elements.

**Not tested:** Auto-focus behavior on modal open, keyboard navigation within the windows editor (arrow keys between time inputs), and the edge case where all window rows are removed then the form is submitted.

### Standards compliance

The code follows frontend-standards: functional components with PropTypes, `.jsx`/`.js` extensions, Zod for runtime validation, no `dangerouslySetInnerHTML`, no inline scripts or `eval()`. Naming conventions match (PascalCase components, camelCase hooks, UPPER_SNAKE_CASE constants). The feature is colocated under `features/master-data/asset-management/`.

### Code duplication assessment

Minimal duplication. The `validateWith` function is imported from a shared location, and the feature reuses `ConfigTable` and `ConfirmDeleteDialog` rather than reimplementing them. The campus→department two-step pattern is implemented inline in `AssetFormModal`; if this pattern appears in other features, consider extracting it to a shared component.

### Error handling completeness

Error scenarios covered: API fetch failure (shows retry button), create/update validation failure (inline errors, no request sent), backend field-level errors (mapped to fields), backend generic errors (form-level `role="alert"`), delete failure (error in confirmation dialog). The `mapApiError` function gracefully handles missing `response.data` with a fallback message.

### File Map

| File | Description |
|------|-------------|
| `api/useAssets.js` | TanStack Query hooks for CRUD operations with cache invalidation |
| `api/useAssets.test.jsx` | Unit tests for all CRUD hooks, covering success and error paths |
| `components/AssetFormModal.jsx` | Create/edit modal with two-step campus→department cascade and availability-windows editor |
| `components/AssetFormModal.test.jsx` | Unit tests for modal validation, windows editor, error handling, accessibility |
| `pages/AssetManagementPage.jsx` | Main page with server-side filters, pagination, and CRUD orchestration |
| `schemas/asset-schemas.js` | Zod schemas for asset create/edit with window validation |
| `constants/asset-options.js` | DAYS_OF_WEEK enum matching backend regex |
| `asset-management.css` | Feature-specific styles for filters bar, modal layout, window-row editor |

</details>

---

## Security Checklist

| Check | Status |
|-------|--------|
| No `dangerouslySetInnerHTML` | ✅ Pass |
| Input validation before API calls | ✅ Pass |
| No secrets in frontend code | ✅ Pass |
| Error messages don't expose internals | ✅ Pass |
| XSS prevention (React escaping) | ✅ Pass |

---

## Accessibility Checklist

| Check | Status |
|-------|--------|
| All interactive elements labeled | ✅ Pass |
| Focus trapping in modal | ✅ Pass |
| `aria-invalid` on error fields | ✅ Pass |
| `role="alert"` for form errors | ✅ Pass |
| Keyboard dismissible (Escape) | ✅ Pass |
| Auto-focus first input | ⚠️ Enhancement |

---

## Recommendation

**APPROVED** — No blocking issues found. The 3 minor issues are non-blocking and can be addressed in future iterations.
