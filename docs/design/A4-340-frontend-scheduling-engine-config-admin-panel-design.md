# A4-340 — Design: Frontend Scheduling Engine Configuration Admin Panel (CRUD)

**Jira:** A4-340 (Story) · **Design Subtask:** A4-342 · **Type:** Frontend
**Requirement doc:** `docs/requirements/A4-340-frontend-scheduling-engine-config-admin-panel-requirements.md` (A4-341 — **Approved**)
**Depends on:** A4-335 (SPA foundation) · **Hard prerequisite:** backend CRUD API (see Dependencies / PD-75)
**Status:** Draft for Lead review

---

## 1. Overview

This design implements a frontend admin panel that manages three scheduling-engine configuration entities — **session-derivation rules**, **soft-constraint weights**, and **institution common slots (CCC/UWE)** — as data tables with add / edit / delete, on top of the A4-335 SPA foundation.

The `/scheduling/config` route already exists as a placeholder in `src/app/router.jsx`. This story replaces that placeholder with a real feature module under `src/features/scheduling-config/`.

Stack (from A4-335 / frontend-standards): React 18 (JSX only, **no TypeScript**), Vite, `react-router-dom` v6 (`createBrowserRouter`), TanStack Query v5 (server state), Zustand (client state), axios `apiClient` at `/api/v1`, PropTypes for prop validation, Zod for form validation. UI per ui-standards: Shadcn/ui tables (skeleton loading), Radix-based modals (focus trap), top-right toasts, header campus selector.

---

## 2. Prerequisite & Provisional Decisions

The requirement doc's **blocking OQ-1** stands: the three config tables have no CRUD REST API today. This design specifies the expected backend contract so the frontend can be built against it, but implementation cannot start until that backend story is delivered.

| # | Decision | Resolves (req OQ) | Provisional value | Note |
|---|---|---|---|---|
| PD-75 | Backend exposes REST CRUD for all three entities under `/api/v1/scheduling-config/*` following UTMS api-standards (paged list, 201 create, 200 update, 204 delete, standard error envelope). | OQ-1 | See §3 contract | **Blocking dependency.** Provisional — pending stakeholder ratification. |
| PD-76 | Active campus is chosen via a **header campus selector** (ui-standards header includes one), stored in a Zustand `campusStore`; every config query is keyed by the selected `campusId`. | OQ-3 | Header selector | Provisional. For a single-campus session this defaults to the user's campus. |
| PD-77 | Soft-constraint weight accepted range in the Zod schema: `0.00`–`10.00`, two decimals. | OQ-2 | 0.00–10.00 | Provisional — confirm with stakeholder; backend remains authoritative. |
| PD-78 | Dropdown option sources: component types = static `['L','T','P']`; soft-constraint types = static list mirroring the backend `SoftConstraintType` enum (6 values); days of week = static Mon–Sat; **slot definitions = fetched** from the time-slot grid API for the selected campus. | OQ-5 | Mixed static + API | Provisional. If the backend later exposes a lookup endpoint for component/soft-constraint types, swap the static lists for it to avoid drift. |
| PD-79 | Table page size default 20 (api-standards default). | OQ-6 | 20 | — |
| PD-80 | RBAC not enforced client-side in this story; the screen is available in an authenticated session and the backend authorizes. A `// TODO` marks where role-gating attaches when the Auth module + A4-335 auth context land. | OQ-4 | Deferred | Carried forward. |
| PD-81 | Common-slot uniqueness is a backend decision (OQ-7); the frontend only surfaces any 409 it returns. | OQ-7 | Surface 409 | Carried forward. |

---

## 3. Expected Backend Contract (PD-75 — to be built by the prerequisite backend story)

All paths under `/api/v1/scheduling-config`. All list endpoints paged (`?campusId=&page=&size=&sort=`) and return the standard `{ data, meta }` envelope; errors use the standard error envelope (`timestamp,status,error,message,path,details`).

