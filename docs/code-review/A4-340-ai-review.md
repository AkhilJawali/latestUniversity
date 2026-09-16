# Scheduling Engine Configuration Admin Panel (A4-340)

A React 18 plain-JSX admin panel that adds table-based CRUD for three scheduling-engine config entities (session-derivation rules, soft-constraint weights, institution common slots) on top of the A4-335 SPA foundation. The feature is built from a generic trio — `ConfigTable`, `ConfigFormModal`, `ConfirmDeleteDialog` — that each of the three tabs wires to its own TanStack Query hook set. A page-level campus picker writes the selected campus into a Zustand store, and every query stays disabled until a campus is chosen. Validation is Zod-on-submit; the modal blocks the request when the schema fails and maps the backend error envelope back to per-field messages.

Watch for: a tab widget that declares `role="tablist"`/`role="tab"` but omits the `aria-controls`/`aria-labelledby`/`id` wiring and arrow-key navigation the pattern requires (confirmed); an `isActive` flag that the schemas accept and the tables display but no form can ever set (confirmed); and a hard runtime crash risk if the backend list payload is ever a bare array instead of the `{ data }` envelope (likely).

The accepted deviations (real A4-390 endpoints, LECTURE/TUTORIAL/PRACTICAL enums, plain CSS + native `<dialog>`, useState+Zod instead of RHF, page-level campus picker, numeric slot-definition input) are all reasonable and internally consistent with the A4-335 foundation and the existing generation feature. None are treated as defects.

**Verdict**: COMMENT

## High-level view

Routing is complete and correct: `/scheduling/config` is repointed to a `React.lazy` page wrapped in `Suspense` inside the shell, matching the generation route's pattern exactly. The route is reachable and code-split as intended.

The generic-component approach is the right call at three entities. The three tabs and three hook modules are near-identical, but the duplication is shallow (config arrays + thin wiring) and extracting a factory now would trade readability for premature abstraction. This is acceptable at three; it's worth revisiting if a fourth entity lands.

State-management steering is followed cleanly. Server state lives in TanStack Query with a consistent `['scheduling-config', <entity>, 'list', campusId]` key structure, mutations invalidate the matching list key on success, and Zustand holds only the UI-level campus selection. The list-data access (`list.data?.data`) is consistent across all three tabs and matches what the hooks return (the axios `.data`, which is the backend `{ data, meta }` envelope) — but it is unguarded against a non-enveloped response.

Security posture is sound for a rendering-only CRUD surface: all cells and messages go through normal JSX text interpolation (React escapes), there is no `dangerouslySetInnerHTML`, no `eval`, no secrets, and the error mapper deliberately surfaces only `message` and per-field `details` — never stack traces or internal paths. Form validation correctly satisfies AC-5: an invalid submit sets field errors and returns before any mutation fires.

Accessibility is the weakest area. The native `<dialog>` gives a real focus trap and Escape handling for free, icon buttons carry `aria-label`s, and the table exposes `aria-busy` — all good. But the tab widget is only half-implemented against the ARIA tabs pattern, and that's the main thing holding this back from a clean approve.

<details>
<summary>Issues (8)</summary>

