# A4-21 — Timetable Publication and Notification Trigger — Design Document

| Field | Value |
|-------|-------|
| Story | A4-21 — Timetable Publication and Notification Trigger |
| Design Subtask | A4-93 |
| Requirement Subtask | A4-92 (Approved) |
| Epic | A4 — UTMS |
| Role / Type | **Backend** (Java 17 / Spring Boot 3.x, Spring Data JPA) |
| Source requirement | docs/requirements/A4-21-timetable-publication-notification-requirements.md |
| Migration | **None** (reuse AuditEvent — OQ-4/PD-110; latest is V14) |
| PD sequence | **PD-107 … PD-112** (project last used: PD-106 in A4-19) |

---

## 1. Introduction

Backend design for timetable publication. New package `com.utms.publication`: a transactional `PublicationService` that transitions an APPROVED draft to PUBLISHED, supersedes the prior published version for the same scope, identifies affected faculty/batches from the draft's sessions, and emits a `TimetablePublishedEvent` (AFTER_COMMIT) that A4-37 (notifications) and A4-39 (feeds) will consume. Two entry points: a manual publish endpoint and an AFTER_COMMIT listener on A4-19's `DraftApprovedEvent`. No new table (audit via the existing `AuditEventPublisher`). Follows the A4-19 event-boundary precedent.

---

## 2. Resolved Open Questions → Provisional Decisions (lead-approved 2026-09-09; logged in docs/open-questions-log.md)

| OQ | Decision | PD |
|----|----------|----|
| OQ-1 (notifications) | Emit `TimetablePublishedEvent` with recipients; delivery deferred to A4-37 (consumer). | **PD-107** |
| OQ-2 (feeds) | Same event signals feed refresh; regeneration deferred to A4-39 (consumer). | **PD-108** |
| OQ-3 (students) | Address students at batch/section granularity; event carries `affectedBatchIds`/`affectedSectionIds`. | **PD-109** |
| OQ-4 (audit vs log) | Reuse `AuditEvent` (UPDATED) for the publish action; **no new table/migration**. | **PD-110** |
| OQ-5 (re-publish) | Re-publishing an already-PUBLISHED draft → 422 "already published". | **PD-111** |
| OQ-6 (endpoint) | `POST /api/v1/timetables/{draftId}/publish` on the existing `SchedulingController`. | **PD-112** |

---

## 3. Module Layout

```
com.utms.publication/
├── event/
│   └── TimetablePublishedEvent.java   # {draftId, departmentId, semester, academicYear,
│                                      #  publishedByUserId, publishedAt,
│                                      #  affectedFacultyIds, affectedBatchIds, affectedSectionIds}
├── dto/
│   └── PublicationResultDto.java      # {draftId, status, supersededDraftId?, affectedFacultyCount, affectedBatchCount}
├── listener/
│   └── DraftApprovedListener.java     # @TransactionalEventListener(AFTER_COMMIT) on A4-19 DraftApprovedEvent
└── service/
    └── PublicationService.java        # publish(draftId, actor): transition + supersede + identify + audit + emit
```
Plus one endpoint added to the existing `com.utms.scheduling.engine.controller.SchedulingController`:
`POST /api/v1/timetables/{draftId}/publish` (PD-112). No new migration (PD-110).

---

## 4. Data & Reuse

- **TimetableDraft** (A4-11) — scope tuple is `(departmentId, semester, academicYear)`; this story reads `status` and writes `PUBLISHED` / (prior) `SUPERSEDED`.
- **ScheduledSession** (A4-11) — `findByDraftIdAndDeletedAtIsNull(draftId)` gives the rows; affected faculty = distinct `facultyId`, affected batches = distinct `batchId`, affected sections = distinct non-null `sectionId`.
- **TimetableDraftRepository** — needs a finder for the current PUBLISHED draft in a scope (new derived method, see §5). Existing `supersedePreviousDrafts` supersedes DRAFT rows only, so it is **not** reused for the published-archival (different status filter) — a targeted supersede is done in the service instead (KD-A21-2).
- **Audit** — `AuditEventPublisher.publish(new AuditEvent("TimetableDraft", draftId, Action.UPDATED, prevStatus, "PUBLISHED", actor, Instant.now()))` (PD-110).
- **A4-19 `DraftApprovedEvent`** — consumed by the listener (auto path).

