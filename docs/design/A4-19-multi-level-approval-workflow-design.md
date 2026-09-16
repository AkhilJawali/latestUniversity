# A4-19 — Multi-Level Approval Workflow — Design Document

| Field | Value |
|-------|-------|
| Story | A4-19 — Multi-Level Approval Workflow |
| Design Subtask | A4-85 |
| Requirement Subtask | A4-84 (Approved) |
| Epic | A4 — UTMS |
| Role / Type | **Backend** (Java 17 / Spring Boot 3.x, Spring Data JPA, Flyway, MapStruct) |
| Source requirement | docs/requirements/A4-19-multi-level-approval-workflow-requirements.md |
| Migration | **V14** (V1–V13 exist) |
| PD sequence | **PD-101 … PD-106** (project last used: PD-100 in A4-16) |

---

## 1. Introduction

Backend design for the configurable multi-level approval workflow. New module package `com.utms.approval`, four entities (`WorkflowInstance`, `WorkflowStep`, `ApprovalPipeline`, `ApprovalLevel`), a transactional `ApprovalWorkflowService` state machine, a REST `ApprovalController`, MapStruct DTO mappers, and Flyway **V14**. It drives the existing `DraftStatus` transitions (`DRAFT→UNDER_REVIEW→APPROVED`, and back to `DRAFT` on first-level rejection) and, on final approval, emits a Spring application event that A4-21 consumes to publish. Follows the layered architecture and the A4-8 backend-workflow precedent.

---

## 2. Resolved Open Questions → Provisional Decisions (lead-approved 2026-09-09; logged in docs/open-questions-log.md)

| OQ | Decision | PD |
|----|----------|----|
| OQ-1 (pipeline config API vs seed) | Seed a default pipeline + expose **read** endpoints; no pipeline-CRUD API this story. | **PD-101** |
| OQ-2 (global vs scoped pipeline) | One **global** active pipeline; `scope` column nullable/reserved for later. | **PD-102** |
| OQ-3 (RBAC not built) | Capture `actorUserId` from request context (header `X-User-Id`, fallback to the auditing principal); store advisory `requiredRole` on each level; enforce when Auth lands. | **PD-103** |
| OQ-4 (PUBLISHED ownership) | Final approval sets `DraftStatus=APPROVED` and publishes a `DraftApprovedEvent`; **A4-21** writes PUBLISHED + side-effects. | **PD-104** |
| OQ-5 (optimistic concurrency) | Add `@Version` to `WorkflowInstance`; concurrent stale actions → `OptimisticLockException` → 409. | **PD-105** |
| OQ-6 (withdraw action) | Not in scope; `WITHDRAWN` enum value reserved, no endpoint. | **PD-106** |

---

## 3. Module Layout

```
com.utms.approval/
├── entity/
│   ├── WorkflowInstance.java        # BaseEntity + draftId, pipelineId, currentLevelIndex, state, @Version
│   ├── WorkflowStep.java            # BaseEntity + workflowInstanceId, levelIndex, levelName, action,
│   │                                #   actorUserId, comments, rejectionReason, actedAt  (append-only)
│   ├── ApprovalPipeline.java        # BaseEntity + name, scope(nullable), isActive
│   └── ApprovalLevel.java           # BaseEntity + pipelineId, levelIndex, levelName, requiredRole
├── enums/
│   ├── WorkflowState.java           # IN_REVIEW, APPROVED, REJECTED_RETURNED, WITHDRAWN
│   └── WorkflowAction.java          # SUBMITTED, APPROVED, REJECTED
├── repository/
│   ├── WorkflowInstanceRepository.java
│   ├── WorkflowStepRepository.java
│   ├── ApprovalPipelineRepository.java
│   └── ApprovalLevelRepository.java
├── dto/
│   ├── SubmitDraftRequest.java      # draftId, comments?
│   ├── ApproveRequest.java          # comments?
│   ├── RejectRequest.java           # rejectionReason (required), comments?
│   ├── WorkflowInstanceDto.java     # state, currentLevelIndex, currentLevelName, draftId, draftStatus, steps[]
│   └── WorkflowStepDto.java
├── event/
│   └── DraftApprovedEvent.java      # {draftId, workflowInstanceId, approvedByUserId, approvedAt}
├── mapper/
│   └── ApprovalMapper.java          # @Mapper(config = BaseMapperConfig.class)
├── service/
│   ├── ApprovalWorkflowService.java # submit/approve/reject/get/list (state machine, @Transactional)
│   └── PipelineResolver.java        # resolves the applicable pipeline + ordered levels
└── web/
    └── ApprovalController.java      # /api/v1/approvals/*
```