1. **Incomplete tab ARIA wiring** — tabs declare `role="tab"` and the container `role="tablist"`, but there are no `id`s, no `aria-controls` on tabs, and no `aria-labelledby` on the single `role="tabpanel"`. Screen readers can't associate the selected tab with its panel. Add the id/aria wiring. (confirmed)
2. **No keyboard navigation between tabs** — tab selection is `onClick`-only; the ARIA tabs pattern expects Left/Right arrow navigation and roving tabindex. Either add arrow-key handling or drop the tab roles and present them as a plain button group. (confirmed)
3. **`isActive` is unsettable** — the Zod schemas accept `isActive`, the tables render an "Active" column, and edit merges an existing row's value, but no form exposes a control for it and `BLANK` omits it, so a newly created row can never set active/inactive. Add a checkbox field or remove the column until it's supported. (confirmed)
4. **Unguarded envelope access can crash** — tabs read `list.data?.data ?? []`. If any endpoint returns a bare array (no `{ data }` wrapper), `.data` is `undefined` and the table silently shows empty; if the shape drifts to a non-array, downstream `.map` throws. Normalize in the hook (`res.data?.data ?? res.data ?? []`). (likely)
5. **Edit sends server-only fields back** — `initialValues={{ ...BLANK, ...editing }}` spreads the whole row (including `id`, `campusId`, timestamps) into form values, and `handleSubmit` forwards all of `data` to the PUT body. Confirm the backend allowlist rejects/ignores extras, or pick only schema fields before mutating. (confirmed)
6. **NaN campus id escapes the guard** — `campusStore.readInitial()` does `raw ? Number(raw) : null`; a non-numeric `sessionStorage` value yields `NaN`, and both the page guard (`campusId == null`) and the query gate (`enabled: campusId != null`) treat `NaN` as a valid campus, firing `GET ...?campusId=NaN`. Low likelihood (the store only writes validated positive ints) but unguarded; use `Number.isInteger` in `readInitial`. (confirmed)
7. **No explicit modal close (X) control** — Escape works via the native `<dialog>` `onCancel`, but ui-standards asks for a visible close control on modals. Add one for discoverability. (confirmed)
8. **No focus-visible / reduced-motion styling** — the CSS defines no focus ring for `.tab`/`.icon-btn` beyond the UA default and no `prefers-reduced-motion` guard. Low impact (no animation present) but confirm focus indicators meet WCAG. (possible)

</details>

<details>
<summary>Details</summary>

### Routing: lazy config page reachable and split

`router.jsx` repoints `/scheduling/config` at a `lazy(() => import('.../SchedulingConfigPage'))` wrapped in `<Suspense fallback={...}>` inside `<AppShell>`, identical in shape to the existing `/scheduling/generation` route. This is complete and correct — the route is reachable, the page is code-split, and the fallback matches the app's empty-state convention. No concern.

### Generic-component reuse vs. duplication across three tabs

`DerivationRuleTab`, `SoftWeightTab`, and `CommonSlotTab` are structurally identical: each declares `COLUMNS`, `FIELDS`, `BLANK`, calls its four hooks, holds `formOpen`/`editing`/`deleting` state, and wires the same three generic components with the same `handleSubmit` shape. The three hook modules (`useDerivationRules`, `useSoftWeights`, `useCommonSlots`) differ only in `BASE` and the query-key entity segment.

At three entities this is the right amount of abstraction. The genuinely reusable surface — table, form modal, delete dialog — is already extracted into shared components; what remains duplicated is declarative config (column/field arrays) plus a dozen lines of near-boilerplate wiring. Collapsing the tabs into a single config-driven factory or the hooks into a `makeCrudHooks(base, entity)` generator would remove ~40 lines but obscure each entity's specifics behind indirection. The duplication is shallow and each file reads on its own. Revisit if a fourth entity arrives — that's the point where a factory pays for itself.

### List-data access is consistent, but the envelope is assumed

Each hook's `queryFn` returns `(await apiClient.get(BASE, { params: { campusId } })).data` — the axios response body, which per api-standards is the `{ data, meta }` envelope. Every tab then reads `list.data?.data ?? []`. So `list.data` is the envelope and `list.data.data` is the array. This is consistent across all three tabs and all three hooks — the claim checks out.

The residual risk is that the access is unguarded against shape drift. The optional chain protects against `list.data` being `undefined` (in-flight / error), but if an endpoint ever returns a bare array or a differently-named payload, the table quietly renders empty with no error surfaced. Normalizing inside the hook (`return res.data?.data ?? res.data ?? []`) would make the tabs read `list.data ?? []` and remove the double-`.data` foot-gun. Not blocking, but it's the kind of coupling that breaks silently.

### State management follows the steering split