New repository method on `TimetableDraftRepository`:
```java
Optional<TimetableDraft> findByDepartmentIdAndSemesterAndAcademicYearAndStatusAndDeletedAtIsNull(
        Long departmentId, String semester, String academicYear, DraftStatus status);
```

---

## 5. Service Logic — `PublicationService.publish(Long draftId, String actor)` (`@Transactional`)

1. Load draft (`findByIdAndDeletedAtIsNull`, else 404).
2. **Idempotency/guard (PD-111, HC-PUB-1):** if `status == PUBLISHED` → 422 "already published"; if `status != APPROVED` → 422 "only an APPROVED draft can be published (current: …)".
3. **Archive prior (HC-PUB-2, AC-5):** find the current PUBLISHED draft for `(departmentId, semester, academicYear)`; if present and different id, set it `SUPERSEDED` and save. Captured as `supersededDraftId`.
4. **Transition (AC-1):** set the target draft `status = PUBLISHED`, save. (Steps 3–4 in the same transaction — HC-PUB-3.)
5. **Identify recipients (FR-3):** load the draft's sessions; compute distinct `affectedFacultyIds`, `affectedBatchIds`, `affectedSectionIds`.
6. **Audit (FR-5.1/PD-110):** publish an `AuditEvent(UPDATED)` for the draft status change.
7. **Emit (FR-4, HC-PUB-4):** publish `TimetablePublishedEvent` with the scope + publisher + timestamp + recipient id sets, so A4-37/A4-39 act on it **after commit**.
8. Return `PublicationResultDto`.

The event is published via `ApplicationEventPublisher` inside the transaction; consumers (A4-37/A4-39, when built) subscribe with `@TransactionalEventListener(phase = AFTER_COMMIT)` so their side-effects run only after the publish commits and never roll it back (HC-PUB-4 / KD-A21-3).

`actor` (PD-103 reuse): manual path → `CurrentUserProvider` (the A4-19 component, reused); auto path → `DraftApprovedEvent.approvedByUserId()`.

---

## 6. Auto-Publish Listener — `DraftApprovedListener`

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onDraftApproved(DraftApprovedEvent event) {
    publicationService.publish(event.draftId(), event.approvedByUserId());
}
```
Runs after A4-19's approval transaction commits (the draft is durably APPROVED), then performs publication in its own transaction (FR-1.2a / AC-7). If publication fails, it does not roll back the approval (already committed) — the failure is logged; a manual publish remains available (KD-A21-4).

---

## 7. API — added to `SchedulingController` (PD-112)

`POST /api/v1/timetables/{draftId}/publish` → `200 PublicationResultDto`.
- `// TODO @PreAuthorize("hasRole('REGISTRAR')")` (RBAC deferred).
- `actor` from `CurrentUserProvider`, not the body.
- Errors via the global handler: 404 (no draft), 422 (not APPROVED / already PUBLISHED).
- Response shape: DTO returned directly (consistent with the scheduling module).

---

## 8. Key Decisions

- **KD-A21-1 — Event-only integration for notifications/feeds (PD-107/PD-108).** A4-21 owns recipient *identification* and the `TimetablePublishedEvent`; A4-37/A4-39 own delivery/regeneration. This story has **no compile dependency** on A4-37/A4-39 — they depend on the event type, which lives in `com.utms.publication.event`. Until they exist, the event is emitted with no consumer (harmless).
- **KD-A21-2 — Targeted published-archival, not `supersedePreviousDrafts`.** The existing repo query supersedes only DRAFT rows; publication must supersede the prior **PUBLISHED** row for the scope. Done via an explicit find-current-published + set SUPERSEDED in the service, so the two supersede paths stay distinct and correct.
- **KD-A21-3 — Publish event AFTER_COMMIT for consumers.** The service publishes the event within the transaction; consumers use `@TransactionalEventListener(AFTER_COMMIT)`. No side-effect can roll back a committed publish (HC-PUB-4).
- **KD-A21-4 — Auto-publish is best-effort post-approval.** The `DraftApprovedEvent` listener runs after the approval commit; a publish failure there is logged and recoverable via the manual endpoint — it never corrupts the (already committed) approval.
- **KD-A21-5 — Students at batch/section granularity (PD-109).** No student/enrolment entity exists; the event carries batch/section id sets. Per-student fan-out is A4-37's job when a student model lands.

