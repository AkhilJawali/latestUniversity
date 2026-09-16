# A4-21 — Timetable Publication and Notification Trigger — Requirement Document

| Field | Value |
|-------|-------|
| Story | A4-21 — Timetable Publication and Notification Trigger |
| Requirement Subtask | A4-92 |
| Epic | A4 — UTMS |
| Role / Type | **Backend** (Java 17 / Spring Boot 3.x, Spring Data JPA, Flyway) |
| BRD Requirements | 6.8 (final sign-off → publication), 6.13 (notifications on publication), 6.14 (calendar feeds update on publication), 6.16 (archive previous published for historical comparison) |
| Story Points | 5 |
| Builds on | A4-11 `TimetableDraft`/`DraftStatus` + `ScheduledSession` (exist); A4-19 `DraftApprovedEvent` (exists) |
| Depends on (downstream consumers, NOT built) | A4-37 Notification Delivery Engine (To Do), A4-39 iCal Calendar Feed (To Do) |

---

## 1. Introduction

This document specifies the **backend** that publishes an approved timetable draft: it transitions the draft to `PUBLISHED`, makes it the active schedule for its department+semester, archives any previously published version (→ `SUPERSEDED`), identifies the affected faculty and student batches, and **emits a publication event** carrying those recipients so notification delivery (A4-37) and calendar-feed refresh (A4-39) can act on it.

Critical scope boundary: **the notification-delivery engine (A4-37) and the calendar-feed service (A4-39) do not exist yet** (both `To Do`, no code). This story therefore **owns the publish transition + archival + affected-user identification + a publication event**, and treats actual multi-channel delivery and feed regeneration as the responsibility of A4-37/A4-39 (event consumers). This mirrors the A4-19 → A4-21 boundary (A4-19 emits `DraftApprovedEvent`; this story consumes it). The event contract is the deliverable that unblocks A4-37/A4-39 (§16, OQ-1/OQ-2).

---

## 2. User Story

*As a* Registrar,
*I want* approved timetables to transition to "published" status with automatic notifications to all affected faculty and students and calendar feed refresh,
*so that* everyone has immediate access to the finalized schedule through their preferred channel.

---

## 3. Actors

- **Registrar** — may manually publish an approved draft.
- **System (approval workflow)** — on final approval (A4-19), auto-triggers publication via the `DraftApprovedEvent` listener.
- **Downstream services (A4-37, A4-39)** — consume the emitted publication event (not built yet).

---

## 4. Domain Model & Constraint Inventory (P5 Step 1)

Referenced (owned elsewhere):
- **TimetableDraft / DraftStatus** (A4-11) — `PUBLISHED` and `SUPERSEDED` values already exist; this story writes them.
- **ScheduledSession** (A4-11) — has `draftId, facultyId, batchId, sectionId, courseId, roomId, dayOfWeek, slotDefinitionId`; the source for identifying affected faculty (distinct `facultyId`) and affected student groups (distinct `batchId`/`sectionId`).
- **A4-19 `DraftApprovedEvent`** `{draftId, workflowInstanceId, approvedByUserId, approvedAt}` — the auto-publish trigger.

Owned by this story (new):
- **TimetablePublishedEvent** (application event) — `{draftId, departmentId, semester, academicYear, publishedByUserId, publishedAt, affectedFacultyIds, affectedBatchIds}`. The contract A4-37 (notifications) and A4-39 (feeds) will consume.
- **A publication service** that performs the transition + archival + recipient identification + event emission.
- Optionally a small **publication record / audit** entry (see OQ-4) — or reuse the generic `AuditEvent`.

CRUD coverage: this story does not introduce a CRUD resource; it performs a **state transition** on an existing draft plus an event emission. No new persisted entity is strictly required unless a publication log is wanted (OQ-4).

---

## 5. Functional Requirements

### FR-1 — Publish an approved draft
- **FR-1.1** The system shall publish a draft that is in `APPROVED` status: set `DraftStatus = PUBLISHED` and make it the active published schedule for its (departmentId, semester, academicYear) (AC-1).
- **FR-1.2** Publication shall be reachable two ways: (a) **automatically** when A4-19 emits `DraftApprovedEvent` on final approval (consumed via an AFTER_COMMIT listener), and (b) **manually** via `POST /api/v1/timetables/{draftId}/publish` for a Registrar-triggered publish of an already-APPROVED draft.
- **FR-1.3** Publishing a draft not in `APPROVED` status shall be rejected with a business-rule error (422).

