# Design Document — Frontend: Scheduling Engine Admin SPA Foundation

**Jira Reference:** A4-335
**Source Requirements:** docs/requirements/A4-335-frontend-spa-foundation-requirements.md (Approved by lead, A4-336)
**Application:** New frontend SPA (`frontend/`)
**Stack:** React 18 (plain JSX, no TypeScript) · Vite · pnpm · React Router v6 · TanStack Query · Zustand · Axios
**Generated:** 2026-09-01

## 1. Overview

This design establishes the React single-page application foundation that all scheduling-engine admin screens build on. It delivers the app bootstrap + build, an app shell (header/sidebar/main), client-side routing with an extensible route table, a server-state provider (TanStack Query) and a client-state convention (Zustand), a single shared Axios API client targeting `/api/v1`, a design-system foundation (global styles + a home for reusable primitives), and security configuration (CSP, no unsafe render sinks). It contains no feature screens — those are A4-340 (config CRUD) and A4-345 (generation viewer), which consume this foundation.

A working scaffold already exists in `frontend/` (built and `pnpm build`-verified during story creation). This design formalizes and hardens that scaffold and defines the extension points feature stories rely on.

**Not in scope:** Feature screens (A4-340, A4-345, A4-15), authentication/JWT + RBAC (security module), any backend change, final component-library selection (see PD-F1).

## 2. Architecture

```
frontend/
├── index.html                 # CSP meta; #root; module entry
├── vite.config.js             # React plugin, @/ alias, /api dev proxy -> :8080
├── jsconfig.json              # @/ alias for editor/tooling
├── package.json               # pnpm deps (pinned)
└── src/
    ├── main.jsx               # ReactDOM.createRoot -> <App/>
    ├── app/
    │   ├── App.jsx            # <AppProviders><RouterProvider/></AppProviders>
    │   ├── providers.jsx      # QueryClientProvider (TanStack Query) + defaults
    │   └── router.jsx         # createBrowserRouter route table (extensible)
    ├── components/
    │   ├── layout/AppShell.jsx  # header + sidebar nav + main
    │   └── ui/                   # reusable primitives (table primitive lands with A4-340)
    ├── features/
    │   └── dashboard/            # DashboardPage, PlaceholderPage
    ├── lib/
    │   └── api-client.js         # single axios instance, /api/v1, interceptor seam
    ├── stores/                   # Zustand stores (client/UI state) — convention only here
    └── styles/global.css         # design-system foundation (tokens + layout)
```

Data flow: `main.jsx` → `App` → `AppProviders` (QueryClient) → `RouterProvider` → `AppShell` wraps each route's page. Feature pages call `apiClient` (`/api/v1`), which the Vite dev proxy forwards to the backend on `:8080`.

## 3. Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-F1 | Single Axios instance in `lib/api-client.js` with `baseURL: '/api/v1'`, used by all features. Request/response interceptors are present as no-op seams for future JWT/refresh. | One origin, one place to add auth + error normalization later. | FR-5; auth wired by the security module (PD-F2). |
| KD-F2 | Routing via `createBrowserRouter` with a central route table; every route renders inside `AppShell`. Unbuilt features get a `PlaceholderPage`. | Extensible without restructuring; feature stories append routes. | FR-3; A4-340/A4-345 add routes here. |
| KD-F3 | TanStack Query `QueryClient` provided app-wide in `providers.jsx` with conservative defaults (staleTime 60s, retry 1, no refetch-on-focus); feature hooks override per query. | Central server-state policy; features tune freshness (state-management steering). | FR-4; conflicts data will override to staleTime 0 later. |
| KD-F4 | Path alias `@/` -> `src/` configured in BOTH `vite.config.js` (bundler resolve.alias) and `jsconfig.json` (editor). | The bundler alias is required for the build; jsconfig alone is editor-only (the scaffold build failed until the Vite alias was added). | Maintainability NFR. |
| KD-F5 | CSP delivered via `<meta http-equiv="Content-Security-Policy">` in `index.html` for the app; `script-src 'self'` (no inline scripts), `connect-src 'self' http://localhost:8080`, `style-src 'self' 'unsafe-inline'`. Production sets the header at the reverse proxy. | Meets FR-7 in the SPA; `unsafe-inline` limited to styles, never scripts. | FR-7; dev HMR tolerated, prod header authoritative. |

## 4. Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-F1 | Keep the lightweight plain-CSS design foundation (CSS variables + global.css + a `components/ui/` home) for now; adopt Tailwind + Shadcn/ui + Lucide when the first feature screen (A4-340) needs rich primitives. | Req OQ#1 | Plain CSS now, component lib at first feature | A4-335 has no feature screens; adding Tailwind/Shadcn now is config with nothing to render. Deferring keeps the foundation minimal and lets A4-340's design choose deliberately. Pending frontend-lead ratification. |
| PD-F2 | No real authentication in this story. `api-client.js` includes commented interceptor seams + a documented extension point; the security module wires JWT/refresh later. | Req OQ#2 | Seam only | Auth is owned by the security module; a no-op seam avoids rework without pretending to implement auth. |
| PD-F3 | Landing page is a simple dashboard listing the module surfaces (engine configuration, timetable generation) with placeholder routes for unbuilt features. | Req OQ#3 | Shell + simple dashboard + placeholders | Confirms the app renders end-to-end (AC#2) without over-building a dashboard before feature data exists. |

## 5. Component Design

### 5.1 Entry + Providers
- `main.jsx`: `ReactDOM.createRoot(#root).render(<React.StrictMode><App/></React.StrictMode>)`, imports `styles/global.css`.
- `App.jsx`: wraps `RouterProvider` in `AppProviders`.
- `providers.jsx`: instantiates `QueryClient` (KD-F3 defaults) via `useState` initializer; provides `QueryClientProvider`. PropTypes on `children`.