---

## 9. Cross-Cutting / Consistency Notes (P6 Step 3)

- **APPROVED→PUBLISHED boundary vs A4-19.** A4-19 sets APPROVED + emits `DraftApprovedEvent` (its HC-AW-6 / PD-104); A4-21 sets PUBLISHED on that event. No double-write; the two stories meet exactly at the event. Verified against the A4-19 design.
- **Single active published (HC-PUB-2) × transaction (HC-PUB-3).** find-current-published + supersede + set-new-published all in one `@Transactional` method → never zero or two active published for a scope. A DB-level partial unique index is **not** added this story (would need a migration; PD-110 says none) — the invariant is enforced in the service; noted as a possible hardening follow-up.
- **Audit reuse (PD-110) × approval trail.** The generic `AuditEvent(UPDATED)` records the publish; it is distinct from A4-19's `workflow_steps` approval trail. No overlap.
- **Idempotency (PD-111).** Re-publish → 422, so no duplicate `TimetablePublishedEvent` is emitted (prevents a notification/feed storm once A4-37/A4-39 exist).
- **Auto + manual paths converge** on the same `publish(draftId, actor)` — one code path, two triggers (AC-1 and AC-7 identical behavior).
- **No new dependency / no migration** — consistent with "match infrastructure to scale" and PD-110.

---

## 10. Testing Strategy (design-level; execution gated separately by lead)

JUnit 5 + Mockito for `PublicationService`:
- publish APPROVED draft → PUBLISHED; result + audit + event emitted (AC-1).
- prior PUBLISHED for same scope → SUPERSEDED; new one PUBLISHED (AC-5).
- affected faculty/batch/section id sets computed as distinct values from sessions (AC-2/AC-3).
- event carries scope + recipients (AC-2/3/4 trigger).
- non-APPROVED draft → 422, no transition/event (AC-6); already-PUBLISHED → 422 (PD-111).
- `DraftApprovedListener` invokes publish with the event's draftId/approver (AC-7).
- Runner: `mvn test`. **Maven unavailable in this environment** — inspection-verified; real `mvn clean package` + `mvn test` gated pre-push (as with A4-19).

---

## 11. Requirement Traceability

| Requirement | Design element |
|-------------|----------------|
| FR-1 publish (manual + auto) | `PublicationService.publish` + `SchedulingController` endpoint (PD-112) + `DraftApprovedListener` (§6) |
| FR-1.3 illegal publish | §5 step 2 guard (422) |
| FR-2 archive prior | §5 step 3 + KD-A21-2 (SUPERSEDED), new repo finder |
| FR-3 identify recipients | §5 step 5 (distinct faculty/batch/section) |
| FR-4 emit event | §5 step 7 + `TimetablePublishedEvent` (KD-A21-1/KD-A21-3) |
| FR-5 audit | §5 step 6 (AuditEvent, PD-110) |
| FR-6 idempotency/atomicity | PD-111 (422) + `@Transactional` (HC-PUB-3) |
| HC-PUB-1..5 | guards; single-active; atomic; AFTER_COMMIT; no-delivery |
| NFR-1..7 | layered; parameterized JPA; audit; no migration; single indexed session query; event decoupling; JUnit |
| AC-1..AC-7 | §10 tests map each |

---

## 12. Dependencies & Provisional Items

- Existing: `TimetableDraft`/`DraftStatus`/`TimetableDraftRepository` (+ new finder), `ScheduledSession`/`ScheduledSessionRepository`, `AuditEventPublisher`, `CurrentUserProvider` (A4-19), `SchedulingController`, `GlobalExceptionHandler`.
- A4-19 `DraftApprovedEvent` (auto trigger).
- Downstream consumers (not needed to compile/run): A4-37, A4-39 subscribe to `TimetablePublishedEvent`.
- Provisional (lead-ratified defaults): PD-107/108 (event-only boundary), PD-109 (batch/section granularity), PD-110 (no migration), PD-111 (422 re-publish).
- **Build caveat:** Maven not available here; inspection-verified; `mvn clean package` + `mvn test` required before push.

---

## 13. Out of Scope

- Notification delivery (A4-37); calendar-feed regeneration (A4-39); per-student model; version diff (A4-20); frontend publish UI; RBAC enforcement; a DB-level single-published unique index (service-enforced this story).
