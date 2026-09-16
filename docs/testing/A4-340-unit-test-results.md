# A4-340 — Scheduling Config Admin Panel — Unit Test Results

| | |
|---|---|
| **Story** | A4-340 |
| **Unit Test subtask** | A4-401 |
| **Framework** | Vitest 2.1.4 + React Testing Library |
| **Command** | `pnpm test` |
| **Result** | 25 tests, 6 files, 0 failures |

---

## 1. Summary

New tests for the A4-340 config admin panel pass alongside the existing A4-335 suite.

| Test file | Tests | Focus |
|-----------|-------|-------|
| `features/scheduling-config/schemas/config-schemas.test.js` | 8 | Zod validation for all 3 entities (valid + invalid paths) |
| `features/scheduling-config/components/ConfigTable.test.jsx` | 4 | Table loading / error / empty / populated states |
| `lib/api-error.test.js` | 3 | Backend error-envelope mapping (400 fields / 409 message / safe fallback) |
| `app/router.test.jsx` (existing) | 3 | Routing |
| `lib/api-client.test.js` (existing) | 5 | Shared client + CSP |
| `components/layout/AppShell.test.jsx` (existing) | 2 | Shell + nav |
| **Total** | **25** | |

## 2. Scenarios covered (A4-340)

### config-schemas (8) — FR-6 / AC-5
- derivationRule: valid rule coerces strings to numbers; rejects bad componentType (`L`); rejects out-of-range slotDuration (0) and hoursPerSession (99).
- softWeight: valid weight coerces; rejects unknown constraintType (`FOO`); rejects weight > 99.99.
- commonSlot: valid slot coerces slotDefinitionId; rejects empty name + bad dayOfWeek.

### ConfigTable (4) — FR-2 / section 5.2
- error state renders an alert + Retry button.
- empty state shows the "no ... yet" prompt.
- populated rows render cell values + per-row Edit/Delete actions with accessible names.
- loading sets `aria-busy=true`.

### api-error (3) — section 5.6 / FR-7
- 400 `details[]` map to per-field errors.
- non-field errors (409) fall back to the envelope message.
- missing data yields a safe generic message (no internals leaked).

## 3. Requirement coverage

| Requirement | Covered by |
|-------------|-----------|
| FR-6 / AC-5 (client validation blocks invalid input) | config-schemas tests + ConfigFormModal returns before request |
| FR-2 (table states) | ConfigTable tests |
| FR-7 / section 5.6 (safe error mapping) | api-error tests |

## 4. Notes / deviations (for code review)

The implementation reconciles the approved design against what actually shipped:
- Real A4-390 endpoints (`/session-derivation-rules`, `/soft-constraint-weights`, `/institution-common-slots`), not the design's provisional `/scheduling-config/*`.
- `componentType` = LECTURE/TUTORIAL/PRACTICAL (A4-390), not the design's `L/T/P`.
- Plain CSS + native `<dialog>`/tabs (matches A4-335 PD-F1), not Shadcn/Radix.
- Plain `useState` + Zod (matches the generation feature), not react-hook-form (not installed).
- Page-level campus picker instead of a shell header selector (TODO noted in code).
- `slotDefinitionId` is a number input; a lookup dropdown is a follow-up.

Interaction tests for the form modal submit and full CRUD round-trip are deferred to the Testing phase (A4-344) integration tests where a real/mocked backend is available; unit tests here focus on the pure validation, mapping, and table-rendering logic.