### 5.2 Routing (`router.jsx`)
`createBrowserRouter` with routes: `/` (DashboardPage), `/scheduling/config` and `/scheduling/generation` (PlaceholderPage until A4-340/A4-345). Each element is wrapped in `<AppShell>`. Feature stories add entries here (KD-F2).

### 5.3 App Shell (`components/layout/AppShell.jsx`)
Semantic layout: `<header>` (logo + title), `<nav aria-label="Primary">` sidebar with `NavLink`s (active styling), `<main>` for page content. Keyboard-navigable; accepts `children`. NAV_ITEMS is a small array so feature stories extend it.

### 5.4 Pages (`features/dashboard/`)
- `DashboardPage.jsx`: landing content (PD-F3) — cards for the two module surfaces.
- `PlaceholderPage.jsx`: `{title, note}` props; "coming soon" empty state for unbuilt routes (AC#4).

### 5.5 API Client (`lib/api-client.js`)
`axios.create({ baseURL: '/api/v1', headers: { 'Content-Type': 'application/json' }, timeout: 30000 })`. Response interceptor passthrough now; documented seam for JWT attach + 401-refresh (PD-F2).

### 5.6 State
- Server state: TanStack Query (KD-F3).
- Client state: Zustand convention documented; `stores/` folder present; no feature stores in this story (FR-4.2). Naming: `use<Domain>Store`.

## 6. Cross-Cutting Concerns

| Concern | Design |
|---|---|
| Security | CSP meta (KD-F5); no `dangerouslySetInnerHTML` in the foundation (FR-7.3); DOMPurify to be introduced when rich text appears (feature stories). |
| Build | Vite production build → `dist/`; `pnpm build` must exit zero (AC#1). |
| Accessibility | Semantic `<header>/<nav>/<main>`, `aria-label` on nav, visible focus; full WCAG AA per feature later. |
| Error handling | A root `ErrorBoundary` is recommended; TanStack Query `onError` conventions documented for feature hooks. |
| Config | Dev proxy `/api -> http://localhost:8080` in `vite.config.js`; production serves the SPA behind a reverse proxy that also routes `/api` and sets the CSP header. |

## 7. Testing Strategy

| Scenario | Type | Approach |
|---|---|---|
| Production build succeeds (AC#1) | Build check | `pnpm build` exits 0 and emits `dist/` (CI gate). |
| App renders shell + dashboard (AC#2) | Component test | Render `<App/>` (or `AppShell + DashboardPage`) and assert header/nav/main + dashboard present. |
| Navigation without reload (AC#3) | Component test | Render router at `/`, click a NavLink, assert target route content renders. |
| Placeholder for unbuilt route (AC#4) | Component test | Navigate to a placeholder route; assert PlaceholderPage renders (no error). |
| API client base path (AC#5) | Unit test | Assert `apiClient.defaults.baseURL === '/api/v1'`. |
| CSP present (AC#6) | Static/DOM check | Assert the CSP meta tag exists in `index.html` with the expected directives. |

Note: frontend test tooling (Vitest + React Testing Library) is not yet in the project; introducing it is part of this story's development (or an explicit sub-task). If deferred, AC verification is via build + manual smoke until the test harness lands — call out in dev.

## 8. Provisional Decisions Summary

PD-F1 (design-system library), PD-F2 (auth seam only), PD-F3 (dashboard landing). All pending frontend-lead ratification; each resolves a requirement Open Question.

## 9. Open Questions (Design-Level)

| # | Question | Owner | Status |
|---|---|---|---|
| OQ-DF1 | Add the frontend test harness (Vitest + RTL) in this story, or as a dedicated setup sub-task before feature stories need it? | Frontend Lead | Provisional: add here so the ACs are automatable. |
| OQ-DF2 | Confirm the production CSP is owned by the reverse proxy (header) vs the meta tag, so the two don't diverge. | System Design / Infra | Provisional: proxy header authoritative in prod; meta for dev/SPA. |

## 10. Consistency Notes

- First frontend design in the project; Key Decisions namespaced KD-F* and Provisional Decisions PD-F* to avoid collision with backend KD/PD numbering (backend ended at KD-63 / PD-87).
- Frontend-only: no migration, no enum, no DB. Consumes the backend `/api/v1` read contracts (A4-11/A4-12); no backend change.
- Design matches the on-disk scaffold (component names, folder layout, Vite/jsconfig alias, CSP directives) verified during story creation.

## 11. Traceability

| Requirement | Design Element |
|---|---|
| FR-1 (bootstrap/build) | main.jsx, vite.config.js, package.json; Section 7 build check (AC#1) |
| FR-2 (app shell + landing) | AppShell (5.3), DashboardPage (5.4), PD-F3 |
| FR-3 (routing) | router.jsx (KD-F2), PlaceholderPage (AC#4) |
| FR-4 (state) | providers.jsx / QueryClient (KD-F3), Zustand convention (5.6) |
| FR-5 (API client) | api-client.js (KD-F1), dev proxy (Section 6) |
| FR-6 (design system) | global.css + components/ui/ home (PD-F1) |
| FR-7 (security/CSP) | CSP meta (KD-F5), no unsafe sinks (Section 6) |
| NFR maintainability | @/ alias in Vite + jsconfig (KD-F4), feature-driven folders |
| NFR language | plain JSX throughout (no TypeScript) |
| Req OQ#1/#2/#3 | PD-F1 / PD-F2 / PD-F3 |