| Entity | List | Create | Update | Delete |
|---|---|---|---|---|
| Derivation rules | `GET /derivation-rules?campusId=` | `POST /derivation-rules` (201) | `PUT /derivation-rules/{id}` (200) | `DELETE /derivation-rules/{id}` (204) |
| Soft-constraint weights | `GET /soft-constraint-weights?campusId=` | `POST /soft-constraint-weights` (201) | `PUT /soft-constraint-weights/{id}` (200) | `DELETE /soft-constraint-weights/{id}` (204) |
| Common slots | `GET /common-slots?campusId=` | `POST /common-slots` (201) | `PUT /common-slots/{id}` (200) | `DELETE /common-slots/{id}` (204) |

**Response DTO shapes the frontend consumes** (field names align with the V10 schema / entities):

```js
// DerivationRuleDto
{ id, campusId, componentType, slotDurationMinutes, hoursPerSession, description, isActive }
// SoftConstraintWeightDto
{ id, campusId, constraintType, weight, isActive }
// CommonSlotDto
{ id, campusId, name, dayOfWeek, slotDefinitionId, appliesToAllBatches, isActive }
```

**Expected status codes the frontend handles:** 201/200/204 success; 400 (field validation → `details[]`); 409 (duplicate `(campus,type)` for rules/weights, or common-slot uniqueness if backend adds it); 422 (business rule); 404; 500.

> If the delivered backend diverges from this contract, the API hooks (§5.3) and Zod schemas (§5.4) are the only places that change.

---

## 4. Module Structure

```
src/features/scheduling-config/
├── pages/
│   └── SchedulingConfigPage.jsx        # Route target; tabs for the 3 entities + campus guard
├── components/
│   ├── ConfigTable.jsx                 # Generic sortable/paginated table (columns + row actions via props)
│   ├── ConfirmDeleteDialog.jsx         # Reusable delete confirmation (Radix Dialog)
│   ├── DerivationRuleTable.jsx         # Wires ConfigTable + columns + hooks for rules
│   ├── DerivationRuleFormModal.jsx     # Add/Edit modal (Zod-validated)
│   ├── SoftWeightTable.jsx
│   ├── SoftWeightFormModal.jsx
│   ├── CommonSlotTable.jsx
│   └── CommonSlotFormModal.jsx
├── api/
│   ├── useDerivationRules.js           # list/create/update/delete query+mutation hooks
│   ├── useSoftWeights.js
│   ├── useCommonSlots.js
│   └── useSlotDefinitions.js           # lookup for common-slot form (time-slots API)
├── schemas/
│   └── config-schemas.js               # Zod schemas for all 3 forms
└── constants/
    └── config-options.js               # componentTypes, softConstraintTypes, daysOfWeek (PD-78)

src/stores/campusStore.js               # selected campusId (PD-76) — shared client state
src/lib/api-error.js                    # maps backend error envelope → field errors / message (shared)
```

The `/scheduling/config` route in `src/app/router.jsx` is repointed from `PlaceholderPage` to `SchedulingConfigPage` (lazy-loaded via `React.lazy` per frontend-standards code-splitting).

---

## 5. Component & Logic Design

### 5.1 SchedulingConfigPage
- Renders page header (title + campus context) and a Radix Tabs group: **Derivation Rules · Soft-Constraint Weights · Common Slots**.
- Reads `campusId` from `campusStore`. If none selected, renders an empty-state prompting campus selection (guards all three queries — no query fires without a campusId).
- Each tab lazily renders its table component. Tabs are keyboard-navigable (Radix handles roving tabindex).

### 5.2 ConfigTable (generic)
- Props: `columns` (array of `{ key, header, sortable, render? }`), `rows`, `isLoading`, `isError`, `onRetry`, `onEdit(row)`, `onDelete(row)`, pagination props.
- States (ui-standards): loading → skeleton rows; error → message + Retry button (`onRetry`); empty → empty-state with an Add CTA; populated → table.
- Actions column: icon buttons (Edit, Delete) with `aria-label`s (a11y — icon-only buttons need labels).
- Sorting: client-side for these small config sets (or passes `sort` to the query — both acceptable, small data). Pagination via TanStack Query params (default size 20, PD-79).
- All cell values rendered as text (React auto-escapes; **no `dangerouslySetInnerHTML`** — UI-3 / NFR-2).

