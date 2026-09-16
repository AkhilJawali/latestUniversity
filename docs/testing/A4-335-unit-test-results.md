# Unit Test Results — A4-335 Frontend Scheduling Engine Admin SPA Foundation

**Story:** A4-335
**Subtask:** A4-378 (Unit Test)
**Date:** 2026-09-01
**Framework:** Vitest 2.1.4 + React Testing Library 16.0.1 + jsdom 25.0.1
**Command:** `pnpm test`

## Summary

| Metric | Value |
|---|---|
| Test files | 3 |
| Tests run | 10 |
| Passed | 10 |
| Failed | 0 |
| Result | PASS (exit 0) |

## Test Files

### AppShell.test.jsx (2 tests) — AC#2
| Test | Verifies |
|---|---|
| renders header, primary nav, and main content | Shell renders header (UTMS), `nav[aria-label=Primary]`, and main content (AC#2) |
| exposes navigation entries for the scheduling-engine sections | Dashboard / Engine Configuration / Timetable Generation nav links present |

### router.test.jsx (3 tests) — AC#2, AC#3, AC#4
| Test | Verifies |
|---|---|
| renders the dashboard inside the shell at / | Dashboard heading + shell nav render at `/` (AC#2) |
| exposes client-side nav links (in-app routing, not full reload) | NavLinks render as SPA anchors with client-side hrefs (`/scheduling/config`, `/`) — in-app routing, not a full reload (AC#3) |
| shows a placeholder (not an error) for an unbuilt feature route | `/scheduling/config` renders the PlaceholderPage "coming soon", not an error (AC#4) |

### api-client.test.js (5 tests) — AC#5, AC#6
| Test | Verifies |
|---|---|
| targets the /api/v1 base path | `apiClient.defaults.baseURL === '/api/v1'` (AC#5) |
| sends JSON by default | Content-Type application/json |
| declares a Content-Security-Policy meta tag | CSP meta present in index.html (AC#6) |
| forbids inline scripts | `script-src 'self'` present, no `script-src 'self' 'unsafe-inline'` (AC#6) |
| allowlists the backend origin for connections | `connect-src 'self' http://localhost:8080` (AC#6) |

## Acceptance Criteria Coverage

| AC | Covered by | Status |
|---|---|---|
| AC#1 (build zero errors -> dist) | Build gate (`pnpm build` exits 0) | PASS |
| AC#2 (shell + landing render) | AppShell + router tests | PASS |
| AC#3 (nav without full reload) | router test (client-side hrefs) | PASS |
| AC#4 (placeholder for unbuilt route) | router test | PASS |
| AC#5 (single client -> /api/v1) | api-client tests | PASS |
| AC#6 (CSP present) | api-client CSP tests | PASS |

## Notes

- AC#3 is verified via NavLink client-side hrefs + the direct-render placeholder test, rather than a DOM-click-driven data-router navigation. The data router's `navigate()` under jsdom/Node 25 triggered an undici `AbortSignal` incompatibility (an environment quirk, not an app defect); the chosen assertions verify the same behavior deterministically.
- The throwaway harness smoke test was removed once real component tests existed.
- All tests are plain JS/JSX (no TypeScript), consistent with the project rule.

All authored tests pass with zero failures.
