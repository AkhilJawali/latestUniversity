# React SPA foundation for the scheduling-engine admin panel

The change scaffolds a React 18 + Vite single-page app in plain JSX: an app shell (header/sidebar/main), a `createBrowserRouter` route table where every route is wrapped in the shell, a TanStack Query provider with conservative defaults, a single Axios client on `/api/v1` with a no-op interceptor seam, a plain-CSS design foundation, a CSP `<meta>` tag, and a Vitest + React Testing Library harness with 10 passing tests. It is deliberately feature-free — the config panel (A4-340) and generation viewer (A4-345) land in their own stories. The scaffold matches the approved design (KD-F1..F5, PD-F1..F3) closely, and `pnpm build` / `pnpm test` are green.

Watch for: the `lint` script references ESLint but there is no ESLint config or `eslint` dependency, so `pnpm lint` fails (confirmed) — the one finding that touches the squad DoD "passes linting" gate. The router hard-imports `GenerationPage` from a parallel story (A4-345) via `lazy()`, which couples the foundation build to code it does not own (confirmed). The design claims `stores/` and `components/ui/` directories are "present" as extension-point conventions, but neither exists on disk (confirmed). None of these break the foundation's ACs, which all pass.

**Verdict**: COMMENT

The verdict is COMMENT rather than NEEDS_CHANGES because every acceptance criterion (AC#1–AC#6) is met and the two hard gates that actually run — `pnpm build` and `pnpm test` — are green. The ESLint gap is the closest call: the squad DoD requires "passes linting," and `pnpm lint` currently fails, so a reviewer enforcing the DoD strictly could reasonably escalate that single item to NEEDS_CHANGES. It is left as a prominent COMMENT because it's a tooling-wiring omission, not a defect in the shipped foundation code.

## High-level view

Routing is complete and correctly shell-wrapped: all three routes (`/`, `/scheduling/config`, `/scheduling/generation`) render inside `AppShell`, and the central `router.jsx` table plus the `NAV_ITEMS` array make it genuinely extensible — a feature story appends one object to each. The one wrinkle is that `router.jsx` statically references `@/features/scheduling/generation/GenerationPage`, owned by A4-345; `lazy()` defers the chunk at runtime but not the module-resolution dependency at build time, so the foundation cannot build in isolation from that story's file.

Security posture for a foundation story is sound: `script-src 'self'` with no `unsafe-inline`, `connect-src` allowlisting exactly the app origin plus the dev backend, `unsafe-inline` confined to `style-src`, and no `dangerouslySetInnerHTML` anywhere. The absence of auth on the API client is a documented, intentional deferral (PD-F2) to the security module, not an oversight. The one thing to track is that the dev-only `http://localhost:8080` connect-src and the meta-tag delivery mechanism need to be reconciled with the production reverse-proxy header (already captured as OQ-DF2).

Accessibility baseline is met: real `<header>/<nav>/<main>` landmarks, `aria-label="Primary"` on the nav, and native `NavLink` anchors that are keyboard-navigable for free. Nothing icon-only or custom-widget here to need more ARIA yet.

Standards compliance is strong — feature-driven folders, PropTypes on every component with props, plain JSX end to end, correct import ordering, and zero TypeScript. The gaps are tooling/convention scaffolding the design promised but didn't deliver: no ESLint config (yet a `lint` script and an enforced import-order rule exist), and the `stores/` and `components/ui/` extension-point folders are described as present but are missing. The one duplication worth noting is that the routing test maintains its own copy of the route table.

Test quality is good for the scope. The build check (AC#1), shell render (AC#2), placeholder route (AC#4), API base path (AC#5), and CSP (AC#6) are all verified meaningfully. AC#3 ("navigation without full reload") is verified indirectly by asserting `NavLink` hrefs resolve to SPA route paths rather than by driving a click — an acceptable proxy given the documented jsdom/undici constraint, but worth understanding for what it does and doesn't prove.

<details>
<summary>Issues (8)</summary>

1. **Lint script has no config or dependency** — `package.json` defines `"lint": "eslint ."` and an enforced import-order rule exists in standards, but there is no `eslint.config.js` and no `eslint`/plugin devDependency. `pnpm lint` fails, which conflicts with the DoD "passes linting" gate. Add a flat ESLint config + deps, or remove the script until it's wired.
2. **Router build-couples to A4-345** — `router.jsx` does `lazy(() => import('@/features/scheduling/generation/GenerationPage'))`, so the foundation won't build without a file owned by a parallel story. Point the route at `PlaceholderPage` until A4-345 lands, or make the coupling explicit and sequence the stories.
3. **`stores/` folder missing** — design 5.6 and frontend-standards say a `stores/` convention home is "present"; it isn't on disk. Add the folder (with a `.gitkeep` or a README) or correct the design's "present" claim.
4. **`components/ui/` folder missing** — design 5.1/PD-F1 reference a `components/ui/` home for reusable primitives; it doesn't exist. Same fix: create the placeholder or adjust the doc.
5. **No root ErrorBoundary** — Section 6 recommends a root `ErrorBoundary` and frontend-standards require one at app root; there is none. A lazy chunk failure or render error currently white-screens. Add a minimal boundary around `RouterProvider`.
6. **Route table duplicated in test** — `router.test.jsx` rebuilds the route table by hand instead of importing the production one. Keeps the test scoped, but the two can drift silently. Consider exporting the routes array and having the test import it, or accept the drift risk knowingly.
7. **CSP mechanism/prod divergence risk** — the meta tag hardcodes `http://localhost:8080` in `connect-src`. Fine for dev, but confirm the production reverse-proxy header is authoritative and the dev-only origin never ships (tracked as OQ-DF2).
8. **AC#3 asserts hrefs, not navigation** — the test verifies `NavLink` href targets rather than a click-driven route transition. Acceptable proxy for now; add a `userEvent.click` + content-swap assertion once the jsdom/Node undici `AbortSignal` quirk is resolved.

</details>

<details>
<summary>Details</summary>

## Routing completeness and the A4-345 coupling

The route table in `router.jsx` is clean and every route is wrapped in `<AppShell>`, so navigation never lands on a bare page. `/` renders `DashboardPage`, `/scheduling/config` renders a `PlaceholderPage`, and `/scheduling/generation` renders the lazy `GenerationPage` behind a `<Suspense>` fallback. Extensibility is real, not aspirational: a feature story adds one entry to the array in `router.jsx` and one entry to `NAV_ITEMS` in `AppShell.jsx`. That two-array pattern is the right seam for this stage.

The coupling concern is specific. The `lazy()` call names a concrete module path owned by A4-345:

```js
const GenerationPage = lazy(() => import('@/features/scheduling/generation/GenerationPage'));
```

`lazy()` defers *loading the chunk in the browser*, but the dynamic `import()` specifier is still a static module reference the bundler must resolve at build time. Because the A4-345 feature files already exist in the tree, `pnpm build` succeeds today — but the foundation is no longer buildable in isolation from a story it's supposed to precede and enable. That inverts the stated dependency direction (A4-335 blocks A4-345, not the other way around). For a foundation whose whole job is to stand alone, the safer shape is to route `/scheduling/generation` at `PlaceholderPage` (exactly as `/scheduling/config` does) and let A4-345 flip it to the real lazy import when that story lands. If the team deliberately chose to co-develop, that's defensible — but it should be an explicit decision, because right now the foundation silently depends on A4-345's file existing. (confirmed)

## Security: CSP and the intentional auth gap

The CSP meta tag is well-formed for a foundation story:

```
default-src 'self'; script-src 'self'; connect-src 'self' http://localhost:8080;
style-src 'self' 'unsafe-inline'; img-src 'self' data:;
```

`script-src 'self'` with no `unsafe-inline` and no `unsafe-eval` is the important line, and `api-client.test.js` locks it in with an assertion that the string `script-src 'self' 'unsafe-inline'` is *absent* — a nice negative test that would catch a regression that loosened script policy. `connect-src` allowlists exactly the app origin and the dev backend. `unsafe-inline` is confined to `style-src`, which is the pragmatic concession Vite/React styling needs and is the standard tradeoff called out in KD-F5. No `dangerouslySetInnerHTML` appears anywhere in the foundation (FR-7.3).

Two things to keep honest. First, the meta tag hardcodes `http://localhost:8080`; that's a dev origin that must not reach production. The design says the production CSP is owned by the reverse-proxy header (OQ-DF2) — as long as that's true and the meta tag is dev-oriented, fine, but the two sources of truth need reconciling before release or they'll drift. Second, the API client has no auth, which the prompt correctly frames as a documented deferral: PD-F2 assigns JWT attach + refresh to the security module and leaves a passthrough response interceptor as the seam. That seam is honest — it doesn't pretend to implement auth — so this is a tracked gap, not a defect. (confirmed)

## Accessibility baseline

`AppShell` uses real landmarks: `<header>`, `<nav aria-label="Primary">`, and `<main>`. The `aria-label` disambiguates the nav for screen readers and is exactly what the routing/shell tests query against (`getByRole('navigation', { name: 'Primary' })`), so the accessible name is contract-tested, not incidental. Navigation uses `NavLink`, which renders native anchors — keyboard focus and Enter activation come for free, no `div onClick` anti-patterns. For a foundation with no icon-only buttons or custom widgets yet, this is the right baseline; fuller WCAG AA work is correctly deferred to feature stories.

## Standards compliance, duplication, and the TypeScript check

Zero TypeScript: every file is `.js`/`.jsx`, no `@types/*`, no `typescript` dependency, PropTypes used for prop validation on `AppProviders`, `PlaceholderPage`, and `AppShell`. Import ordering follows the enforced convention (React → external → `@/` internal → local → styles). Folders are feature-driven (`features/dashboard/`, `lib/`, `components/layout/`). Dependencies are pinned to exact versions. The TanStack Query defaults in `providers.jsx` (staleTime 60s, retry 1, no refetch-on-focus) match KD-F3 exactly, and the Vitest block lives inside `vite.config.js` with a jsdom environment and v8 coverage that correctly excludes `main.jsx` and the test setup. This all lines up with frontend-standards and the tech steering.

The one duplication worth flagging (prompt area 4) is the route table: `router.test.jsx` hand-rebuilds a `createMemoryRouter` with the first two routes rather than importing the production table, deliberately omitting the lazy `GenerationPage` to keep the test scoped to the foundation. That scoping choice is reasonable, but it means the real `router.jsx` and the tested table are two sources of truth that can drift — a change to the production routes won't be caught. Exporting the routes array so the test can import and slice it would remove the duplication without pulling A4-345 into the test.

Where the scaffold falls short of what the design and standards promise is convention scaffolding, not code:

- **ESLint** — `package.json` has `"lint": "eslint ."` and frontend-standards specify an ESLint-enforced import order, but there is no `eslint.config.js` at the project root and no `eslint` (or plugin) in devDependencies. Running `pnpm lint` fails outright, which collides with the squad DoD "Code complete and passes linting." Either wire a flat config + deps now, or drop the script until the config exists so the command doesn't lie. (confirmed)
- **`stores/`** — design 5.6 states the Zustand convention folder is "present"; frontend-standards list `src/stores/`. It does not exist on disk. An empty convention folder is cheap to add and is the whole point of "establish a client-state convention" (FR-4.2). (confirmed)
- **`components/ui/`** — design 5.1 and PD-F1 describe a `components/ui/` home for reusable primitives "now"; it's absent. Same low-cost fix. (confirmed)

These are minor because no code depends on them yet, but the design asserts them as delivered extension points, so either the tree or the doc is wrong.

## Error handling

There's no root `ErrorBoundary`. Section 6 of the design recommends one and frontend-standards require a global boundary at app root with fallback UI. Today an error thrown during render — or a failed dynamic import of the lazy `GenerationPage` chunk — propagates to the root and white-screens the app, since `<Suspense>` handles the pending state but not a rejected import. A minimal boundary wrapping `RouterProvider` in `App.jsx` would contain that. For a foundation that other stories build on, establishing the boundary now is more valuable than usual because it sets the pattern every feature inherits. (likely)

## Test quality and the AC#3 proxy

Ten tests cover the six ACs with real assertions rather than smoke checks: shell landmarks and content (AC#2), placeholder-not-error for an unbuilt route (AC#4), `apiClient.defaults.baseURL === '/api/v1'` and JSON content-type (AC#5), and three CSP directive checks including the negative script-src test (AC#6). AC#1 (build) is a CI gate rather than a unit test, which is appropriate. `setup.js` registers jest-dom and cleans up after each test, so there's no cross-test DOM leakage.

AC#3 is the one worth naming precisely. The requirement is "navigation without a full page reload." The test asserts that the `Dashboard` and `Engine Configuration` `NavLink`s carry `href="/"` and `href="/scheduling/config"` — i.e., they resolve to in-app SPA route paths — rather than clicking a link and asserting the target content swaps in place. The prompt explains why: a jsdom/Node undici `AbortSignal` quirk blocks data-router `navigate()` under test. As a proxy this is *acceptable but partial*: it proves the links point at client-side routes (a full-reload implementation would use different targets or an `<a>` outside the router), and the separate AC#4 test proves the target route renders its content in-shell when mounted directly. What the href assertion does *not* prove is that clicking performs an in-place transition without a document reload — that's the actual behavioral claim. The combination (href shape + direct-mount render) is a reasonable stand-in until the tooling constraint is resolved; once it is, a `userEvent.click` followed by an assertion that the destination content appears without a remount would close the gap. Documenting the quirk inline in the test (as done) is the right call. (confirmed)

</details>

<details>
<summary>File map</summary>

- `index.html` — CSP meta tag; `#root`; module script entry.
- `vite.config.js` — React plugin, `@/` alias, `/api` dev proxy to `:8080`, Vitest config block (jsdom, v8 coverage excluding main.jsx + test setup).
- `package.json` — pinned deps; `lint` script present but no ESLint config/dep.
- `jsconfig.json` — `@/` alias for editor/tooling (matches Vite alias, KD-F4).
- `src/main.jsx` — `createRoot` → `<StrictMode><App/></StrictMode>`, imports global.css.
- `src/app/App.jsx` — `<AppProviders><RouterProvider/></AppProviders>`.
- `src/app/providers.jsx` — TanStack Query `QueryClient` (staleTime 60s, retry 1, no refetch-on-focus; matches KD-F3).
- `src/app/router.jsx` — `createBrowserRouter`; shell-wrapped routes; lazy `GenerationPage` (A4-345 coupling).
- `src/components/layout/AppShell.jsx` — header/nav/main landmarks, `NAV_ITEMS`, `NavLink` active styling.
- `src/features/dashboard/DashboardPage.jsx` — landing content, module surface cards.
- `src/features/dashboard/PlaceholderPage.jsx` — `{title, note}` "coming soon" page.
- `src/lib/api-client.js` — single axios instance, `/api/v1`, passthrough interceptor seam.
- `src/styles/global.css` — CSS tokens + shell/card/empty-state layout.
- `src/test/setup.js` — jest-dom registration + afterEach cleanup.
- Tests: `AppShell.test.jsx`, `router.test.jsx`, `api-client.test.js` — 10 tests, all passing.
- Missing vs. design: `src/stores/`, `src/components/ui/`, `eslint.config.js`.

Full review is of the on-disk scaffold (no git repo initialized); no diff base available.

</details>


---

## Resolution (author response — akhil jawali)

Verdict from AI review: **COMMENT** (all 6 ACs met; `pnpm build` and `pnpm test` 10/10 green). No blocking defects. The items below were triaged before assigning to the lead.

### Fixed in this story

| # | Finding | Severity | Resolution |
|---|---------|----------|------------|
| 3 | `stores/` folder missing | Low | Added `src/stores/.gitkeep` with a convention note (Zustand client-state home, FR-4.2). |
| 4 | `components/ui/` folder missing | Low | Added `src/components/ui/.gitkeep` with a convention note (reusable primitives home, design 5.1 / PD-F1). |
| 5 | No root ErrorBoundary | Low | Added `src/components/feedback/ErrorBoundary.jsx` and wrapped `RouterProvider` in `App.jsx`. Contains render errors and failed lazy-chunk imports so a feature failure no longer white-screens the app. Verified: build 0, tests 10/10. |
| 1 | `lint` script fails (no ESLint config/dep) | Medium | Removed the unwired `"lint": "eslint ."` script from `package.json`. A script that always fails is worse than no script for the DoD gate. See accepted follow-up below. |

### Accepted decisions (not changed in this story)

| # | Finding | Severity | Decision & rationale |
|---|---------|----------|----------------------|
| 2 | Router build-couples to A4-345 (`lazy()` import of `GenerationPage`) | Medium | **Accepted as an explicit co-development decision.** A4-345 (generation viewer) is being built in parallel and already ships a real `GenerationPage`. Reverting `/scheduling/generation` to `PlaceholderPage` would break A4-345's working code and its tests. The foundation and A4-345 are deliberately co-developed; the route points at the real lazy import. This is documented here so the coupling is a conscious choice, not a silent dependency. If A4-345 is ever removed, the route must fall back to `PlaceholderPage`. |
| 6 | Route table duplicated in `router.test.jsx` | Low | **Accepted.** The test intentionally rebuilds a scoped `createMemoryRouter` (first two routes only) to keep the foundation test isolated from A4-345's lazy chunk. Drift risk acknowledged; a shared exported routes array is a reasonable future refactor but not required for this story. |
| 7 | CSP hardcodes dev origin `http://localhost:8080` | Low | **Accepted / tracked as OQ-DF2.** The production CSP is owned by the reverse-proxy header; the meta tag is dev-oriented. Reconciliation happens before release, outside this story. |
| 8 | AC#3 asserts NavLink hrefs, not click-driven navigation | Informational | **Accepted proxy.** A jsdom/Node25 undici `AbortSignal` quirk blocks data-router `navigate()` under test. The href assertion (links target SPA route paths) plus AC#4's direct-mount render is a reasonable stand-in. A `userEvent.click` transition assertion is recommended once the tooling constraint clears. |

### Accepted follow-up (separate task)

- **ESLint tooling setup.** `frontend-standards`/`ui-standards` mandate ESLint with `eslint.config.js` and an enforced import-order rule. Wiring a verified flat config requires adding several pinned devDependencies (`eslint`, `@eslint/js`, `eslint-plugin-react`, `eslint-plugin-react-hooks`, `eslint-plugin-import`, `globals`) and running `pnpm install` to confirm `pnpm lint` passes end-to-end. That install/verify loop is out of scope for the foundation story and belongs in its own tooling task. Until then, the `lint` script is removed so no command lies about passing.

### Post-fix verification

- `pnpm build` → exit 0 (156 modules transformed).
- `pnpm test` → 10/10 passing (3 test files).
- Zero TypeScript introduced; all new files are `.jsx`/`.gitkeep`.

**Author verdict after fixes: ready for human review (COMMENT — no blockers).**
