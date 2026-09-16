# Requirement Document — Frontend: Scheduling Engine Admin SPA Foundation

## 1. Introduction

This document captures the requirements for the React single-page application (SPA) foundation that all scheduling-engine admin screens are built on. It is an enabling/infrastructure story: it establishes the app shell, routing, server- and client-state management, a single API client, a design-system foundation, and security configuration — but no feature screens. The scheduling-engine feature screens (configuration admin panel, generation & draft viewer) are separate stories that consume this foundation.

This is a frontend-only story. It does not add or change any backend behavior.

## 2. User Story

**A4-335:** As a frontend developer, I want the React SPA scaffolded with routing, server/client state management, an API client, and a design-system foundation, so that all scheduling-engine admin screens have a consistent, secure base to build on.

**Story Points:** 3

**BRD Requirements:** Section 8 (Usability NFR — "Drag-and-drop timetable editor; mobile-responsive student/faculty views; minimal training required for coordinators"); Section 8 (Security & Access Control — RBAC and secure client handling). This story is the enabling foundation for the frontend that consumes the scheduling-engine outputs defined in BRD 6.2 and 6.10; it does not itself implement a BRD functional feature.

## 3. Actors

| Actor | Interaction |
|---|---|
| Frontend developer | Builds, runs, and extends the SPA; adds feature routes/pages on top of the foundation |
| Coordinator / Admin (end user) | Loads the app, sees the shell and landing page, navigates between sections (feature screens arrive in later stories) |
| Backend REST API (A4-11/A4-12 and future config APIs) | Provides data at base path `/api/v1`; consumed via the shared API client |

## 4. User Journeys

### Journey 1: Developer builds and runs the foundation (Happy Path)

**Before:** Node 20+ and pnpm are installed. The backend REST API base path (`/api/v1`) is known.

**During:**
1. Developer installs dependencies and starts the dev server.
2. The app boots and renders the app shell (header + sidebar navigation + main content area) with a landing page.
3. Developer runs the production build.
4. The build completes with zero errors and produces a distributable bundle.

**After:** The foundation is ready for feature stories to add routes, pages, and state on top of it.

### Journey 2: End user navigates the shell

**Before:** The app is deployed/served and loaded in a supported browser.

**During:**
1. User opens the app; the landing page renders inside the shell.
2. User clicks a navigation item.
3. The router navigates to the target route and renders its content without a full page reload.

**After:** The user is on the selected section. Sections whose feature stories are not yet implemented show a clear placeholder.

### Journey 3: A component fetches backend data

**Before:** A feature component needs data from the backend.

**During:**
1. The component issues a request through the shared API client.
2. The client sends the request to the `/api/v1` base path (routed to the backend by the dev proxy / production reverse proxy).
3. The server-state layer caches and manages the response.

**After:** Data is available to the component; subsequent identical requests may be served from cache per the configured freshness policy.

## 5. Functional Requirements

### FR-1: Application Bootstrap and Build

- FR-1.1: The system shall be a React 18 single-page application authored in plain JavaScript/JSX (no TypeScript), package-managed by pnpm and built with a modern bundler.
- FR-1.2: The system shall produce a production build with zero errors that emits a distributable bundle.
- FR-1.3: The system shall run a local development server that serves the app for iterative development.

### FR-2: App Shell and Layout

