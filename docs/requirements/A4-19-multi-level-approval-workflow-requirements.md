# A4-19 — Multi-Level Approval Workflow — Requirement Document

| Field | Value |
|-------|-------|
| Story | A4-19 — Multi-Level Approval Workflow |
| Requirement Subtask | A4-84 |
| Epic | A4 — UTMS |
| Role / Type | **Backend** (Java 17 / Spring Boot 3.x, Spring Data JPA, Flyway) |
| BRD Requirements | 6.8 (configurable multi-level workflow; review comments; rejection with reason; full approval audit trail) |
| Story Points | 8 |
| Builds on | A4-11/A4-12 `TimetableDraft` + `DraftStatus` (DRAFT/UNDER_REVIEW/APPROVED/PUBLISHED/SUPERSEDED, exists); common audit infra (`AuditEvent`/`AuditEventPublisher`, exists) |

---

## 1. Introduction

This document specifies the **backend** for a configurable multi-level timetable-draft approval workflow: a coordinator submits a generated draft, it advances through an ordered set of approval levels (default Coordinator → HOD → Dean/Registrar), each reviewer can approve (advance) or reject (return with a reason and comments), and every action is recorded in a full audit trail. The workflow drives the **existing** `DraftStatus` lifecycle on `TimetableDraft` (`DRAFT → UNDER_REVIEW → APPROVED → PUBLISHED`). Actual publication side-effects (notifications, calendar feed) are a **separate** story (A4-21); this story takes a draft to `APPROVED` and exposes the final-approval signal that A4-21 consumes.

This is a backend story: it delivers entities, a workflow service/state-machine, a configurable pipeline, a REST API, and Flyway schema. RBAC enforcement of who may act at each level is **not yet available** (endpoints are `permitAll` with `// TODO @PreAuthorize`), so level authorization is modeled as workflow configuration and flagged as an Open Question (§16, OQ-3).

---

## 2. User Story

*As an* HOD,
*I want* a configurable multi-level approval workflow (Coordinator → HOD → Dean/Registrar) with review comments, rejection with reason, and full audit trail,
*so that* timetable drafts are properly reviewed at each level before publication.

---

## 3. Actors

- **Coordinator** — submits a draft into the workflow.
- **HOD / Dean / Registrar** — reviewers at successive levels; approve or reject.
- **All stakeholders** — read the current status/level and history.

(Role identity is carried as a `userId` string on each action, sourced from the request context; authorization by role is deferred — OQ-3.)

---

## 4. Domain Model & Constraint Inventory (P5 Step 1)

Entities this story owns (new):

- **WorkflowInstance** — one per draft submission; tracks the pipeline, current level index, and overall state.
  - `id`, `draftId` (FK → `timetable_drafts`), `pipelineId` (which configuration was used), `currentLevelIndex`, `state` (`IN_REVIEW` / `APPROVED` / `REJECTED_RETURNED` / `WITHDRAWN`), `createdAt/By`, `updatedAt/By`, `deletedAt` (BaseEntity).
- **WorkflowStep** — the audit trail: one row per action taken.
  - `id`, `workflowInstanceId` (FK), `levelIndex`, `levelName`, `action` (`SUBMITTED` / `APPROVED` / `REJECTED`), `actorUserId`, `comments` (nullable ≤ 2000), `rejectionReason` (nullable ≤ 500, required iff action = REJECTED), `actedAt`, BaseEntity columns.
- **ApprovalPipeline** — the configurable level definition (AC-5).
  - `id`, `name`, `scope` (nullable department/campus association — see OQ-2), `isActive`.
- **ApprovalLevel** — an ordered level within a pipeline.
  - `id`, `pipelineId` (FK), `levelIndex` (0-based order), `levelName` (e.g. "Coordinator", "HOD", "Dean/Registrar"), `requiredRole` (string, advisory until RBAC — OQ-3).

