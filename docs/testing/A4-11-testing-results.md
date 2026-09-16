# Testing Results — Timetable Generation Engine

- **Story**: A4-11 — Timetable Generation Engine
- **Testing Subtask**: A4-55 — Testing — Timetable Generation Engine
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `com.utms.scheduling.engine` (solver, service, model)
- **Build**: Maven `test` — JDK 21 (Amazon Corretto 21.0.8), Spring Boot 3.3.2

---

## 1. Scope

Validation of the core auto-generation engine: constraint-based generation of a
first-draft weekly timetable with hard-constraint enforcement, soft-constraint
optimization, feasibility/quality scoring, and unresolved-violation reporting.

---

## 2. Environment Notes

- Local PATH JDK is 25; Lombok's annotation processor is incompatible with JDK 25
  javac internals, so tests compile/run with **JDK 21** (LTS, the project target).
- Engine tests are JUnit 5 + Mockito unit tests over the solver, CSP state, and the
  orchestrating service. Testcontainers-backed integration tests require Docker,
  which is unavailable here (see Gaps).

---

## 3. Acceptance Criteria → Test Mapping

| AC | Criterion | Coverage | Result |
|----|-----------|----------|--------|
| AC1 | Draft generated within 2 min | Orchestration pipeline (`SchedulingEngineService`) exercised via unit tests; the 2-min SLA is enforced by `SolverContext` timeout (`SolverContextTest`) | PASS (unit-level) |
| AC2 | Draft includes feasibility + quality scores | `SoftConstraintOptimizer` quality scoring path; result persistence carries scores | PASS |
| AC3 | Zero hard-constraint violations in output | Hard-constraint validation + CSP propagation (`SolverContextTest`, solver init/propagate) | PASS |
| AC4 | Unresolved soft-constraint violations listed (with the 4 named types) | `InfeasibilityCollectorTest` (9) + soft-violation identification | PASS |
| AC5 | Common slots (CCC/UWE) treated as hard, pre-placed | Pre-placement path (shared with A4-14 fixed-session pre-placement) | PASS |
| AC6 | Day-pattern saturation balancing | Soft-constraint optimizer balancing path | PASS (unit-level) |
| AC7 | Room proximity for back-to-back sessions | Soft-constraint (proximity) path | PASS (unit-level) |
| AC8 | Gap minimization | Soft-constraint (gap) path | PASS (unit-level) |

---

## 4. Test Execution Summary

Command: `mvn -Dtest="com.utms.scheduling.**" test`

- **Whole scheduling suite: 78 tests, 0 failures, 0 errors — BUILD SUCCESS.**
- Engine-specific: `SolverContextTest` (9), `InfeasibilityCollectorTest` (9),
  plus the shared service tests. All green.

---

## 5. Issues Found and Fixed

None specific to A4-11. (A defect was found and fixed in the A4-12 timeout tests
during this same run — see the A4-12 results doc.) No regression in engine tests.

---

## 6. Gaps / Follow-up

- **Performance SLA (AC1, "within 2 minutes")** is enforced structurally by the
  solver timeout, not measured under a realistic ~40-section load in this run. A
  Gatling/load test against a seeded dataset is the appropriate follow-up (the
  testing standards list Gatling for SLA validation).
- **Integration tests (Testcontainers + real Postgres)** were not run (no Docker).
  End-to-end persistence and query behavior should be exercised in a Docker-enabled
  CI environment. Environment limitation, not a code defect.

---

## 7. Verdict

The engine's acceptance criteria are validated at the unit/service level and the
scheduling test suite is green (78/78). The 2-minute performance SLA and DB-backed
integration remain as environment-dependent follow-ups. Testing subtask A4-55 is
ready for lead approval.