### FR-2 — Archive the previous published version
- **FR-2.1** When a draft is published and a previously `PUBLISHED` draft exists for the same (departmentId, semester, academicYear), the previous one shall be transitioned to `SUPERSEDED` (archived, still queryable for historical comparison per BRD 6.16) (AC-5).
- **FR-2.2** At most one `PUBLISHED` draft shall exist per (departmentId, semester, academicYear) at any time.

### FR-3 — Identify affected recipients
- **FR-3.1** On publication, the system shall compute the affected **faculty** as the distinct `facultyId`s across the published draft's sessions (AC-2 targeting).
- **FR-3.2** The system shall compute the affected **student groups** as the distinct `batchId`s (and `sectionId`s where present) across the published draft's sessions (AC-3 targeting). *(There is no per-student entity yet; students are addressed at batch/section granularity — see §14 / OQ-3.)*

### FR-4 — Emit the publication event (notification + feed trigger)
- **FR-4.1** On successful publication (after commit), the system shall emit a `TimetablePublishedEvent` carrying the draft/department/semester identity, the publisher, the timestamp, and the affected faculty/batch ids (AC-2, AC-3, AC-4 trigger).
- **FR-4.2** The event is the integration contract for **A4-37** (delivers notifications via each user's preferred channel) and **A4-39** (refreshes affected calendar feeds). This story does **not** implement delivery or feed regeneration — those are consumer stories (§15, OQ-1/OQ-2).
- **FR-4.3** Emitting the event shall not roll back the publication if a (future) consumer fails; the event fires AFTER_COMMIT.

### FR-5 — Publication status & audit
- **FR-5.1** The publish operation shall record an audit entry (who published, when, which draft, dept/semester) via the existing audit mechanism (AuditEvent) or a dedicated publication log (OQ-4).
- **FR-5.2** `GET /api/v1/timetables/{draftId}` (existing) reflects the `PUBLISHED`/`SUPERSEDED` status after the operation; no new read endpoint is required by this story.

### FR-6 — Idempotency & integrity
- **FR-6.1** Re-publishing an already-`PUBLISHED` draft shall be a no-op success (idempotent) or rejected (422) — see OQ-5; it shall never create a second active version or a duplicate event storm.
- **FR-6.2** The transition + archival shall occur in a single transaction so the system never has zero or two active published versions for the same scope.

---

## 6. Constraints Owned by This Document

- **HC-PUB-1 (Publish only from APPROVED):** publication requires `DraftStatus == APPROVED`.
- **HC-PUB-2 (Single active published per scope):** at most one `PUBLISHED` draft per (departmentId, semester, academicYear); publishing a new one supersedes the prior (AC-5).
- **HC-PUB-3 (Atomic transition + archive):** the new-PUBLISHED and prior-SUPERSEDED writes commit together (FR-6.2).
- **HC-PUB-4 (Event after commit):** the publication event fires only after the transition commits (FR-4.3), so consumers never act on an un-committed publish.
- **HC-PUB-5 (No delivery in this story):** notification delivery and feed regeneration are out of scope; only the event + recipient identification are owned here.

---

## 7. Constraints Referenced from Other Documents

- **DraftStatus lifecycle (A4-11):** DRAFT → UNDER_REVIEW → APPROVED → **PUBLISHED** → **SUPERSEDED**. This story writes PUBLISHED and SUPERSEDED.
- **A4-19 `DraftApprovedEvent`:** consumed to auto-publish on final approval (FR-1.2a). (A4-19 sets APPROVED; this story sets PUBLISHED — the boundary agreed in A4-19 OQ-4/HC-AW-6.)
- **A4-37 (notifications) / A4-39 (feeds):** consume `TimetablePublishedEvent`; enforce channel preference, digests, retries (A4-37) and feed regeneration (A4-39). Not built yet.
- **ScheduledSession (A4-11):** source of affected faculty/batch ids.

---

## 8. Validation Rules

| Field | Rule |
|-------|------|
| publish.draftId (path) | required; must reference an existing draft in APPROVED status (else 422) |
| (auto path) DraftApprovedEvent.draftId | must reference a draft that is APPROVED at handling time |

---

## 9. Non-Functional Requirements

- **NFR-1 (Layered architecture):** Controller → Service (`@Transactional`) → Repository; no controller-to-repository calls; entities mapped to DTOs where returned.
- **NFR-2 (Security/standards):** parameterized JPA; no stack traces exposed (EntityNotFound→404, business-rule→422, optimistic-lock→409 via the global handler).
- **NFR-3 (Auditability):** the publish action is audited (FR-5.1).
- **NFR-4 (Migrations):** only if a publication-log table is chosen (OQ-4) — next Flyway version **V15** (V14 is the latest, created by A4-19), reversible, all BaseEntity columns. Default proposal is **no new table** (reuse AuditEvent), hence no migration.
- **NFR-5 (Performance):** recipient identification is a single indexed query over the draft's sessions; publish + archive < 500ms for a department-sized draft.
- **NFR-6 (Decoupling):** consumers subscribe via Spring events; this story has no compile dependency on A4-37/A4-39 (they depend on the event type, which this story owns).
- **NFR-7 (Testing):** JUnit 5 + Mockito for the transition (APPROVED→PUBLISHED), archival of the prior published, recipient computation, event emission, and the illegal-state (non-APPROVED) path.

---

## 10. Acceptance Criteria (Given / When / Then)

1. **Publish → active (AC-1):** *Given* a draft in APPROVED, *When* published, *Then* its status becomes PUBLISHED and it is the active schedule for its dept/semester.
2. **Faculty notified (AC-2):** *Given* publication, *When* affected faculty are identified, *Then* a publication event is emitted carrying each affected `facultyId` (delivery via preferred channel is A4-37's responsibility — see §15).
3. **Students notified (AC-3):** *Given* publication, *When* affected student groups are identified, *Then* the event carries the affected `batchId`s/`sectionId`s (delivery is A4-37's responsibility; student-level granularity per OQ-3).
4. **Feeds refreshed (AC-4):** *Given* publication, *When* the event is emitted, *Then* it signals affected users so calendar feeds can be refreshed (regeneration is A4-39's responsibility — see §15).
5. **Previous archived (AC-5):** *Given* an existing PUBLISHED version for the same scope, *When* a new draft is published, *Then* the previous one becomes SUPERSEDED and remains queryable.
6. **Illegal publish (edge):** *Given* a draft not in APPROVED, *When* publish is attempted, *Then* a 422 is returned and no transition/event occurs.
7. **Auto-publish on final approval:** *Given* A4-19 emits `DraftApprovedEvent`, *When* handled, *Then* the draft is published (same path as manual publish).

---

## 11. Data Model (conceptual)

- No new persisted entity required by default. If a publication log is adopted (OQ-4): **timetable_publications** (BaseEntity + `draft_id`, `department_id`, `semester`, `academic_year`, `published_by`, `published_at`, `superseded_draft_id?`).
- **TimetablePublishedEvent** (transient application event) — see §4.

---

## 12. Dependencies

- A4-11: `TimetableDraft`/`DraftStatus`/`TimetableDraftRepository`, `ScheduledSession`/`ScheduledSessionRepository`.
- A4-19: `DraftApprovedEvent` (auto-publish trigger).
- Common: `BaseEntity`, `GlobalExceptionHandler`, `AuditEventPublisher`, Spring `ApplicationEventPublisher`.
- Downstream (not required to compile/run this story): A4-37, A4-39 consume `TimetablePublishedEvent`.

---

## 13. Assumptions

1. A published draft's active-schedule identity is (departmentId, semester, academicYear) — the same tuple `TimetableDraft` carries and `supersedePreviousDrafts` already keys on.
2. Affected faculty/batches are fully derivable from the draft's `ScheduledSession` rows.
3. A user id for the publisher is available from the request context (manual path) or the event (`approvedByUserId`, auto path) — consistent with A4-19 PD-103.
4. A4-37/A4-39 will subscribe to `TimetablePublishedEvent` when built; until then, publication completes and the event is emitted with no consumer (harmless).

---

## 14. Consistency Notes (contradiction check)

- **"notifications to faculty and students" (AC-2/AC-3) vs. no notification engine.** A4-37 is `To Do` (no code). This story cannot *deliver* notifications; it identifies recipients and emits the event. Delivery, channel preference, digests, and retries are A4-37 (BRD 6.13). Recorded as HC-PUB-5 + OQ-1; ACs re-scoped to "event emitted carrying recipients."
- **"calendar feed refresh" (AC-4) vs. no feed service.** A4-39 is `To Do` (no code). Feed regeneration is A4-39 (BRD 6.14); this story signals it via the event. OQ-2.
- **"students" vs. no student entity.** No per-student model exists; sessions carry `batchId`/`sectionId`. Students are addressed at batch/section granularity; per-student fan-out belongs to A4-37 when a student/enrolment model exists. OQ-3.
- **APPROVED → PUBLISHED boundary (A4-19).** A4-19 stops at APPROVED and emits `DraftApprovedEvent`; this story owns the PUBLISHED transition. Consistent with the A4-19 design (HC-AW-6 / PD-104). No double-write.
- **SUPERSEDED semantics (A4-11).** The enum comment states SUPERSEDED drafts remain queryable for version comparison — exactly what AC-5 needs. Reused, not redefined.

---

## 15. Out of Scope

- **Notification delivery** (multi-channel dispatch, preferred-channel routing, digests, retries) — A4-37.
- **Calendar feed generation/refresh** (.ics, subscription URLs) — A4-39.
- **Per-student notification fan-out / student entity** — depends on a student/enrolment model (not present).
- **Version comparison / diff UI** — A4-20 / reporting.
- **Frontend** publish button / status display — a frontend story.
- **RBAC** enforcement of who may publish — pending Auth (advisory; endpoint carries a `// TODO @PreAuthorize` for Registrar).

---

## 16. Open Questions

| # | Question | Why it matters | Proposed default |
|---|----------|----------------|------------------|
| OQ-1 | A4-37 (notification engine) is not built. Confirm A4-21 emits `TimetablePublishedEvent` (recipients included) and delivery is deferred to A4-37. | Defines the deliverable boundary for AC-2/AC-3. | Emit the event now; A4-37 consumes it later. Confirm. |
| OQ-2 | A4-39 (calendar feed) is not built. Confirm feed refresh is signalled via the same event and regeneration is deferred to A4-39. | AC-4 boundary. | Signal via the event; A4-39 regenerates later. Confirm. |
| OQ-3 | No per-student entity exists. Address students at batch/section granularity in the event? | AC-3 fidelity. | Carry `affectedBatchIds`/`sectionIds`; per-student fan-out when an enrolment model lands. Confirm. |
| OQ-4 | Persist a dedicated `timetable_publications` log, or reuse the generic `AuditEvent` for the publish action? | Whether a V15 migration is needed. | Reuse `AuditEvent` (no new table/migration) for this story; add a publication log later if reporting needs it. Confirm. |
| OQ-5 | Re-publishing an already-PUBLISHED draft: idempotent no-op, or 422? | FR-6.1 behavior. | Treat as 422 "already published" (explicit), to avoid duplicate event emission. Confirm. |
| OQ-6 | Manual publish endpoint path: `POST /timetables/{draftId}/publish` (extends the existing SchedulingController resource) vs. a new approvals/publication controller. | API placement/consistency. | `POST /api/v1/timetables/{draftId}/publish` (co-located with the draft resource). Confirm. |

---

## 17. Traceability

| BRD / Story AC | Requirement |
|----------------|-------------|
| BRD 6.8 (final sign-off → publication) / AC-1, AC-7 | FR-1 (manual + auto via DraftApprovedEvent) |
| BRD 6.13 (notifications on publication) / AC-2, AC-3 | FR-3 (identify) + FR-4 (event); delivery = A4-37 (§15) |
| BRD 6.14 (calendar feeds update) / AC-4 | FR-4 (event signal); regeneration = A4-39 (§15) |
| BRD 6.16 (archive previous published) / AC-5 | FR-2 (SUPERSEDED), HC-PUB-2 |
| AC-6 (illegal publish) | FR-1.3, HC-PUB-1 |
| Idempotency / single active version | FR-6, HC-PUB-2/HC-PUB-3 |
