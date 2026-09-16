# Testing Results — Campus Hierarchy Master Data Management

- **Story**: A4-2 — Campus Hierarchy Master Data Management
- **Testing Subtask**: A4-219 — Testing — Campus Hierarchy Master Data Management
- **Date**: 2026-09-04
- **Tester**: akhil jawali
- **Module**: `com.utms.masterdata` (campus, department, program, batch, section, hierarchy)
- **Build**: Maven `test` phase — JDK 21 (Amazon Corretto 21.0.8), Spring Boot 3.3.2

---

## 1. Scope

End-to-end validation of the six acceptance criteria for the Campus Hierarchy
Master Data Management story. The hierarchy under test is:

```
Campus → Department → Program → Batch → Section
```

Covered behaviors: full CRUD with persistence, referential-integrity enforcement
on delete, invalid parent-reference rejection, batch/section creation with
strength and parent association, and full-tree traversal.

---

## 2. Environment Notes

- The local machine has JDK 25 on `PATH`, but the project targets Java 21 (LTS).
  Lombok's annotation processor is incompatible with the JDK 25 javac internals
  (`ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`),
  so tests were compiled and run with **JDK 21** (`JAVA_HOME` pinned to
  `Amazon Corretto jdk21.0.8_9`). This matches the workflow's LTS-only rule.
- **Testcontainers-backed integration tests were not executed** because Docker is
  not available in this environment. Behavioral validation was therefore performed
  at the service layer (real service logic, mocked repositories), which exercises
  the same business rules the acceptance criteria assert. See "Gaps / Follow-up".

---

## 3. Acceptance Criteria → Test Mapping

| AC | Criterion | Test(s) | Result |
|----|-----------|---------|--------|
| AC1 | Admin creates a campus (name, code, location) → persisted | `CampusServiceTest.create_validRequest_returnsCampusDto` | PASS |
| AC2 | Department created under a campus with valid campus reference → appears as child | `DepartmentServiceTest.create_validRequest_returnsDepartmentDto`; `HierarchyTreeServiceTest.getCampusTree_exists_returnsTreeForCampus` | PASS |
| AC3 | Delete a department that has active programs → rejected with referential-integrity error | `DepartmentServiceTest.delete_hasPrograms_throwsBusinessRuleViolation`; `CampusServiceTest.delete_hasDepartments_throwsBusinessRuleViolation` | PASS |
| AC4 | Batch/section created with strength, program reference, elective association → persisted and associated | `BatchServiceTest.*`; `SectionServiceTest.create_validRequest_returnsSectionDto` | PASS |
| AC5 | Invalid parent reference (non-existent campus/batch) → validation error, no entity created | `DepartmentServiceTest.create_invalidCampusId_throwsEntityNotFoundException`; `SectionServiceTest.create_invalidBatchId_throwsEntityNotFoundException` | PASS |
| AC6 | Full hierarchy (Campus → Department → Program → Batch → Section) is traversable | `HierarchyTreeServiceTest.getFullTree_populatedHierarchy_returnsFullyTraversableTree` | PASS |

All six acceptance criteria are covered and passing.

---

## 4. Test Execution Summary

Command:

```
mvn -Dtest="CampusServiceTest,DepartmentServiceTest,ProgramServiceTest,BatchServiceTest,SectionServiceTest,HierarchyTreeServiceTest" test
```

| Test Class | Tests | Failures | Errors | Skipped |
|------------|-------|----------|--------|---------|
| CampusServiceTest | 10 | 0 | 0 | 0 |
| DepartmentServiceTest | 5 | 0 | 0 | 0 |
| ProgramServiceTest | 5 | 0 | 0 | 0 |
| BatchServiceTest | 4 | 0 | 0 | 0 |
| SectionServiceTest | 6 | 0 | 0 | 0 |
| HierarchyTreeServiceTest | 5 | 0 | 0 | 0 |
| **Total** | **35** | **0** | **0** | **0** |

**Result: BUILD SUCCESS — all 35 tests passed.**

---

## 5. Issues Found and Fixed

| # | Issue | Severity | Resolution |
|---|-------|----------|------------|
| 1 | No test covered AC6 (full hierarchy tree traversal). `HierarchyTreeService` had zero test coverage — the tree endpoint that satisfies the "traversable" acceptance criterion was untested. | Medium | Added `HierarchyTreeServiceTest` with 5 tests: full-tree traversal across all 5 levels, single-campus tree, empty-tree edge case, and not-found paths for campus and department. All pass. |

No other issues found. No regressions in the existing campus-hierarchy tests.

---

## 6. Gaps / Follow-up

- **Integration tests (Testcontainers + REST Assured)** could not be executed in
  this environment because Docker is unavailable. The acceptance criteria are
  validated at the service (business-logic) layer. When a Docker-enabled CI
  environment is available, the same criteria should also be exercised against a
  real Postgres container to confirm Flyway migrations, JPA mappings, referential
  integrity constraints, and HTTP status codes end-to-end. This is an environment
  limitation, not a code defect.

---

## 7. Verdict

All six acceptance criteria for story A4-2 pass. The one coverage gap found during
testing (AC6 tree traversal) was fixed by adding `HierarchyTreeServiceTest`, and
the full suite is green (35/35). Testing subtask A4-219 is complete.