Referenced (owned elsewhere):
- **TimetableDraft / DraftStatus** (A4-11) — the workflow transitions `status` (DRAFT→UNDER_REVIEW→APPROVED); PUBLISHED is set by A4-21. Draft carries `version` (optimistic-concurrency anchor).
- **Audit infra** (`AuditEvent`/`AuditEventPublisher`) — the generic app audit stream. Note its `Action` enum is only {CREATED, UPDATED, DELETED}; the *approval* audit trail (who/level/action/comments/reason) is richer and is owned here as `WorkflowStep` (§14 consistency note). Generic `AuditEvent`s may additionally be emitted for the draft status change.

CRUD coverage:
- WorkflowInstance: **created** on submit; **read** (status/history); **updated** (level advance / return); no hard delete (soft-delete via BaseEntity; withdrawn instances retained for audit).
- WorkflowStep: **append-only** (created on each action; never updated/deleted — audit immutability).
- ApprovalPipeline/Level: full CRUD is **out of scope for this story's API** except the ability to define at least one pipeline via seed/config (OQ-1); read is needed to run the workflow.

---

## 5. Functional Requirements

### FR-1 — Submit a draft into the workflow
- **FR-1.1** `POST /api/v1/approvals/submit` with `{draftId, comments?}` shall create a `WorkflowInstance` for the draft using the applicable pipeline, set `currentLevelIndex` to the first review level, transition the draft `DraftStatus` to `UNDER_REVIEW`, and append a `WorkflowStep` (action=SUBMITTED) (AC-1).
- **FR-1.2** Submitting a draft that is not in `DRAFT` status, or that already has an active workflow instance, shall be rejected with a business-rule error (422) — no duplicate active workflow per draft.
- **FR-1.3** Submission shall capture the submitter `userId` from the request context.

### FR-2 — Approve at a level
- **FR-2.1** `POST /api/v1/approvals/{workflowInstanceId}/approve` with `{comments?}` shall append a `WorkflowStep` (action=APPROVED) at the current level, then: if a next level exists, advance `currentLevelIndex` to it (draft stays `UNDER_REVIEW`); if it is the final level, set the workflow `state=APPROVED` and transition the draft `DraftStatus` to `APPROVED` (AC-2).
- **FR-2.2** Final approval shall expose an application event / signal that the publication story (A4-21) consumes to publish. This story does **not** perform publication side-effects (notifications, feed) — see §15 / OQ-4.

### FR-3 — Reject at a level (return with reason)
- **FR-3.1** `POST /api/v1/approvals/{workflowInstanceId}/reject` with `{rejectionReason (required), comments?}` shall append a `WorkflowStep` (action=REJECTED) and return the draft to the previous level (AC-3): if a previous review level exists, move `currentLevelIndex` back to it; if rejected at the first review level, return the draft to the submitter (workflow `state=REJECTED_RETURNED`, draft `DraftStatus` back to `DRAFT`).
- **FR-3.2** `rejectionReason` is mandatory for a rejection; a rejection without a reason is rejected with a validation error (400).
- **FR-3.3** The rejection reason and comments shall be readable by the submitter and subsequent viewers (part of the history, FR-5).

### FR-4 — Full audit trail
- **FR-4.1** Every submit/approve/reject action shall append an immutable `WorkflowStep` capturing: who (`actorUserId`), when (`actedAt`), action, level (`levelIndex` + `levelName`), comments, and rejectionReason where applicable (AC-4).
- **FR-4.2** `WorkflowStep` rows shall never be updated or deleted (append-only audit). The write shall occur within the same transaction as the state change (atomicity).

### FR-5 — Status & history visibility
- **FR-5.1** `GET /api/v1/approvals/{workflowInstanceId}` shall return the current state, current level (index + name), the draft id/status, and the ordered list of `WorkflowStep`s (AC-6).
- **FR-5.2** `GET /api/v1/approvals/draft/{draftId}` shall return the workflow instance (if any) for a draft, so any stakeholder can see its current status and level (AC-6).
- **FR-5.3** `GET /api/v1/approvals?state=IN_REVIEW&levelIndex=1` (filterable) shall list workflow instances pending at a given level, for reviewer queues (paginated per API standards).

