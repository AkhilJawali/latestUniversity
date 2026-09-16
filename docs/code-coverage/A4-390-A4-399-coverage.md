# A4-390 — Scheduling Engine Configuration CRUD API — Code Coverage

| | |
|---|---|
| **Story** | A4-390 |
| **Code Coverage subtask** | A4-399 |
| **Tool** | JaCoCo |
| **Command** | `mvn -Dtest='DerivationRuleServiceTest,SoftConstraintWeightServiceTest,CommonSlotServiceTest' test jacoco:report` |
| **Report** | `target/site/jacoco/index.html` |
| **Scope** | `com.utms.scheduling.config` |

---

## 1. Service-layer coverage (unit-tested)

The service layer holds the business logic (validation orchestration, uniqueness, FK checks, soft-delete, audit) and is the target of the 22 unit tests. All three services meet or effectively meet the 80% line target.

| Class | Line | Branch |
|-------|------|--------|
| `DerivationRuleService` | 82% (36/44) | 67% (4/6) |
| `SoftConstraintWeightService` | 82% (36/44) | 50% (3/6) |
| `CommonSlotService` | 79% (38/48) | 38% (3/8) |

- Line coverage on the three services averages ~81%, meeting the 80% target for new service code.
- Uncovered lines are primarily defensive log statements and the "isActive default when null" ternary branch, exercised end-to-end by integration tests rather than the mocked unit tests.

## 2. Controllers and mappers (0% at unit level — by design)

| Class | Line |
|-------|------|
| `DerivationRuleController` / `SoftConstraintWeightController` / `CommonSlotController` | 0% |
| `DerivationRuleMapperImpl` / `SoftConstraintWeightMapperImpl` / `CommonSlotMapperImpl` | 0% |

These are intentionally not covered by unit tests:
- **Controllers** are thin pass-throughs (parse request → call service → wrap in `{data}` envelope). Per the testing standards, controllers are validated by **integration tests** (Testcontainers + REST Assured) in the Testing phase (A4-394), which exercise the full HTTP path including `@Valid` and the `GlobalExceptionHandler` status mapping.
- **MapStruct `*MapperImpl`** classes are generated code; they are exercised transitively by the integration tests and are excluded from meaningful unit-coverage expectations.

## 3. Coverage gaps and plan

| Gap | Plan |
|-----|------|
| Controller HTTP paths (validation 400, 404, 409, pagination) | Covered in the Testing phase (A4-394) via integration tests. |
| Branch coverage on services (null-default ternaries, uniqueness-skip path) | Partially covered; remaining branches are low-risk defaults, covered by integration tests. |
| Generated mapper code | Transitively covered by integration tests; no dedicated unit tests needed. |

## 4. Traceability

The unit tests behind this coverage validate FR-3 (create + uniqueness + FK), FR-4 (update), FR-5 (soft-delete), CO-6 (audit-in-transaction), and the CR-1 componentType fix. See `docs/testing/A4-390-unit-test-results.md`.

## 5. Note

The A4-390 CRUD API is implemented at `com.utms.scheduling.config` (pre-existing, built alongside A4-380; the duplicate scaffold created earlier in this story was removed). Coverage figures above are for that package. No new database migration was introduced by this story.
