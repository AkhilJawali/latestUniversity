# A4-380 — Design: Scheduling Engine Configuration CRUD API — Backend

**Jira:** A4-380 (Story) · **Design Subtask:** A4-382 · **Type:** Backend
**Requirement doc:** `docs/requirements/A4-380-backend-scheduling-engine-config-crud-api-requirements.md` (A4-381 — **Approved**)
**Epic:** A4-1 · **Blocks:** A4-340 · **Closes:** A4-11 OQ-D3
**Status:** Draft for Lead review

---

## 1. Overview

Adds REST CRUD for the three existing scheduling-engine config tables (`session_derivation_rules`, `soft_constraint_weights`, `institution_common_slots` — migration V10), which are currently read-only inputs to the engine. The design mirrors the existing `com.utms.masterdata.asset` CRUD pattern: Controller → Service → Repository, MapStruct mapper with `BaseMapperConfig`, request/response DTOs with Jakarta Validation, soft-delete via `BaseEntity.deletedAt`, audit events via `AuditEventPublisher`, and the shared `GlobalExceptionHandler` for the standard error envelope.

No schema change is required for the core scope. The three entities and repositories already exist under `com.utms.scheduling.engine.entity` / `...repository`; this story adds the web/service/DTO layer.

Endpoints match the contract A4-340's design expects (its §3): base `/api/v1/scheduling-config`, paged `{data, meta}` lists, 201/200/204.

---

## 2. Provisional Decisions (resolving requirement Open Questions)

| # | Decision | Resolves | Value | Note |
|---|---|---|---|---|
| PD-82 | Do **not** add a uniqueness constraint to `institution_common_slots` in this story unless ratified. Ship without it (matches current V10). | OQ-1 | No new constraint | Provisional. If ratified, add migration V11 with a partial unique index `(campus_id, day_of_week, slot_definition_id) WHERE deleted_at IS NULL`. |
| PD-83 | Weight validated `@DecimalMin("0.00") @DecimalMax("10.00")`, 2 decimals. | OQ-2 | 0.00–10.00 | Provisional — pending ratification; DB allows up to 99.99. |
| PD-84 | `campusId` is **immutable** on update: the mapper ignores it, and update operates within the record's existing campus. Changing campus = delete + recreate. | OQ-3 | Immutable | Provisional. |
| PD-85 | RBAC not enforced yet; controllers carry `// TODO @PreAuthorize` placeholders exactly like `AssetController`. | OQ-4 | Deferred | Carried forward. |
| PD-86 | `componentType` and `dayOfWeek` validated with an allowlist `@Pattern` in the request DTO: componentType ∈ `{L,T,P}`, dayOfWeek ∈ `{MONDAY..SATURDAY}`. Recommend promoting to enums in a later refactor. | OQ-5 | Allowlist pattern | Provisional. |
| PD-87 | Non-existent referenced `campusId`/`slotDefinitionId` → `EntityNotFoundException` (404), consistent with `AssetService` which throws 404 for missing FK targets. | OQ-6 | 404 | Matches existing pattern. |

---

## 3. API Design

Base: `/api/v1/scheduling-config`. All lists paged (`?campusId=&page=&size=&sort=`), `{data, meta}` envelope. Errors via `GlobalExceptionHandler` (standard envelope).

| Method | Path | Description | Success | Traces to |
|---|---|---|---|---|
| GET | `/derivation-rules?campusId=` | List active derivation rules | 200 | FR-1.1, FR-2 |
| POST | `/derivation-rules` | Create | 201 | FR-2 |
| PUT | `/derivation-rules/{id}` | Update | 200 | FR-2, FR-5 |
| DELETE | `/derivation-rules/{id}` | Soft-delete | 204 | FR-5, C-6 |
| GET | `/soft-constraint-weights?campusId=` | List active weights | 200 | FR-3 |
| POST | `/soft-constraint-weights` | Create | 201 | FR-3 |
| PUT | `/soft-constraint-weights/{id}` | Update | 200 | FR-3, FR-5 |
| DELETE | `/soft-constraint-weights/{id}` | Soft-delete | 204 | FR-5 |
| GET | `/common-slots?campusId=` | List active common slots | 200 | FR-4 |
| POST | `/common-slots` | Create | 201 | FR-4 |
| PUT | `/common-slots/{id}` | Update | 200 | FR-4, FR-5 |
| DELETE | `/common-slots/{id}` | Soft-delete | 204 | FR-5 |