### FR-6 — Configurable pipeline
- **FR-6.1** The number and order of approval levels shall be data-driven via `ApprovalPipeline` + `ApprovalLevel` rows; changing them changes the workflow with no code change (AC-5).
- **FR-6.2** A default pipeline (Coordinator → HOD → Dean/Registrar) shall be seeded (migration/seed data) so the workflow is operable out of the box.
- **FR-6.3** The workflow shall resolve which pipeline applies to a draft (default: the single active pipeline; scoped pipelines per department/campus is OQ-2).

### FR-7 — State-machine integrity
- **FR-7.1** Only legal transitions are permitted: submit only from `DRAFT`; approve/reject only while `IN_REVIEW`; no action on an `APPROVED`/`WITHDRAWN` instance. Illegal transitions return 422.
- **FR-7.2** Concurrent actions on the same instance shall not corrupt state — use optimistic locking (`@Version`) on the workflow instance (and/or the draft `version`), rejecting stale writes with 409 (OQ-5).

---

## 6. Constraints Owned by This Document

- **HC-AW-1 (Single active workflow per draft):** a draft has at most one non-terminal `WorkflowInstance`.
- **HC-AW-2 (Rejection needs a reason):** action=REJECTED requires a non-blank `rejectionReason` (≤ 500).
- **HC-AW-3 (Audit immutability):** `WorkflowStep` is append-only; no update/delete.
- **HC-AW-4 (Legal transitions only):** per FR-7.1.
- **HC-AW-5 (Same-transaction audit):** the `WorkflowStep` and the draft/instance state change commit atomically.
- **HC-AW-6 (Final approval ⇒ APPROVED, not PUBLISHED):** this story stops at `APPROVED`; publication is A4-21.

---

## 7. Constraints Referenced from Other Documents

- **`DraftStatus` lifecycle (A4-11):** DRAFT → UNDER_REVIEW → APPROVED → PUBLISHED → SUPERSEDED. This story writes UNDER_REVIEW and APPROVED (and back to DRAFT on first-level rejection). PUBLISHED is owned by A4-21; SUPERSEDED by version management.
- **Draft `version` + `supersedePreviousDrafts` (A4-11 repo):** available for concurrency/versioning.
- **Audit infra (`AuditEvent`/`AuditEventPublisher`):** generic app audit; the approval-specific trail is `WorkflowStep` (§14).
- **RBAC:** not yet enforced (permitAll); level `requiredRole` is advisory until the Auth module (OQ-3).

---

## 8. Validation Rules

| Field | Rule |
|-------|------|
| submit.draftId | required; must reference an existing draft in DRAFT status |
| approve/reject.workflowInstanceId | required; must reference a non-terminal instance |
| reject.rejectionReason | required, non-blank, ≤ 500 chars |
| comments (submit/approve/reject) | optional, ≤ 2000 chars |
| list filters (state, levelIndex, page, size) | optional; page/size bounded (default size 20, max 100) |

---

## 9. Non-Functional Requirements

- **NFR-1 (Layered architecture):** Controller → Service (`@Transactional`) → Repository; entities never leave the service layer (map to DTOs). No controller-to-repository calls.
- **NFR-2 (Security/standards):** parameterized JPA only; no secrets; never expose stack traces (global handler maps EntityNotFound→404, business-rule→422, validation→400, optimistic-lock→409).
- **NFR-3 (Auditability):** approval trail is immutable and complete (FR-4).
- **NFR-4 (Migrations):** Flyway, next version **V14** (V1–V13 exist); reversible; all BaseEntity columns on every new table (id, is_active, created_at, updated_at, created_by, updated_by, deleted_at).
- **NFR-5 (Performance):** status/history reads < 500ms; reviewer-queue list paginated; index FKs and `(state, current_level_index)`.
- **NFR-6 (Testing):** JUnit 5 + Mockito unit tests for the state machine (legal/illegal transitions, reject-needs-reason, final-approval signal), audit append, and configurable pipeline resolution.

