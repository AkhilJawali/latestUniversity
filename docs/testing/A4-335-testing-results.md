# Testing Results — Frontend Scheduling Engine Admin SPA Foundation

- **Story**: A4-335 — Frontend Scheduling Engine Admin SPA Foundation (React Setup)
- **Testing Subtask**: A4-339 — Testing — Frontend Scheduling Engine Admin SPA Foundation
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `frontend/` (app shell, routing, API client, CSP, design-system foundation)
- **Runner**: Vitest 2.1.4 + React Testing Library (jsdom); Vite 5.4.10 build; Node 24, pnpm

---

## 1. Scope

End-to-end validation of the five acceptance criteria for the SPA foundation:
the React app builds to a dist bundle, a Dashboard renders inside the app shell,
client-side navigation works without full page reload, all data access routes
through a single API client at `/api/v1`, and a Content-Security-Policy restricts
sources.

---

## 2. Environment Notes

- Two verifications were run: `pnpm build` (production Vite build for AC1) and
  `pnpm test` (Vitest suite for AC2–AC5). No backend or Docker needed.

---

## 3. Acceptance Criteria → Test Mapping

| AC | Criterion | Verification | Result |
|----|-----------|--------------|--------|
| AC1 | `pnpm install` + `pnpm build` → builds with zero errors, produces dist bundle | `pnpm build` — 173 modules transformed, dist/ assets emitted (index + code-split GenerationPage/SchedulingConfigPage chunks), built in 4.26s, exit code 0 | PASS |
| AC2 | Dashboard landing page renders inside app shell (header + sidebar nav) | `AppShell.test.jsx` (header "UTMS", Primary nav, main content); `router.test.jsx` (dashboard heading + Primary nav at `/`) | PASS |
| AC3 | Nav link → React Router navigates without a full page reload | `router.test.jsx` — NavLinks render as in-app anchors with SPA hrefs (`/`, `/scheduling/config`); target route renders in-shell | PASS |
| AC4 | API call routes through a single configured API client at `/api/v1` | `api-client.test.js` — `apiClient.defaults.baseURL === '/api/v1'`; JSON content-type default | PASS |
| AC5 | CSP restricts sources (no inline scripts; backend origin allowlisted) | `api-client.test.js` CSP block — `index.html` declares CSP meta, `script-src 'self'` without `'unsafe-inline'`, `connect-src 'self' http://localhost:8080` | PASS |

All five acceptance criteria are covered and passing.

---

## 4. Test Execution Summary

Commands: `pnpm build` then `pnpm test`

- **Build:** BUILD SUCCESS — 173 modules transformed, dist bundle produced, exit code 0.
- **Tests:** 45 passed / 0 failed across 10 files (whole frontend suite).

A4-335 foundation-specific test files:

| Test File | Tests | ACs |
|-----------|-------|-----|
| components/layout/AppShell.test.jsx | 2 | AC2 |
| app/router.test.jsx | 3 | AC2, AC3 |
| lib/api-client.test.js | 5 | AC4, AC5 |
| lib/api-error.test.js | 3 | supporting (error handling) |

The remaining passing tests belong to A4-345 / A4-340 features and confirm no
regression in the foundation.

---

## 5. Issues Found and Fixed

None. The foundation was already well covered by unit tests (AppShell, routing,
API client, CSP) and the production build is clean. No defects surfaced during
end-to-end testing.

---

## 6. Gaps / Follow-up

- **CSP is validated by asserting the `index.html` meta tag content**, not by a
  live browser runtime check. A browser-based E2E (e.g., Playwright) could later
  confirm the CSP is actually enforced at runtime. Non-blocking for this story.
- **AC1 is verified by a real production build**; there is no automated CI gate
  asserting the build in this run — recommend wiring `pnpm build` into CI.

---

## 7. Verdict

All five acceptance criteria for A4-335 pass. The production build is clean and
the foundation test suite (AppShell, routing, API client, CSP) is green, with no
regressions in the broader frontend suite (45/45). No issues found. Testing
subtask A4-339 is ready for lead approval.