**GET `/{id}`** is also provided per entity (matching AssetController) returning `{data}`.

**Controllers** follow `AssetController` exactly: `@RestController @RequestMapping @RequiredArgsConstructor @Tag`, `@Valid @RequestBody`, `Map.of("data", ...)` / `meta` for lists, `// TODO @PreAuthorize("hasRole('ADMIN')")` placeholders (PD-85).

**409 example** (duplicate):
```json
{"timestamp":"2026-09-01T10:30:00Z","status":409,"error":"Conflict","message":"Derivation rule for component type 'L' already exists for this campus","path":"/api/v1/scheduling-config/derivation-rules"}
```
**400 example** (invalid enum):
```json
{"timestamp":"...","status":400,"error":"Bad Request","message":"Validation failed","path":"...","details":[{"field":"constraintType","message":"must be a valid soft constraint type","rejectedValue":"FOO"}]}
```

---

## 4. Module Structure

New package `com.utms.scheduling.config` (web/service/DTO layer; reuses existing engine entities/repositories):

```
com.utms.scheduling.config/
├── DerivationRuleController.java        SoftConstraintWeightController.java     CommonSlotController.java
├── DerivationRuleService.java           SoftConstraintWeightService.java        CommonSlotService.java
├── DerivationRuleMapper.java            SoftConstraintWeightMapper.java         CommonSlotMapper.java
├── DerivationRuleDto.java               SoftConstraintWeightDto.java            CommonSlotDto.java
├── CreateDerivationRuleRequest.java     CreateSoftConstraintWeightRequest.java  CreateCommonSlotRequest.java
├── UpdateDerivationRuleRequest.java     UpdateSoftConstraintWeightRequest.java  UpdateCommonSlotRequest.java
```

Reused (existing): entities `SessionDerivationRule`, `SoftConstraintWeight`, `InstitutionCommonSlot` and their repositories (`com.utms.scheduling.engine`); `SoftConstraintType` enum (`com.utms.scheduling.engine.enums`); `CampusRepository` (`com.utms.masterdata.campus`); `SlotDefinitionRepository` (`com.utms.masterdata.timeslot`); `AuditEventPublisher`, `BaseMapperConfig`, `GlobalExceptionHandler`, exceptions.

> Repositories need list/uniqueness/lookup finders added (§5.4). They currently expose only `findByCampusIdAndIsActiveTrue` / `existsByCampusIdAndDeletedAtIsNull`.

---

## 5. Service Logic

