# A4-340 — Scheduling Config Admin Panel — Code Coverage

| | |
|---|---|
| **Story** | A4-340 |
| **Code Coverage subtask** | A4-402 |
| **Tool** | Vitest v8 coverage |
| **Command** | `pnpm coverage` |
| **Result** | 25 tests pass; report at `frontend/coverage/index.html` |

---

## 1. Unit-tested units (the pure logic)

The tests target the deterministic logic — validation, error mapping, and table
rendering — which is where unit tests add the most value.

| Unit | Line | Branch |
|------|------|--------|
| `components/ConfigTable.jsx` | 100% | 85.71% |
| `lib/api-error.js` | 100% | 90.9% |
| `schemas/config-schemas.js` | exercised (all 3 schemas, valid + invalid) | — |

These meet/exceed the 80% target for the logic they cover.

## 2. Not unit-tested (by design — covered by integration in A4-344)

The interactive UI wiring shows low unit coverage because it is exercised
end-to-end rather than in isolation:
- `components/ConfigFormModal.jsx`, `DerivationRuleTab.jsx`, `SoftWeightTab.jsx`, `CommonSlotTab.jsx`
- `api/useDerivationRules.js`, `useSoftWeights.js`, `useCommonSlots.js` (TanStack Query hooks)
- `pages/SchedulingConfigPage.jsx`, `stores/campusStore.js`

These require a rendered app with a QueryClient + a mocked/real backend to test
meaningfully (form submit -> mutation -> table refresh, campus-guard, tab switch).
That is the Testing phase (A4-344) via integration tests, matching how the
generation viewer (A4-345) and the backend slices were handled. The overall
"All files" figure (~29%) reflects this intended split, not a coverage gap in
the tested logic.

## 3. Coverage gaps and plan

| Gap | Plan |
|-----|------|
| Form modal submit / validation-block-on-invalid at the DOM level | A4-344 integration (userEvent submit) |
| CRUD hooks (query/mutation + invalidation) | A4-344 integration against a mocked API |
| Page campus-guard + tab switching | A4-344 integration render |

## 4. Traceability

Unit tests validate FR-6/AC-5 (Zod validation), FR-2 (table states), and
FR-7/section 5.6 (error mapping). See `docs/testing/A4-340-unit-test-results.md`.

## 5. Note

The config panel builds cleanly (`pnpm build` exit 0) as its own lazy chunk.
Implementation reconciles the approved design to the real A4-390 endpoints and
the A4-335 plain-CSS stack (see the unit-test results doc for the full deviation list).