Server state is entirely in TanStack Query; the only Zustand store (`campusStore`) holds a single UI-scoped value (the selected campus id) and persists it to `sessionStorage` — exactly the "client/UI state only, never duplicate server data" rule. Query keys follow a clean `['scheduling-config', <entity>, 'list', campusId]` hierarchy, and each mutation invalidates precisely its own list key on success. `enabled: campusId != null` correctly gates every query behind a chosen campus, and `staleTime: 5 min` matches the master-data guidance. This is compliant and idiomatic.

The one rough edge is initialization: `readInitial()` reads the persisted value with `raw ? Number(raw) : null`. A valid write always stores a positive integer string, so the happy path is fine, but a corrupt or tampered `sessionStorage` entry (`"abc"`) produces `NaN`. `NaN == null` is `false` and `NaN != null` is `true`, so both the page's render guard and every hook's `enabled` gate accept it, and the queries fire with `campusId=NaN` in the query string. Guarding the read with `Number.isInteger(n) && n > 0` (the same check the page already applies on submit) closes this.

One small note: the steering's canonical query-key example nests under a per-resource `queryKeys` object; here each hook file defines a local `keys.list`. Functionally equivalent and arguably cleaner for a self-contained feature — no change needed.

### Form validation blocks the request on invalid input (AC-5)

`ConfigFormModal.submit` runs `validateWith(schema, values)` first; on `!result.success` it sets field errors and `return`s before ever calling `onSubmit`, so no mutation fires. On success it clears errors and calls `onSubmit(result.data, { onError })`, passing the *coerced* Zod output (not raw strings) to the mutation. The `onError` handler maps the backend envelope: 400 field details become per-field errors, everything else becomes a single `role="alert"` form message. This is a correct, complete implementation of AC-5 and the error-mapping requirement.

`validateWith` keeps only the first issue per field (`errors[field] == null` guard), which is the right UX for inline errors. `noValidate` on the form correctly defers to Zod rather than native browser validation.

### The `isActive` gap

`derivationRuleSchema`, `softWeightSchema`, and `commonSlotSchema` all declare `isActive: z.boolean().optional()`. All three tables render an "Active" column via a `render` that reads `r.isActive`. On edit, `{ ...BLANK, ...editing }` carries the existing row's `isActive` into form values. But no `FIELDS` array includes an `isActive` control, and no `BLANK` seeds it (except `CommonSlotTab` which seeds `appliesToAllBatches`, a different flag). The net effect: a user can never set or toggle `isActive` from this UI — created rows omit it entirely, and edited rows can only preserve whatever the server already had. Either add a checkbox field to each `FIELDS` spec (the modal already supports `type: 'checkbox'`) or drop the column until the control exists, so the UI doesn't advertise state it can't manage.

### Accessibility: dialog good, tabs half-done

The native `<dialog>` + `showModal()` approach is a genuine strength: it provides a real focus trap, Escape-to-dismiss (wired through `onCancel`), and an inert backdrop without any custom focus-management code. Icon buttons carry descriptive `aria-label`s (`Edit Derivation Rules 7`), the table sets `aria-busy` during load, error text uses `role="alert"`, and field errors are associated via `aria-describedby` + `aria-invalid`. That's a solid baseline.

The tab widget is where it falls short of the pattern it declares. It sets `role="tablist"` on the container and `role="tab"` + `aria-selected` on each button, and renders one `<div role="tabpanel">`. Missing: each tab needs an `id` and `aria-controls` pointing at the panel; the panel needs `aria-labelledby` pointing back at the active tab; and the pattern expects arrow-key navigation with a roving `tabIndex` (only the active tab in the tab order). As written, a screen-reader user hears "tab, selected" but has no programmatic link to the panel, and keyboard users can't move between tabs with arrows. The cheapest correct fix is often to *not* claim the roles — a labeled group of buttons that swap content is fully accessible without ARIA — but if the roles stay, the wiring must be completed. This is the primary reason for COMMENT rather than a clean approve.

The modal also lacks an explicit close (X) button; Escape dismisses it via the native `onCancel`, but ui-standards calls for a visible close affordance in addition to Escape.

### Security: escaping and error mapping