---

## 4. Data Model & Migration (V14)

`V14__create_approval_workflow_tables.sql` — four tables, all with the full BaseEntity column set (`id BIGSERIAL PK, is_active, created_at, updated_at, created_by, updated_by, deleted_at`) per data-access standards.

- **approval_pipelines**: `name VARCHAR(100) NOT NULL`, `scope VARCHAR(100) NULL`, plus BaseEntity. Seed one active row "Default Institution Pipeline".
- **approval_levels**: `pipeline_id BIGINT NOT NULL FK→approval_pipelines`, `level_index INT NOT NULL`, `level_name VARCHAR(100) NOT NULL`, `required_role VARCHAR(50) NULL`, plus BaseEntity. `uq_approval_levels_pipeline_index (pipeline_id, level_index)`. Seed 3 rows: (0,'Coordinator','COORDINATOR'), (1,'HOD','HOD'), (2,'Dean/Registrar','DEAN_OR_REGISTRAR').
- **workflow_instances**: `draft_id BIGINT NOT NULL FK→timetable_drafts`, `pipeline_id BIGINT NOT NULL FK→approval_pipelines`, `current_level_index INT NOT NULL`, `state VARCHAR(20) NOT NULL`, `version BIGINT NOT NULL DEFAULT 0` (JPA `@Version`), plus BaseEntity. Partial unique index `uq_workflow_instances_active_draft ON (draft_id) WHERE state IN ('IN_REVIEW') AND deleted_at IS NULL` (HC-AW-1). Index `idx_workflow_instances_state_level (state, current_level_index)` (reviewer queue, NFR-5).
- **workflow_steps**: `workflow_instance_id BIGINT NOT NULL FK→workflow_instances`, `level_index INT NOT NULL`, `level_name VARCHAR(100) NOT NULL`, `action VARCHAR(20) NOT NULL`, `actor_user_id VARCHAR(100) NOT NULL`, `comments VARCHAR(2000) NULL`, `rejection_reason VARCHAR(500) NULL`, `acted_at TIMESTAMP NOT NULL`, plus BaseEntity. Index `idx_workflow_steps_instance (workflow_instance_id)`. Append-only enforced at the service layer (no update/delete methods exposed) (HC-AW-3).

FK constraint names: `fk_approval_levels_pipeline`, `fk_workflow_instances_draft`, `fk_workflow_instances_pipeline`, `fk_workflow_steps_instance`. Migration is reversible (documented DROP order in a header comment).

---

## 5. Service Logic — `ApprovalWorkflowService` (`@Transactional`)

State machine. `readOnly=true` on queries; write methods default transactional so the step append + state change commit atomically (HC-AW-5).

- **submit(draftId, comments, actorUserId):**
  1. Load draft (`findByIdAndDeletedAtIsNull`, else 404). Assert `status == DRAFT` else 422 (HC-AW-4). Assert no active instance for the draft else 422 (HC-AW-1).
  2. Resolve pipeline via `PipelineResolver` (PD-101/102: the single active pipeline; its ordered levels). The first *review* level is `levelIndex = 1` when level 0 is the drafting "Coordinator" role (see KD-A19-2); `currentLevelIndex` set accordingly.
  3. Create `WorkflowInstance{state=IN_REVIEW}`; set draft `status=UNDER_REVIEW`; append `WorkflowStep{action=SUBMITTED, level=submitter level, actorUserId}`. Save both (same tx).
- **approve(instanceId, comments, actorUserId):**
  1. Load instance (404 if missing). Assert `state==IN_REVIEW` else 422 (HC-AW-4).
  2. Append `WorkflowStep{action=APPROVED, level=current}`.
  3. If a next level exists → `currentLevelIndex++` (draft stays UNDER_REVIEW). Else → `state=APPROVED`, draft `status=APPROVED`, and publish `DraftApprovedEvent` (PD-104, after commit via `ApplicationEventPublisher`; A4-21 listens).