- FR-2.1: The system shall render an application shell consisting of a header, a sidebar navigation area, and a main content area (desktop-first admin layout).
- FR-2.2: The system shall render a landing/dashboard page inside the shell as the default route [scope of the landing page content — placeholder vs. real dashboard — subject to OQ#3].
- FR-2.3: The shell shall present navigation entries for the scheduling-engine admin sections (e.g., engine configuration, timetable generation), with sections not yet implemented shown as clearly labeled placeholders.

### FR-3: Client-Side Routing

- FR-3.1: The system shall provide client-side routing so navigation between sections does not trigger a full page reload.
- FR-3.2: The route table shall be extensible so feature stories can register new routes without restructuring the foundation.
- FR-3.3: Navigating to a route whose feature is not yet built shall show a placeholder page rather than an error.

### FR-4: State Management Foundation

- FR-4.1: The system shall provide a server-state management layer (for API data: caching, background refresh, request de-duplication) available application-wide via a provider.
- FR-4.2: The system shall establish a client/UI-state management convention (for UI-only state such as sidebar toggles, filters, selection) distinct from server state; no feature-specific stores are required in this story.
- FR-4.3: Server-state defaults (e.g., freshness/stale time, retry) shall be centrally configured and overridable per feature query.

### FR-5: API Client

- FR-5.1: The system shall provide a single, shared API client instance used by all features, targeting the backend base path `/api/v1`.
- FR-5.2: In local development, requests to `/api/v1` shall be routed to the backend server (via a dev proxy) so the browser uses a single origin (no CORS in local dev).
- FR-5.3: The API client shall provide a seam for attaching authentication (JWT) and token-refresh handling when the auth/security module is available [auth is not implemented in this story — see OQ#2].

### FR-6: Design-System Foundation

- FR-6.1: The system shall establish a design-system foundation providing reusable UI primitives — including a data-table primitive suitable for the admin CRUD screens that follow — and global styles [the specific component-library choice is a design decision — see OQ#1].
- FR-6.2: The layout and primitives shall follow the project UI standards (spacing scale, typography, color semantics, responsive behavior) as elaborated in the design phase.

### FR-7: Security Configuration

- FR-7.1: The system shall apply a Content-Security-Policy that restricts sources — no inline scripts, and only the application origin plus the backend origin allowlisted for connections.
- FR-7.2: The system shall follow secure cookie handling conventions (e.g., HttpOnly/Secure/SameSite) for any cookies, consistent with org security standards; no authentication cookies are issued in this story.
- FR-7.3: The system shall not use unsafe rendering sinks (e.g., raw HTML injection) in the foundation; any future rich-text rendering must be sanitized (design-phase concern for feature stories).

## 6. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Build | `pnpm install` then production build completes with zero errors and produces a bundle. |
| Usability | Desktop-first admin shell (coordinators); responsive foundation so student/faculty views can be mobile-responsive later (BRD Section 8). |
| Security | CSP restricts script/connect sources; no inline scripts; backend origin allowlisted (org security standards). |
| Maintainability | Feature-driven folder structure; path alias for `src`; components/state organized per frontend standards so feature stories extend without rework. |
| Language | Plain JavaScript/JSX only — no TypeScript (hard project rule). |
| Accessibility | Semantic landmarks (header/nav/main) and keyboard-navigable nav as the baseline; full WCAG AA is elaborated per feature in later stories. |

## 7. Acceptance Criteria

1. **Given** the frontend repository with Node 20+ and pnpm, **When** dependencies are installed and the production build is run, **Then** the build completes with zero errors and produces a distributable bundle.
2. **Given** the dev server is started, **When** the app loads, **Then** a landing/dashboard page renders inside the app shell (header + sidebar navigation + main content).
3. **Given** the app shell, **When** the user clicks a navigation entry, **Then** the router navigates to the target route without a full page reload.
4. **Given** a route whose feature story is not yet implemented, **When** the user navigates to it, **Then** a clearly labeled placeholder page is shown (not an error).
5. **Given** a component needs backend data, **When** it makes a request, **Then** the request goes through the single shared API client targeting `/api/v1`, routed to the backend in local dev via the proxy.
6. **Given** the app loaded in a browser, **When** the page renders, **Then** a Content-Security-Policy is present that forbids inline scripts and allowlists only the app and backend origins.

## 8. Dependencies

| Dependency | Blocking? |
|---|---|
| Node 20+ / pnpm toolchain | Yes |
| Backend REST API base path `/api/v1` (A4-11/A4-12 live) | Partial — the client targets it; the foundation renders without live data |
| Auth/security module (JWT) | No — this story only provides the interceptor seam (OQ#2) |
| A4-340 (config panel), A4-345 (generation viewer), A4-15 (editor) | These CONSUME this foundation — this story blocks them, not vice versa |

## 9. Assumptions

1. Frontend uses plain JavaScript/JSX only (no TypeScript) per the hard project rule.
2. Server-state management is TanStack Query and client-state is Zustand per tech steering; the exact design-system library is a design decision (OQ#1).
3. No authentication is implemented in this story; the API client provides a seam for it later.
4. The app is desktop-first for coordinators/admins; responsive/mobile elaboration happens in the student/faculty view stories.

## 10. Out of Scope

- Feature screens: engine configuration CRUD (A4-340), generation & draft viewer (A4-345), drag-drop editor (A4-15).
- Authentication / JWT handling and RBAC enforcement (security module).
- Any backend change.
- Final design-system library selection and component catalogue (design phase / OQ#1).

## 11. Open Questions

| # | Question | Impact | Needs Answer From |
|---|---|---|---|
| OQ#1 | Design-system library: adopt Tailwind + Shadcn/ui + Lucide (per UI standards) now, or keep a lightweight plain-CSS foundation and adopt the component library when the first feature screen needs it? | Component/table primitives, styling approach | System Design / Frontend Lead |
| OQ#2 | Should this story wire any real auth seam behavior (e.g., a no-op interceptor placeholder), or defer all auth to the security module and only document the extension point? | API client scope | System Design |
| OQ#3 | Does the landing page render a real dashboard (summary tiles/links), or only a shell with placeholder routes until feature screens exist? | FR-2.2 scope | Frontend Lead / Product |

## 12. Traceability

| Source | FR Mapping |
|---|---|
| BRD Section 8 — Usability (minimal-training admin UI; mobile-responsive foundation) | FR-2, FR-6, NFR Usability |
| BRD Section 8 — Security & Access Control (secure client handling) | FR-7 |
| Tech steering — React 18, plain JSX, pnpm, TanStack Query, Zustand | FR-1, FR-4 |
| API standards — `/api/v1` base path, single client | FR-5 |
| Enabler for BRD 6.2 / 6.10 engine outputs consumed by A4-340 / A4-345 | FR-3 (extensible routing), Section 8 dependencies |
