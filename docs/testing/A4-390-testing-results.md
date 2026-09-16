# Testing Results — Scheduling Engine Configuration CRUD API

- **Story**: A4-390 — Scheduling Engine Configuration CRUD API (derivation rules, soft weights, common slots)
- **Testing Subtask**: A4-394 — Testing — Scheduling Engine Configuration CRUD API
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `com.utms.scheduling.config` (derivation rules, soft-constraint weights, common slots)
- **Build**: Maven `test` — JDK 21 (Amazon Corretto 21.0.8), Spring Boot 3.3.2

---

## 1. Scope

Validation of the CRUD REST API for the three scheduling-engine configuration
entities: list (paginated, campus-filtered, excluding soft-deleted), create with
validation, uniqueness enforcement, referential integrity, soft-delete, and
field-level validation errors.

---

## 2. Acceptance Criteria → Test Mapping

| AC | Criterion | Test(s) | Result |
|----|-----------|---------|--------|
| AC1 | List filtered by campus returns only non-soft-deleted rows, paginated | Service list tests across the three entities | PASS |
| AC2 | Valid create → persisted, audit event same transaction, 201 | Create-path tests (`DerivationRuleServiceTest`, `SoftConstraintWeightServiceTest`, `CommonSlotServiceTest`) | PASS |
| AC3 | Duplicate (campus+componentType / campus+constraintType) → 409, no row | Uniqueness/conflict tests per entity | PASS |
| AC4 | Common-slot referencing non-existent campus/slot definition → 400/409, no row | Referential-integrity tests (`CommonSlotServiceTest`) | PASS |
| AC5 | Delete → soft-deleted, excluded from list/get, retained for audit | Soft-delete tests per entity | PASS |
| AC6 | Invalid payload → 400 with field-level errors, no internals leaked | Validation tests per entity | PASS |

---

## 3. Test Execution Summary

Command: `mvn -Dtest="com.utms.scheduling.**" test`

- `DerivationRuleServiceTest` (8), `SoftConstraintWeightServiceTest` (7),
  `CommonSlotServiceTest` (7) — all PASS (22 tests across the three entities).
- Whole scheduling suite: **78 tests, 0 failures, 0 errors — BUILD SUCCESS.**

---

## 4. Issues Found and Fixed

None. CRUD, uniqueness (409), referential integrity, soft-delete, and validation
behave per spec across all three configuration entities. No regression.

---

## 5. Gaps / Follow-up

- **HTTP-level assertions (status codes 201/400/409, error envelope shape)** are
  validated at the service layer here; a REST-Assured + Testcontainers integration
  test would confirm the controller status codes and JSON envelope end-to-end. Not
  run (no Docker) — follow-up in CI.
- **Audit-in-same-transaction (AC2)** is exercised via the service's audit publisher
  interaction; transactional atomicity against a real DB is an integration-test
  follow-up.

---

## 6. Verdict

All six acceptance criteria pass at the service layer; scheduling suite green
(78/78). No issues found. Testing subtask A4-394 is ready for lead approval.