### 5.3 API hooks (TanStack Query v5)
Pattern per entity (example `useDerivationRules.js`):

```js
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api-client';

const BASE = '/scheduling-config/derivation-rules';
const key = (campusId) => ['derivation-rules', campusId];

export function useDerivationRules(campusId) {
  return useQuery({
    queryKey: key(campusId),
    queryFn: async () => (await apiClient.get(BASE, { params: { campusId } })).data,
    enabled: campusId != null, // guard: no fetch until a campus is chosen (PD-76)
  });
}

export function useCreateDerivationRule(campusId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body) => (await apiClient.post(BASE, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: key(campusId) }), // refresh table (FR-3.3)
  });
}
// useUpdate... (PUT /{id}), useDelete... (DELETE /{id}) follow the same shape,
// each invalidating key(campusId) on success (FR-4.3 / FR-5.3).
```

Mutations expose `isPending` so form submit buttons show a spinner + disabled state (ui-standards action-pending).

### 5.4 Zod schemas (`config-schemas.js`)
Mirror the requirement doc Validation Rules table:

```js
import { z } from 'zod';
import { COMPONENT_TYPES, SOFT_CONSTRAINT_TYPES, DAYS_OF_WEEK } from '../constants/config-options';

export const derivationRuleSchema = z.object({
  componentType: z.enum(COMPONENT_TYPES),                 // OQ-5 / PD-78
  slotDurationMinutes: z.coerce.number().int().positive(),
  hoursPerSession: z.coerce.number().min(0).max(99.9)     // DECIMAL(3,1)
    .refine((n) => Number((n * 10).toFixed(0)) === n * 10, 'At most one decimal place'),
  description: z.string().max(200).optional().or(z.literal('')),
});

export const softWeightSchema = z.object({
  constraintType: z.enum(SOFT_CONSTRAINT_TYPES),          // 6 enum values
  weight: z.coerce.number().min(0).max(10),               // PD-77 (provisional)
});

export const commonSlotSchema = z.object({
  name: z.string().min(1).max(100),
  dayOfWeek: z.enum(DAYS_OF_WEEK),
  slotDefinitionId: z.coerce.number().int().positive(),   // from useSlotDefinitions
  appliesToAllBatches: z.boolean().default(true),
});
```

`campusId` is injected from `campusStore` at submit time (not a form field) so a record is always scoped to the active campus.

### 5.5 Form modals (Add / Edit)
- Radix `Dialog` (focus trap + Escape dismissal + overlay — NFR-3 / ui-standards modals).
- Fields per requirement doc §5. Labels above inputs; required marked; inline errors below field on blur/submit (ui-standards forms).
- On submit: run the Zod schema. On failure → set field-level errors, **send no request** (FR-6.3 / UI-1). On success → call the create/update mutation.
- Mutation error → map via `mapApiError` (§5.6): 409 → attach a duplicate message near the type field and keep the modal open (FR-3.4 / FR-4.4 / AC-6); 400 `details[]` → attach per-field; otherwise → error toast.
- Edit modal is pre-filled from the row (FR-4.1).

### 5.6 Error mapping (`src/lib/api-error.js`)
```js
export function mapApiError(error) {
  const res = error?.response?.data;
  if (res?.details?.length) {
    // field-level: [{ field, message }] → { [field]: message }
    return { fields: Object.fromEntries(res.details.map((d) => [d.field, d.message])) };
  }
  return { message: res?.message || 'Something went wrong. Please try again.' };
}
```
Never surfaces stack traces / internal detail (NFR-2 / FR-7.1). Success → success toast (FR-7.2); list-load failure → ConfigTable error state (FR-7.3).

### 5.7 Delete flow
`ConfirmDeleteDialog` names the record (FR-5.1); confirm → delete mutation (backend soft-deletes, FR-5.2); success → invalidate query so the row disappears (FR-5.3) + toast; cancel → close, no change (FR-5.4 / AC-8).