- **reject(instanceId, rejectionReason, comments, actorUserId):**
  1. Load instance (404). Assert `state==IN_REVIEW` else 422. Assert `rejectionReason` non-blank (also bean-validated → 400) (HC-AW-2).
  2. Append `WorkflowStep{action=REJECTED, rejectionReason, comments, level=current}`.
  3. If current level has a prior *review* level → `currentLevelIndex--` (draft stays UNDER_REVIEW). Else (at first review level) → `state=REJECTED_RETURNED`, draft `status=DRAFT` (returned to submitter) (FR-3.1).
- **get(instanceId) / getByDraft(draftId):** return `WorkflowInstanceDto` with ordered steps (FR-5.1/5.2).
- **list(state, levelIndex, pageable):** paged reviewer queue (FR-5.3).

Optimistic concurrency (PD-105): `WorkflowInstance.@Version`; a concurrent approve/reject on a stale version throws `OptimisticLockException`, mapped to 409 by the global handler (add mapping if absent — see §8).

`actorUserId` (PD-103): resolved by a small `CurrentUserProvider` reading `X-User-Id` header, falling back to the Spring Data auditing principal (`createdBy` source) or `"system"`. Advisory `requiredRole` is stored on the step's level but **not enforced** yet.

---

## 6. API — `ApprovalController` (`/api/v1/approvals`)

Responses follow the established scheduling-controller convention (DTO/list returned directly, consistent with `ConflictController`/`SchedulingController`), not the `{data:[...]}` envelope (cross-doc consistency with the scheduling module).

| Method + path | Body | Response | Notes |
|---------------|------|----------|-------|
| `POST /submit` | `SubmitDraftRequest{draftId, comments?}` | 201 `WorkflowInstanceDto` | FR-1 |
| `POST /{id}/approve` | `ApproveRequest{comments?}` | 200 `WorkflowInstanceDto` | FR-2 |
| `POST /{id}/reject` | `RejectRequest{rejectionReason!, comments?}` | 200 `WorkflowInstanceDto` | FR-3; 400 if reason blank |
| `GET /{id}` | — | 200 `WorkflowInstanceDto` (with steps) | FR-5.1 |
| `GET /draft/{draftId}` | — | 200 `WorkflowInstanceDto` (404 if none) | FR-5.2 |
| `GET ?state=&levelIndex=&page=&size=` | — | 200 `PagedResponse<WorkflowInstanceDto>` | FR-5.3 |
| `GET /pipelines` | — | 200 `[ApprovalPipelineDto]` (levels included) | PD-101 read-only |

`// TODO @PreAuthorize` on the action endpoints (RBAC deferred, PD-103). `@Valid` on request bodies; `actorUserId` injected from `CurrentUserProvider`, never taken from the body.

---

## 7. Key Decisions (continue project KD sequence)

- **KD-A19-1 — Owned approval trail (`workflow_steps`), not the generic `AuditEvent`.** The common `AuditEvent.Action` enum is only {CREATED,UPDATED,DELETED}; it cannot express SUBMITTED/APPROVED/REJECTED + level + reason. The authoritative approval audit is `workflow_steps` (append-only). A generic `AuditEvent(UPDATED)` for the draft status change MAY additionally be published via `AuditEventPublisher`, but it is not the approval trail. (Req §14.)
- **KD-A19-2 — Level 0 = drafting/Coordinator; review starts at level 1.** The seeded pipeline lists Coordinator(0) → HOD(1) → Dean/Registrar(2). Submission is performed by the coordinator (level 0); the first *reviewer* is level 1 (HOD), matching AC-1 ("moves to under review at the HOD level"). Final level = highest `levelIndex`. This resolves the off-by-one in "advance/return a level".
- **KD-A19-3 — Final-approval event is published after commit** (`@TransactionalEventListener(phase=AFTER_COMMIT)` on the A4-21 side) so publication side-effects never run inside this story's transaction and cannot roll back an approved draft (PD-104).
- **KD-A19-4 — Append-only enforced by construction:** no update/delete repository methods for `WorkflowStep`; the service only ever `save`s new rows (HC-AW-3).

---

## 8. Cross-Cutting / Consistency Notes (P6 Step 3)