Every dynamic value reaches the DOM through JSX text interpolation — table cells (`String(row[col.key] ?? '')` or a `render` returning text), entity labels, error messages, the delete label. React escapes all of it; there is no `dangerouslySetInnerHTML`, no `innerHTML`, no `eval`, and no template that could reintroduce an injection sink. Grep across the feature confirms none of `dangerouslySetInnerHTML | eval | token | secret | apiKey` appears.

`mapApiError` is deliberately conservative: it reads only `error.response.data`, extracts `details[].field/message` for 400s and a single `message` otherwise, and falls back to a generic "Something went wrong" string. It never touches `error.stack`, the request config, or the `path`/`status` internals — so backend internals can't leak into the UI. The `.filter((d) => d && d.field)` guard also prevents malformed detail entries from producing `undefined` keys. This matches NFR-2 / FR-7.1.

### Loading / error / empty states

`ConfigTable` covers all four states explicitly: error renders a card with a `role="alert"` message and a Retry button wired to `list.refetch`; loading renders three skeleton rows under an `aria-busy` table; empty renders a centered "No … yet" row with an inline Add CTA; populated renders rows with Edit/Delete actions. The states are mutually exclusive and the transitions are driven by the query flags. This is complete and matches the ui-standards skeleton-over-spinner guidance. Mutation-pending states disable the modal/dialog buttons and swap in "Saving…"/"Deleting…" labels — good feedback.

### No TypeScript

Confirmed: all feature files are `.js`/`.jsx`, prop validation uses PropTypes, runtime validation uses Zod, and a source-tree search for `.ts`/`.tsx` returns nothing (the only `.d.ts` hits are inside `node_modules`). The NO-TypeScript rule holds.

</details>

<details>
<summary>File map</summary>

- `app/router.jsx` — `/scheduling/config` repointed to a lazy `SchedulingConfigPage` under Suspense; matches the generation route pattern.
- `features/scheduling-config/pages/SchedulingConfigPage.jsx` — campus picker (writes `campusStore`) + campus guard + three-entity tabs. Tab ARIA wiring incomplete; no arrow-key nav.
- `features/scheduling-config/components/ConfigTable.jsx` — generic table with loading/error/empty/populated states, `aria-busy`, aria-labelled icon buttons. Solid.
- `features/scheduling-config/components/ConfigFormModal.jsx` — generic add/edit modal; native `<dialog>`, Zod-on-submit, blocks request on invalid, maps API errors. Correct AC-5 behavior; forwards full row on edit; no explicit close (X).
- `features/scheduling-config/components/ConfirmDeleteDialog.jsx` — native `<dialog>` delete confirm; focus trap + Escape for free.
- `features/scheduling-config/components/{DerivationRule,SoftWeight,CommonSlot}Tab.jsx` — thin wiring of the generics to each hook set; near-identical, acceptable at three. `isActive` column shown but unsettable.
- `features/scheduling-config/api/use{DerivationRules,SoftWeights,CommonSlots}.js` — TanStack Query v5 CRUD hooks against the real A4-390 endpoints; consistent query keys, invalidate-on-success. Returns the `{ data, meta }` envelope.
- `features/scheduling-config/schemas/config-schemas.js` — Zod schemas (bounds reconciled to A4-390 PD-93) + `validateWith` helper. Declares `isActive` that no form sets.
- `features/scheduling-config/constants/config-options.js` — `COMPONENT_TYPES` (3), `SOFT_CONSTRAINT_TYPES` (6), `DAYS_OF_WEEK` (7), mirroring backend enums.
- `features/scheduling-config/scheduling-config.css` — plain CSS for picker/tabs/table/modal; no visible focus-ring override, no reduced-motion guard.
- `stores/campusStore.js` — Zustand, `sessionStorage`-persisted `campusId`; UI-only state, steering-compliant. `readInitial` doesn't guard against a NaN from a corrupt stored value.
- `lib/api-error.js` — maps backend `{ message, details[] }` envelope to `{ fields }` or `{ message }`; no internal leakage.

Reviewed as a set of files (target is not a git working tree, so no diff was available). Full files read directly.

</details>
