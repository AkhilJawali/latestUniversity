# A4-390 — Scheduling Engine Configuration CRUD API — Unit Test Results

| | |
|---|---|
| **Story** | A4-390 |
| **Unit Test subtask** | A4-398 |
| **Framework** | JUnit 5 + Mockito |
| **Command** | `mvn -Dtest='DerivationRuleServiceTest,SoftConstraintWeightServiceTest,CommonSlotServiceTest' test` |
| **Result** | 22 tests, 0 failures, 0 errors, 0 skipped |

---

## 1. Summary

The three configuration CRUD services are covered by isolated JUnit 5 + Mockito unit tests (repositories, mapper, campus/slot repositories, and the audit publisher are mocked). All 22 tests pass.

| Test class | Tests | Result |
|-----------|-------|--------|
| `DerivationRuleServiceTest` | 8 | PASS |
| `SoftConstraintWeightServiceTest` | 7 | PASS |
| `CommonSlotServiceTest` | 7 | PASS |
| **Total** | **22** | **PASS** |

(Surefire reports confirm 0 failures/errors; the wrapper exit code initially reflected only Maven's stderr JVM-agent warnings, not test outcome.)

## 2. Implementation note — where the code lives

The A4-390 CRUD API is implemented in package `com.utms.scheduling.config` (`DerivationRuleService`, `SoftConstraintWeightService`, `CommonSlotService` and their controllers/DTOs/mappers/requests, plus a `validation` sub-package). This implementation predated this story (built alongside A4-380) and already satisfies the A4-390 requirement. A duplicate scaffold created earlier in this story under `com.utms.scheduling.engine.config` was removed to avoid two CRUD APIs over the same tables.

## 3. Correctness fix applied under this story

- **componentType value bug (requirement CR-1).** The derivation-rule request previously validated `componentType` with `@Pattern("^[LTP]$")`, accepting `"L"`, `"T"`, `"P"`. The scheduling engine (`SessionDeriver`) filters derivation rules by the literal strings `"LECTURE"`, `"TUTORIAL"`, `"PRACTICAL"`, so rules created via the API with single-letter values would never be read by the engine.
  - **Fix:** the pattern is now `^(LECTURE|TUTORIAL|PRACTICAL)$` in both `CreateDerivationRuleRequest` and `UpdateDerivationRuleRequest`, and the affected test data was updated to `LECTURE`/`TUTORIAL`. This makes API-created rules visible to the engine.

## 4. Scenarios covered

### DerivationRuleService (8)
- create — valid request returns DTO, saves, publishes audit
- create — missing campus throws EntityNotFound (no save)
- create — duplicate componentType throws Conflict (no save)
- update — changed componentType re-checks uniqueness, updates, audits
- update — same componentType skips the uniqueness check
- update — not found throws EntityNotFound (no save)
- delete — existing soft-deletes (deletedAt set, isActive false), audits
- delete — not found throws EntityNotFound (no save)

### SoftConstraintWeightService (7)
- create — valid request returns DTO, saves, audits
- create — missing campus throws EntityNotFound
- create — duplicate constraintType throws Conflict
- update — changed constraintType re-checks uniqueness, audits
- update — not found throws EntityNotFound
- delete — existing soft-deletes, audits
- delete — not found throws EntityNotFound

### CommonSlotService (7)
- create — valid request returns DTO, saves, audits
- create — missing campus throws EntityNotFound
- create — missing slot definition throws EntityNotFound
- update — changed slot definition re-validates the FK, audits
- update — not found throws EntityNotFound
- delete — existing soft-deletes, audits
- delete — not found throws EntityNotFound

## 5. Requirement coverage

| Requirement | Covered by |
|-------------|-----------|
| FR-3 create + FR-6 uniqueness (CO-1/CO-2) | create happy-path + duplicate-conflict tests (derivation, weight) |
| FR-3 FR integrity (CO-4/CO-5) | missing-campus / missing-slot-definition tests |
| FR-4 update | update happy-path + not-found tests |
| FR-5 soft-delete (CO-3) | delete soft-delete tests (deletedAt + isActive asserted) |
| CO-6 audit-in-transaction | `verify(auditEventPublisher).publish(...)` in every mutating test |
| CR-1 componentType engine contract | pattern fix + LECTURE/TUTORIAL test values |

## 6. Notes / variances (for code review)

- The existing implementation treats `componentType`/`constraintType` as **mutable on update** (re-checking uniqueness), whereas the approved A4-390 design proposed them as immutable identity fields (KD-2). The mutable-with-recheck behavior is functionally safe and is retained. Flagged as an accepted variance for the code-review step.
- `campusId` is immutable on update in the existing code (PD-84), consistent with the design.
- `constraintType` is validated by a dedicated `ConstraintValidator` (`ValidSoftConstraintType`) in the `validation` sub-package against the engine's `SoftConstraintType` enum — a robust equivalent of the design's enum-typed field.