- **State change + audit atomicity (HC-AW-5):** both happen in the same `@Transactional` service method; the event fires only AFTER_COMMIT (KD-A19-3) — no partial "approved but unpublished-with-error" state, and no "audited but not transitioned".
- **Single-active-instance (HC-AW-1) × partial unique index:** enforced in DB (`WHERE state='IN_REVIEW'`) *and* checked in the service for a friendly 422 before hitting the constraint.
- **Optimistic lock (PD-105) × global handler:** `GlobalExceptionHandler` must map `OptimisticLockException`/`ObjectOptimisticLockingFailureException` → 409. Verify it exists; if not, add it (small addition, flagged as the one change outside the new package).
- **PUBLISHED boundary (PD-104) × DraftStatus:** this story writes only UNDER_REVIEW/APPROVED (+ back to DRAFT on first-level reject). It never writes PUBLISHED — A4-21 does, on the event. No overlap with version-management's SUPERSEDED.
- **Reject-return semantics (KD-A19-2):** "previous level" = previous *review* level; at the first review level, reject returns to the submitter (draft→DRAFT, state→REJECTED_RETURNED). No off-by-one.
- **Response shape consistency:** direct DTO/list like the rest of the scheduling module (not `{data}`), so the future frontend approval story reads it the same way it reads sessions/conflicts.

---

## 9. Testing Strategy (design-level; execution gated separately by lead)

JUnit 5 + Mockito unit tests for `ApprovalWorkflowService`:
- submit: DRAFT→UNDER_REVIEW, instance created at first review level, SUBMITTED step (AC-1); reject submit when not DRAFT / already active (422).
- approve: advance mid-pipeline; final approve → APPROVED + draft APPROVED + `DraftApprovedEvent` published (AC-2).
- reject: mid-level returns one level; first-level returns to submitter (draft→DRAFT) (AC-3); blank reason → validation error, no step (AC-7).
- audit: every action appends an immutable step with who/when/action/level/comments/reason (AC-4); step repository exposes no update/delete.
- pipeline: changing seeded levels changes traversal with no code change (AC-5).
- illegal transition on APPROVED instance → 422, unchanged (AC-8).
- concurrency: stale `@Version` approve → 409 (PD-105).
- Runner: `mvn test`. (Maven availability caveat below.)

---

## 10. Requirement Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 submit | `ApprovalWorkflowService.submit` + `POST /submit`; SUBMITTED step |
| FR-2 approve/advance/final | `.approve` + `POST /{id}/approve`; `DraftApprovedEvent` (PD-104/KD-A19-3) |
| FR-3 reject with reason | `.reject` + `POST /{id}/reject`; return-a-level (KD-A19-2); reason required (HC-AW-2) |
| FR-4 audit trail | `WorkflowStep` append-only (KD-A19-1/KD-A19-4); same-tx (§8) |
| FR-5 status/history/queue | `.get`/`.getByDraft`/`.list` + GET endpoints |
| FR-6 configurable pipeline | `ApprovalPipeline`/`ApprovalLevel` + `PipelineResolver` + seed (PD-101/102) |
| FR-7 state-machine integrity | guards in service (HC-AW-4) + `@Version` (PD-105) |
| HC-AW-1..6 | partial unique index; reason validation; append-only; guards; same-tx; APPROVED-not-PUBLISHED |
| NFR-1..6 | layered pkg; parameterized JPA; immutable audit; V14 + BaseEntity cols; indexes/pagination; JUnit |
| AC-1..AC-8 | §9 tests map each |

---

## 11. Dependencies & Provisional Items

- Existing: `TimetableDraft`/`DraftStatus`/`TimetableDraftRepository`, `BaseEntity`, `GlobalExceptionHandler`, `AuditEventPublisher`, `BaseMapperConfig`, Flyway.
- One change outside the new package: ensure `GlobalExceptionHandler` maps optimistic-lock → 409 (§8).
- A4-21 consumes `DraftApprovedEvent` (downstream).
- Provisional (lead-ratified defaults, revisit if needed): PD-101 (read-only pipeline API), PD-103 (interim userId source), PD-104 (publish boundary).
- **Build caveat:** Maven is not available in this environment; the backend cannot be compiled/tested here — implementation will be inspection-verified and flagged for a real `mvn clean package` + `mvn test` before push.

---

## 12. Out of Scope

- PUBLISHED transition + notifications/feed (A4-21); version diff (A4-20); compliance gating (A4-33); RBAC enforcement; pipeline-CRUD API; withdraw action (PD-106); frontend approval UI.