---

## 6. State Management

- **Server state:** TanStack Query per entity, keyed by `campusId`. Mutations invalidate the entity's query key to refresh tables. Aligns with A4-335 QueryClient defaults (`staleTime 60s`, `retry 1`).
- **Client state:** `campusStore` (Zustand) holds the selected `campusId`; the header campus selector writes it, config queries read it (PD-76). Modal open/close and edit-target held in local component state (`useState`), not global.

---

## 7. Routing

`src/app/router.jsx`: replace the `/scheduling/config` placeholder element with `<SchedulingConfigPage />` (lazy). No new routes; nav item already exists in `AppShell`.

---

## 8. Traceability

| Requirement | Design element |
|---|---|
| FR-1 shell / tabs | SchedulingConfigPage (§5.1), router repoint (§7) |
| FR-2 tables (sort/paginate/actions) | ConfigTable (§5.2) + per-entity table wrappers |
| FR-3 add | Form modals (§5.5) + create hooks (§5.3) |
| FR-4 edit | Pre-filled modal (§5.5) + update hooks |
| FR-5 delete (soft, confirm) | ConfirmDeleteDialog + delete hooks (§5.7) |
| FR-6 Zod validation | config-schemas.js (§5.4) |
| FR-7 error/feedback | mapApiError + toasts + table error state (§5.6) |
| UI-1..4 (owned constraints) | §5.4 (no request until valid), §5.7 (confirm), §5.2/5.6 (encode + safe errors) |
| Referenced 409 uniqueness | §5.5 mutation-error handling |
| NFR-1 perf | small data + query caching (§6) |
| NFR-2 XSS | text rendering, no dangerouslySetInnerHTML (§5.2) |
| NFR-3 a11y | Radix Dialog focus trap, aria-labels, keyboard tabs (§5.1/5.2/5.5) |
| NFR-4 consistency | reuse A4-335 shell + Shadcn primitives |
| Req OQ-1..7 | PD-75..81 (§2) |

---

## 9. Dependencies & Consistency Notes

- **Blocking:** Backend CRUD API (PD-75) must exist first. The prerequisite backend story does **not** exist yet (Jira search returned 0). It must be created and completed before this frontend implementation begins.
- **`zod` is not yet in `frontend/package.json`** (deps: react-query, axios, prop-types, react, react-dom, react-router-dom, zustand). It must be added (pinned version) as part of this story. Shadcn/ui + Radix + Lucide (ui-standards component library) must also be present or added via A4-335; if A4-335 has not yet installed them, that is a dependency to confirm.
- **`campusStore` / header campus selector** (PD-76): if A4-335 already provides a campus selector, reuse it; otherwise this story adds it. Confirm ownership with A4-335.
- Provisional decisions PD-75..81 continue the project PD sequence (A4-11 ended at PD-74).
- Every requirement-doc Open Question (OQ-1..7) is dispositioned in §2 (resolved-provisional or carried-forward) — none silently dropped.

---

## 10. Out of Scope (unchanged from requirements)

Backend CRUD implementation (separate prerequisite story); scheduled-session editing / drag-drop (A4-15); generation trigger & draft viewing (separate story); RBAC rule definition (backend + auth); bulk import/export.

---

## 11. Open Questions (carried to design review)

| # | Question | Owner |
|---|---|---|
| OQ-D1 | Confirm creation and scope of the prerequisite backend CRUD story (PD-75). **Blocking.** | Product Owner / Lead |
| OQ-D2 | Ratify provisional weight range 0.00–10.00 (PD-77). | Product Owner |
| OQ-D3 | Does A4-335 already provide the header campus selector + Shadcn/Radix setup, or does this story add them (PD-76, §9)? | Lead / A4-335 owner |
| OQ-D4 | Should component-type and soft-constraint-type options come from a backend lookup endpoint instead of static lists to prevent drift (PD-78)? | System Design |

---

*Prepared for Lead review (A4-342). Implementation is gated on OQ-D1 (backend CRUD API).*