### 5.1 DerivationRuleService (representative)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DerivationRuleService {
    private final SessionDerivationRuleRepository repository;
    private final CampusRepository campusRepository;
    private final DerivationRuleMapper mapper;
    private final AuditEventPublisher auditEventPublisher;

    @Transactional
    public DerivationRuleDto create(CreateDerivationRuleRequest req) {
        requireCampus(req.getCampusId());                                   // C-4 -> 404 (PD-87)
        if (repository.existsByCampusIdAndComponentTypeAndDeletedAtIsNull(  // C-1
                req.getCampusId(), req.getComponentType())) {
            throw new ConflictException("Derivation rule for component type '"
                + req.getComponentType() + "' already exists for this campus");   // 409
        }
        SessionDerivationRule e = mapper.toEntity(req);
        e.setIsActive(true);
        e = repository.save(e);
        auditEventPublisher.publish(new AuditEvent("SessionDerivationRule", e.getId(),
                AuditEvent.Action.CREATED, null, e, currentUser(), Instant.now())); // C-7, same tx
        return mapper.toDto(e);
    }

    @Transactional(readOnly = true)
    public Page<DerivationRuleDto> findAll(Long campusId, Pageable pageable) {
        Specification<SessionDerivationRule> notDel = notDeleted();
        Specification<SessionDerivationRule> spec = (campusId == null)
                ? notDel : notDel.and(byCampusId(campusId));                 // FR-1.4 active only
        return repository.findAll(spec, pageable).map(mapper::toDto);
    }

    @Transactional
    public DerivationRuleDto update(Long id, UpdateDerivationRuleRequest req) {
        SessionDerivationRule e = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SessionDerivationRule", id)); // FR-5.1 404
        // uniqueness re-check excluding self (C-1) if componentType changes
        if (!e.getComponentType().equals(req.getComponentType())
            && repository.existsByCampusIdAndComponentTypeAndDeletedAtIsNull(e.getCampusId(), req.getComponentType())) {
            throw new ConflictException("Derivation rule for component type '"
                + req.getComponentType() + "' already exists for this campus");
        }
        mapper.updateEntity(req, e);   // campusId ignored by mapper -> immutable (PD-84)
        e = repository.save(e);
        auditEventPublisher.publish(new AuditEvent("SessionDerivationRule", e.getId(),
                AuditEvent.Action.UPDATED, null, e, currentUser(), Instant.now()));
        return mapper.toDto(e);
    }

    @Transactional
    public void delete(Long id) {
        SessionDerivationRule e = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("SessionDerivationRule", id));
        e.setDeletedAt(LocalDateTime.now());
        e.setIsActive(false);                                               // C-6 soft delete
        repository.save(e);
        auditEventPublisher.publish(new AuditEvent("SessionDerivationRule", e.getId(),
                AuditEvent.Action.DELETED, e, null, currentUser(), Instant.now()));
    }
    // requireCampus, notDeleted(), byCampusId() as typed Specifications (data-access Specification rule)
}
```

### 5.2 SoftConstraintWeightService — differences
- Validates `constraintType` against the enum: `EnumUtils`/`SoftConstraintType.valueOf` guarded, or a custom `@ValidSoftConstraintType` DTO annotation (preferred — fails at 400 before the service). Either way an invalid value → 400, never persisted (C-3, protects the engine's `valueOf` read).
- Uniqueness on `(campusId, constraintType)` (C-2), same active-only pattern.
- `weight` range enforced by DTO annotations (PD-83).

### 5.3 CommonSlotService — differences
- Validates `campusId` (C-4) **and** `slotDefinitionId` via `SlotDefinitionRepository.findByIdAndDeletedAtIsNull` (C-5) → 404 if missing (PD-87).
- No uniqueness check in the core scope (PD-82).

### 5.4 Repository additions
- `SessionDerivationRuleRepository`: add `JpaSpecificationExecutor`, `findByIdAndDeletedAtIsNull`, `existsByCampusIdAndComponentTypeAndDeletedAtIsNull`.
- `SoftConstraintWeightRepository`: add `JpaSpecificationExecutor`, `findByIdAndDeletedAtIsNull`, `existsByCampusIdAndConstraintTypeAndDeletedAtIsNull`.
- `InstitutionCommonSlotRepository`: add `JpaSpecificationExecutor`, `findByIdAndDeletedAtIsNull`.
- Existing engine finders (`findByCampusIdAndIsActiveTrue`) remain — the engine read path is untouched.

---

## 6. DTOs & Validation

Each entity has `Create<E>Request`, `Update<E>Request` (Jakarta Validation), and `<E>Dto` (response, `@Builder`, includes id + audit timestamps like `AssetDto`).

- **DerivationRule:** `campusId @NotNull`; `componentType @NotBlank @Size(max=10) @Pattern(L|T|P)` (PD-86); `slotDurationMinutes @NotNull @Positive`; `hoursPerSession @NotNull @DecimalMin("0.0") @Digits(integer=2,fraction=1)`; `description @Size(max=200)`.
- **SoftConstraintWeight:** `campusId @NotNull`; `constraintType @NotBlank` + enum-membership validation (C-3); `weight @NotNull @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer=2,fraction=2)` (PD-83).
- **CommonSlot:** `campusId @NotNull`; `name @NotBlank @Size(max=100)`; `dayOfWeek @NotBlank @Size(max=10) @Pattern(MONDAY..SATURDAY)` (PD-86); `slotDefinitionId @NotNull`; `appliesToAllBatches` (Boolean, default true).

Update requests omit `campusId` (immutable, PD-84) or ignore it in the mapper.

Mappers: `@Mapper(config = BaseMapperConfig.class)`; `toEntity` (no FK associations to ignore since campusId is a plain Long column, not a `@ManyToOne`), `updateEntity(req, @MappingTarget entity)` with `campusId` ignored.

---

## 7. Error Handling

Reuses `GlobalExceptionHandler`: `MethodArgumentNotValidException`→400 (field details), `EntityNotFoundException`→404, `ConflictException`→409, generic→500 (no stack trace). Matches api-standards envelope. No new handler needed.

---

## 8. Persistence / Migration

- Core scope: **no migration** (tables exist in V10; the partial unique indexes `WHERE deleted_at IS NULL` on the two tables mean soft-deleted rows don't block re-creating the same `(campus,type)` — consistent with the active-only duplicate check).
- Conditional: if OQ-1/PD-82 is ratified to add common-slot uniqueness → **V11** additive migration (next version after V10) with a partial unique index. Flagged, not created now.

---

## 9. Testing Strategy

- **Unit (JUnit 5 + Mockito):** each service — create/update/delete/list happy paths; duplicate→409; missing campus/slot→404; invalid enum→400; soft-delete sets `deletedAt`; audit event published. Naming `method_scenario_result`.
- **Integration (Testcontainers + Postgres):** each controller — 201/200/204, paged list active-only, 409 on duplicate, 400 on invalid enum, 404 on missing id; verify row soft-deleted (not physically removed).
- Coverage target ≥ 80% new code (JaCoCo).

---

## 10. Traceability

| Requirement | Design element |
|---|---|
| FR-1 CRUD surface / paged / active-only / validation / audit | §3 endpoints, §5 services, §6 DTOs, §7 errors |
| FR-2 derivation rules | DerivationRule* classes, §5.1, §6 |
| FR-3 weights + enum validation | SoftConstraintWeight* , §5.2, C-3 |
| FR-4 common slots + FK | CommonSlot*, §5.3, C-5 |
| FR-5 update (404, re-validate, campus immutable) | §5.1 update, PD-84 |
| FR-6 error mapping | §7 GlobalExceptionHandler |
| C-1/C-2 uniqueness | existsBy...AndDeletedAtIsNull + §8 partial indexes |
| C-3 enum | §5.2 / §6 validation |
| C-4/C-5 FK existence | requireCampus / SlotDefinitionRepository lookup |
| C-6 soft delete | §5 delete methods |
| C-7 audit same tx | AuditEventPublisher inside @Transactional |
| NFR perf/security/audit/consistency/docs | pagination, JPA params, audit, Asset pattern, Springdoc |
| Req OQ-1..6 | PD-82..87 |
| A4-340 contract (§3) | §3 endpoints match |

---

## 11. Consistency Notes

- Provisional Decisions numbered PD-82..87, continuing the project sequence (A4-340 design ended at PD-81).
- Endpoints, envelope, and DTO field names match A4-340 design §3 so the frontend contract holds.
- Audit column types / soft-delete pattern inherited from `BaseEntity` — identical across the project.
- Migration version: none for core; V11 only if OQ-1 ratified (V10 is the latest applied).
- Every requirement Open Question (OQ-1..6) is dispositioned in §2; none dropped.

---

## 12. Out of Scope

Frontend (A4-340); engine consumption/algorithm changes; RBAC role definitions (Auth); bulk import/export; default-data seeding.

---

## 13. Open Questions (carried to design review)

| # | Question | Owner |
|---|---|---|
| OQ-D1 | Ratify PD-82: ship common slots without a uniqueness constraint, or add V11 `(campus_id, day_of_week, slot_definition_id)` unique? | Product Owner / System Design |
| OQ-D2 | Ratify PD-83 weight range 0.00–10.00. | Product Owner |
| OQ-D3 | Ratify PD-84 (campusId immutable) and PD-86 (componentType/dayOfWeek as pattern allowlists vs enums). | System Design |

---

*Prepared for Lead review (A4-382). No blocking dependency — all inputs (tables, campus, slot definitions, enum, audit) exist. Ready for development on approval.*