---

## 10. Acceptance Criteria (Given / When / Then)

1. **Submit → under review (AC-1):** *Given* a draft in DRAFT, *When* the coordinator submits it, *Then* a workflow instance is created at the first review level, the draft becomes UNDER_REVIEW, and a SUBMITTED audit step exists.
2. **Approve → advance/final (AC-2):** *Given* an instance at a non-final level, *When* approved, *Then* it advances to the next level; *Given* the final level, *When* approved, *Then* the workflow is APPROVED and the draft becomes APPROVED (publication signal emitted for A4-21).
3. **Reject with reason (AC-3):** *Given* a reviewer rejecting, *When* a rejectionReason + comments are provided, *Then* the draft returns to the previous level (or to the submitter at level 0) with the feedback recorded and visible.
4. **Audit trail (AC-4):** *Given* any submit/approve/reject, *When* it occurs, *Then* an immutable WorkflowStep captures who/when/action/level/comments(/reason), written in the same transaction.
5. **Configurable pipeline (AC-5):** *Given* the pipeline levels are changed in data, *When* a new draft is submitted, *Then* the workflow follows the new levels with no code change.
6. **Status visibility (AC-6):** *Given* a draft in approval, *When* any stakeholder queries it, *Then* the current state, level, and full history are returned.
7. **Reject needs reason (error path):** *Given* a rejection with no reason, *When* submitted, *Then* a 400 validation error is returned and no step is recorded.
8. **Illegal transition (edge):** *Given* an APPROVED instance, *When* an approve/reject is attempted, *Then* a 422 is returned and state is unchanged.

---

## 11. Data Model (conceptual)

- **workflow_instances** (BaseEntity + `draft_id`, `pipeline_id`, `current_level_index`, `state`, `@Version`).
- **workflow_steps** (BaseEntity + `workflow_instance_id`, `level_index`, `level_name`, `action`, `actor_user_id`, `comments`, `rejection_reason`, `acted_at`) — append-only.
- **approval_pipelines** (BaseEntity + `name`, `scope?`, `is_active`).
- **approval_levels** (BaseEntity + `pipeline_id`, `level_index`, `level_name`, `required_role`).

Enums (`@Enumerated(STRING)`): `WorkflowState {IN_REVIEW, APPROVED, REJECTED_RETURNED, WITHDRAWN}`, `WorkflowAction {SUBMITTED, APPROVED, REJECTED}`.

---

## 12. Dependencies

- `TimetableDraft` + `DraftStatus` + `TimetableDraftRepository` (A4-11, exist).
- Common: `BaseEntity`, `GlobalExceptionHandler`, `AuditEventPublisher`, `BaseMapperConfig` (MapStruct), Flyway.
- A4-21 (publication) **consumes** this story's final-approval signal (downstream; not a dependency of A4-19).

---

## 13. Assumptions

1. A user identity (`userId` string) is available from the request context even before full auth (e.g. a header/principal placeholder), to stamp actions. If not, OQ-3 covers the interim.
2. One active pipeline exists (seeded); per-scope pipeline selection is OQ-2.
3. Final approval sets `APPROVED`; a separate A4-21 listener performs publication (OQ-4).
4. The draft `version` and/or an instance `@Version` provide optimistic concurrency (OQ-5).

---

## 14. Consistency Notes (contradiction check)

- **Generic AuditEvent vs. approval trail.** The common `AuditEvent.Action` enum is only {CREATED, UPDATED, DELETED} and cannot express SUBMITTED/APPROVED/REJECTED with level+reason. Resolution: the authoritative approval audit trail is the owned **`workflow_steps`** table (FR-4). A generic `AuditEvent` (UPDATED) *may* also be emitted for the draft status change, but it is not the approval trail. No contradiction — two distinct records with distinct purposes.
- **"publishes if final level" (AC-2 wording) vs. scope.** The story AC says "or publishes if final level." Publication side-effects (notifications, calendar feed) are BRD 15/16 and owned by **A4-21**. Resolution: final approval sets `DraftStatus=APPROVED` and emits a publication signal; A4-21 turns that into PUBLISHED + side-effects. HC-AW-6 records this boundary; flagged as OQ-4 for confirmation of exactly where the PUBLISHED transition is written.
- **DraftStatus already contains APPROVED/PUBLISHED.** The enum exists (A4-11) — this story does not redefine it, only drives transitions into UNDER_REVIEW/APPROVED.
- **Return-to-previous-level (AC-3) semantics.** "Previous level" means the prior review level; at the first review level a rejection returns the draft to the submitter (status back to DRAFT). Made explicit in FR-3.1 to avoid an off-by-one ambiguity.

---

## 15. Out of Scope

- **Publication and notifications** (PUBLISHED transition side-effects, feeds) — A4-21.
- **RBAC enforcement** of who may act at a level — pending the Auth module (levels carry an advisory `requiredRole`).
- **Version comparison / diff** between draft iterations — A4-20.
- **Compliance publication-gating** — A4-33.
- **Frontend** approval UI (submission/review screens, status tracking) — a paired frontend story (not this backend story).
- Full CRUD **API** for pipelines/levels (beyond seeding + read) unless confirmed (OQ-1).

---

## 16. Open Questions

| # | Question | Why it matters | Proposed default |
|---|----------|----------------|------------------|
| OQ-1 | Should this story expose a pipeline-configuration **API** (CRUD for pipelines/levels), or is seed + DB edit sufficient for "configurable" (AC-5)? | Scope of the API surface. | Seed a default pipeline + read endpoints now; full pipeline CRUD as a follow-up (config screen is a frontend story). Confirm. |
| OQ-2 | Is the pipeline **global** or scoped per department/campus? | Determines pipeline resolution (FR-6.3). | One global active pipeline for this story; add scoping later. Confirm. |
| OQ-3 | RBAC is not built (permitAll). How is "who may act at this level" enforced now? | AC integrity for level authorization. | Store `actorUserId` from request context + advisory `requiredRole` on the level; enforce when Auth lands. Confirm the userId source (header/principal). |
| OQ-4 | Where is the `PUBLISHED` transition written — here on final approval, or entirely in A4-21? | Ownership boundary (HC-AW-6). | This story sets APPROVED + emits a final-approval event; A4-21 sets PUBLISHED. Confirm. |
| OQ-5 | Optimistic concurrency: use a new `@Version` on `workflow_instances`, the draft `version`, or both? | Prevents concurrent-action corruption (FR-7.2). | Add `@Version` to `workflow_instances`; reject stale writes with 409. Confirm. |
| OQ-6 | Should a "withdraw submission" action exist (submitter pulls a draft out of review)? | The story doesn't mention it; `WITHDRAWN` state is modeled but unused otherwise. | Not in scope for this story; keep the enum value reserved. Confirm. |

---

## 17. Traceability

| BRD / Story AC | Requirement |
|----------------|-------------|
| BRD 6.8 (multi-level Coordinator→HOD→Dean/Registrar) / AC-1, AC-2 | FR-1, FR-2, FR-6 |
| BRD 6.8 (review comments) | FR-2.1, FR-3.1, `comments` field |
| BRD 6.8 (rejection with reason) / AC-3, AC-7 | FR-3, HC-AW-2 |
| BRD 6.8 (full approval audit trail) / AC-4 | FR-4, `workflow_steps`, HC-AW-3/HC-AW-5 |
| AC-5 (configurable pipeline) | FR-6 |
| AC-6 (status/level visibility) | FR-5 |
| AC-8 (illegal transition) | FR-7, HC-AW-4 |
| "publishes if final level" | FR-2.2 + HC-AW-6 (boundary to A4-21, OQ-4) |
