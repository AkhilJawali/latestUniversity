# Design: Fortnightly and Alternate-Week Scheduling Patterns

**Jira Reference:** A4-13
**Source Requirements:** docs/requirements/A4-13-fortnightly-alternate-week-scheduling-patterns-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven · PostgreSQL 15+ · Flyway
**Generated:** 2026-09-01

## 1. Overview

This design adds a **recurrence pattern** to a scheduled session so it can occur every teaching week (Weekly — the existing behavior) or every other teaching week (Fortnightly / Alternate-Week). It delivers three things at the engine/service layer:

1. Two new persisted attributes on `scheduled_sessions` — `recurrence_type` and `week_group` — that default to the existing weekly behavior for all current rows (backward compatible).
2. A **`WeekParityResolver`** that deterministically maps any teaching date to a week group (Week-A / Week-B), anchored to the semester start date from the academic calendar (A4-9).
3. A **`SessionRecurrenceService`** that (a) validates and persists a session's recurrence pattern, and (b) exposes reusable read operations — "do these two sessions ever occur in the same week?" (the alternate-week non-conflict rule) and "on which dates does this session actually occur?" — for downstream consumers.

The rule that two sessions on opposite week groups sharing one slot do **not** conflict (and two whose weeks overlap **do** conflict) is owned here as a reusable predicate. The conflict-detection engine (A4-16), the drag-drop editor (A4-15), and the calendar-feed exporter (A4-39) are **not built yet**; this story exposes the service and read APIs they will consume, and its own edit/read endpoints exercise the columns so nothing is dead storage.

Fortnightly recurrence relaxes only the *which-weeks* dimension. On every week a session actually occurs, it remains subject to all A4-11 hard constraints (capacity, equipment, faculty availability, grid conformance); this story does not weaken any of those — it only narrows the set of weeks (FR-4.3).

**Not in scope:** the conflict-detection engine itself (A4-16), editor UI (A4-15), iCal feed generation (A4-39), engine auto-assignment of week groups to lab sub-groups (SC-FN-1, deferred), recurrence periods beyond fortnightly (A4-13 OQ#6), post-publication editing (A4-48).

## 2. Architecture

```
Controller Layer
    └── SessionRecurrenceController
         │  PUT   /api/v1/timetables/sessions/{sessionId}/recurrence   (set/change pattern)
         │  DELETE/api/v1/timetables/sessions/{sessionId}/recurrence   (revert to weekly)
         │  GET   /api/v1/timetables/sessions/{sessionId}/occurrences  (occurrence dates — for feed/display)
         │
Service Layer
    ├── SessionRecurrenceService (validate → persist pattern → audit; read: occurrences, overlap)
    ├── WeekParityResolver (date → WeekGroup, anchored to semester start; O(1))
    └── RecurrenceOverlapEvaluator (do two sessions ever co-occur? — HC-FN-1 / HC-FN-2)
         │
Event Publishing
    └── AuditEventPublisher (same contract as A4-2 / A4-11; within edit @Transactional)
         │
Repository Layer
    └── ScheduledSessionRepository (existing — reused; extended with recurrence columns)
         │
Master Data Read Contracts (consumed, not owned)
    ├── AcademicCalendarRepository / CalendarQueryService (semester start anchor + isWorkingDay) — A4-9
    └── (draft → department → campus resolution reuses existing associations) — A4-2 / A4-11
         │
Database (existing scheduled_sessions table, extended)
    └── scheduled_sessions  (+ recurrence_type, + week_group)
```

Reusable predicates consumed later by A4-16 / A4-15 / A4-39:
- `RecurrenceOverlapEvaluator.everCoOccur(SessionRecurrence a, SessionRecurrence b) : boolean`
- `SessionRecurrenceService.getOccurrenceDates(sessionId) : List<LocalDate>`

## Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-59 | Recurrence is stored as **two columns on `scheduled_sessions`** (`recurrence_type`, `week_group`), not a separate table. | It is 1:1 with a session, always read with it, and must travel through draft→publish (FR-7.1). A separate table adds a join for no benefit. | Backward compat: `recurrence_type` DEFAULT 'WEEKLY' NOT NULL; `week_group` NULL. Existing rows become WEEKLY automatically. |
| KD-60 | `recurrence_type` and `week_group` are Java enums persisted as `@Enumerated(STRING)`. `RecurrenceType {WEEKLY, FORTNIGHTLY}`, `WeekGroup {WEEK_A, WEEK_B}`. | Data-access standard forbids ordinals; STRING is self-documenting and extensible for OQ#6. | Enum extension (e.g., add a 3-week period) is additive, no schema change. |
| KD-61 | Week parity is computed as a **sequential week index from the anchor Monday**: `weekIndex = WEEKS.between(mondayOf(semesterStart), mondayOf(date))`; `WEEK_A` iff `weekIndex` is even, else `WEEK_B`. Parity is by **calendar-week index**, independent of holidays. | O(1), deterministic, reproducible (NFR determinism). Using calendar-week index (not a count of teaching days) prevents a holiday from renumbering subsequent weeks. | Resolves the "holiday skip vs. parity shift" collision (see §9). Anchor value is PD-81. |
| KD-62 | A holiday/non-working occurrence is **skipped** (that single occurrence does not happen), but the week retains its parity number. Occurrence dates are filtered through `CalendarQueryService.isWorkingDay`. | Matches how the rest of the system treats non-working days (A4-9 is the single source of truth). No shifting keeps the A/B alternation intact. | PD-82. Reuses existing A4-9 read contract; no new calendar logic. |
| KD-63 | The alternate-week non-conflict rule is a **pure predicate** `everCoOccur(a, b)` decided from `(recurrenceType, weekGroup)` alone — not from enumerated dates. Two sessions co-occur unless both are FORTNIGHTLY on opposite week groups. | The rule is week-group algebra, not date intersection, so it is O(1) and has no calendar dependency. A4-16 calls this before any date-level work. | This story OWNS the rule (HC-FN-1/HC-FN-2); A4-16 owns detection and calls this predicate. |
| KD-64 | Editing recurrence does **not** move the session's day or slot (FR-1.5); the endpoint only mutates the two recurrence columns. Reverting to WEEKLY clears `week_group` to NULL. | Recurrence changes *which weeks*, never *which day* (story AC #1 "every other Monday"). | Validation rejects `week_group` when type is WEEKLY (HC-FN-4). |

## Provisional Decisions

These resolve open questions from the requirement document. They are **provisional — pending stakeholder ratification** and are carried into §8.

| # | Decision | Resolves | Default Chosen | Rationale |
|---|---|---|---|---|
| PD-81 | Week-parity anchor = `academic_calendars.semester_start_date` for the session's campus/semester; the **first teaching week = WEEK_A** (even index). | A4-13 OQ#1 | Semester start | The calendar (A4-9) is the existing authority for the semester boundary; no new config needed. |
| PD-82 | A fortnightly occurrence landing on a holiday/non-working day/exam window is **skipped with no shift**; the alternation is preserved. | A4-13 OQ#2 | Skip, no shift | Consistent with A4-9 semantics; shifting would break A/B symmetry between paired lab groups. |
| PD-83 | Week-group representation = enum `WEEK_A` / `WEEK_B`; display labels ("Week A / odd weeks") are chosen by the frontend (A4-15). | A4-13 OQ#4 | WEEK_A / WEEK_B | Two disjoint groups is all fortnightly needs; naming is a UI concern. |
| PD-84 | Both alternate-week groups of a shared slot use the **same day and slot** (they differ only by week group). Cross-day A/B split is deferred. | A4-13 OQ#3 | Same day/slot | Matches requirement Assumption 2; simplest correct model for split labs. |

## 3. API Design

Base path `/api/v1/timetables/sessions/{sessionId}/recurrence` (and `/occurrences`). All endpoints require the caller to have access to the session's department (COORDINATOR own-dept, HOD/REGISTRAR any) — same scoping model as A4-11.

| Method | Path | Description | Auth | Traces To |
|---|---|---|---|---|
| PUT | `/{sessionId}/recurrence` | Set or change the session's recurrence pattern | COORDINATOR (own dept) / HOD / REGISTRAR | FR-1.1, FR-1.2, FR-1.4, FR-1.5 |
| DELETE | `/{sessionId}/recurrence` | Revert the session to weekly | COORDINATOR (own dept) / HOD / REGISTRAR | FR-1.4 |
| GET | `/{sessionId}/occurrences` | List the actual occurrence dates within the semester (holidays skipped) | COORDINATOR+ | FR-4.2, FR-6.1 |

### PUT request body

```json
{ "recurrenceType": "FORTNIGHTLY", "weekGroup": "WEEK_A" }
```
For `"recurrenceType": "WEEKLY"`, `weekGroup` must be omitted/null.

### Success response (PUT) — 200

```json
{
  "sessionId": 4821,
  "dayOfWeek": "MONDAY",
  "slotDefinitionId": 33,
  "recurrenceType": "FORTNIGHTLY",
  "weekGroup": "WEEK_A"
}
```

### GET occurrences response — 200

```json
{
  "sessionId": 4821,
  "recurrenceType": "FORTNIGHTLY",
  "weekGroup": "WEEK_A",
  "occurrenceDates": ["2026-01-05", "2026-01-19", "2026-02-02", "2026-02-16"]
}
```

### Validation / error responses (same envelope as A4-11)

- **422** — `week_group` supplied for a WEEKLY session, or missing for a FORTNIGHTLY session (HC-FN-4):
```json
{"timestamp":"...","status":422,"error":"Unprocessable Entity","message":"Invalid recurrence pattern","path":"/api/v1/timetables/sessions/4821/recurrence","details":[{"field":"weekGroup","message":"weekGroup is required when recurrenceType is FORTNIGHTLY and must be null when WEEKLY"}]}
```
- **422** — anchor cannot be resolved (no academic calendar with a semester start date for the campus/semester):
```json
{"timestamp":"...","status":422,"error":"Unprocessable Entity","message":"Cannot compute week parity: no academic calendar with a semester start date for campus 1","path":"/api/v1/timetables/sessions/4821/occurrences","details":[{"check":"CALENDAR_ANCHOR","message":"semester_start_date is required to resolve fortnightly weeks"}]}
```
- **404** — session not found. **403** — caller outside the session's department.

## 4. Data Model

**Migration V12** (follows V11). Extends the existing `scheduled_sessions` table (created unqualified in V10 — the ALTER matches that style).

```sql
-- V12__add_recurrence_to_scheduled_sessions.sql
-- Story: A4-13 (Fortnightly and Alternate-Week Scheduling Patterns)
-- Design: KD-59 (columns on session), KD-60 (enums as STRING), PD-81..84
-- Rollback: ALTER TABLE scheduled_sessions DROP CONSTRAINT ck_session_recurrence_weekgroup,
--           DROP COLUMN week_group, DROP COLUMN recurrence_type; DROP INDEX idx_session_recurrence;

ALTER TABLE scheduled_sessions
    ADD COLUMN recurrence_type VARCHAR(20) NOT NULL DEFAULT 'WEEKLY';

ALTER TABLE scheduled_sessions
    ADD COLUMN week_group VARCHAR(10) NULL;

-- Enforce HC-FN-4 at the database as a safety net (service also validates):
ALTER TABLE scheduled_sessions
    ADD CONSTRAINT ck_session_recurrence_weekgroup
    CHECK (
        (recurrence_type = 'WEEKLY'      AND week_group IS NULL) OR
        (recurrence_type = 'FORTNIGHTLY' AND week_group IN ('WEEK_A','WEEK_B'))
    );

-- Partial index to fetch fortnightly sessions quickly (feed/display, alternate-week checks):
CREATE INDEX idx_session_recurrence
    ON scheduled_sessions(recurrence_type)
    WHERE deleted_at IS NULL AND recurrence_type <> 'WEEKLY';
```

Entity change (`ScheduledSession`) — two enum fields via `@Enumerated(EnumType.STRING)`:

| Column | Type | Notes |
|---|---|---|
| recurrence_type | VARCHAR(20) NOT NULL DEFAULT 'WEEKLY' | maps `RecurrenceType {WEEKLY, FORTNIGHTLY}` |
| week_group | VARCHAR(10) NULL | maps `WeekGroup {WEEK_A, WEEK_B}`; null when weekly |

No new table; the columns are covered by the existing `scheduled_sessions` BaseEntity audit columns (already present in V10). Existing rows are unaffected — the `DEFAULT 'WEEKLY'` backfills them.

## 5. Service Logic

### 5.1 WeekParityResolver (KD-61)

```
resolve(LocalDate date, LocalDate semesterStart) -> WeekGroup:
    LocalDate anchorMonday = semesterStart.with(previousOrSame(MONDAY))
    LocalDate dateMonday   = date.with(previousOrSame(MONDAY))
    long weekIndex = ChronoUnit.WEEKS.between(anchorMonday, dateMonday)
    return (weekIndex % 2 == 0) ? WEEK_A : WEEK_B      // PD-81: first week = WEEK_A
```
Pure, O(1), no I/O. The semester start comes from `AcademicCalendar.semesterStartDate` (A4-9), looked up once per request by the session's campus + semester.

### 5.2 RecurrenceOverlapEvaluator (KD-63 — owns HC-FN-1 / HC-FN-2)

```
everCoOccur(a, b) -> boolean:
    if a.type == WEEKLY  or b.type == WEEKLY:  return true    // a weekly session hits every week
    // both FORTNIGHTLY:
    return a.weekGroup == b.weekGroup                          // same group co-occurs; opposite never does
```
This is the exact rule from HC-FN-1 (opposite groups → false → no conflict) and HC-FN-2 (overlap → true → conflict). A4-16 calls this as a gate before running its slot/resource comparison. It depends on nothing but the two patterns, so it never needs the calendar.

### 5.3 SessionRecurrenceService.setRecurrence (FR-1.1, FR-1.2, FR-1.4, FR-1.5; HC-FN-4)

```
@Transactional
setRecurrence(sessionId, recurrenceType, weekGroup, actor):
    session = repo.findByIdAndDeletedAtIsNull(sessionId) or throw 404
    assertCallerInDepartment(session, actor)                 // 403 otherwise
    validate(recurrenceType, weekGroup)                      // HC-FN-4 -> 422
    before = snapshot(session)
    session.setRecurrenceType(recurrenceType)
    session.setWeekGroup(recurrenceType == FORTNIGHTLY ? weekGroup : null)  // KD-64 revert clears group
    // day_of_week and slot_definition_id are NOT modified (FR-1.5)
    repo.save(session)
    auditPublisher.publish(SESSION_RECURRENCE_CHANGED, session, before, after, actor)  // NFR audit
    return toDto(session)
```
`DELETE` calls `setRecurrence(sessionId, WEEKLY, null, actor)`.

`validate`: WEEKLY ⇒ weekGroup must be null; FORTNIGHTLY ⇒ weekGroup must be WEEK_A or WEEK_B. Day and slot are never touched (FR-1.5).

### 5.4 SessionRecurrenceService.getOccurrenceDates (FR-4.2, FR-6.1, FR-6.2; PD-82)

```
@Transactional(readOnly = true)
getOccurrenceDates(sessionId):
    session  = repo.findByIdAndDeletedAtIsNull(sessionId) or throw 404
    calendar = calendarRepo.findByCampusAndSemester(campusOf(session), semesterOf(session))
                 or throw 422 (CALENDAR_ANCHOR)
    targetDow = DayOfWeek.valueOf(session.dayOfWeek)
    dates = every date in [semesterStart, semesterEnd] whose dayOfWeek == targetDow
    if session.type == FORTNIGHTLY:
        dates = dates.filter(d -> parityResolver.resolve(d, semesterStart) == session.weekGroup)
    dates = dates.filter(d -> calendarQueryService.isWorkingDay(campusId, d))   // PD-82 skip holidays
    return dates
```
For a WEEKLY session this returns every working occurrence (feed behavior unchanged, FR-6.2). This is the read path that A4-39 (feed) and A4-15 (display) will consume; the story's own GET endpoint exercises it.

### 5.5 Department scoping

Reuses the A4-11 model: resolve `session.draftId → timetable_drafts.department_id`; COORDINATOR must match, HOD/REGISTRAR bypass. Returns 403 otherwise.

## 6. Cross-Cutting Concerns

| Concern | Design |
|---|---|
| Audit | `SESSION_RECURRENCE_CHANGED` event via `AuditEventPublisher` inside `setRecurrence`'s `@Transactional` (same pattern/authority as A4-11 KD-54, audit trail A4-43). Records before/after `{recurrenceType, weekGroup}`. |
| Security | Department scoping on all write/read endpoints (§5.5). RBAC annotations as per A4-11. |
| Error handling | 404 / 403 / 422 via existing `GlobalExceptionHandler`, same envelope `{timestamp,status,error,message,path,details}`. DB `CHECK` constraint is a safety net behind service validation. |
| Backward compatibility | `recurrence_type DEFAULT 'WEEKLY'` + nullable `week_group`; existing rows and existing engine output (A4-11) are unchanged. `everCoOccur` returns `true` for any weekly pair, preserving current conflict semantics. |
| Constraint scope (FR-4.3) | This story narrows only which weeks a session occurs. All other A4-11 hard constraints (capacity, equipment, faculty availability, grid conformance) are unchanged and continue to be enforced by their owners on each occurring week. |
| Determinism | Parity is a pure function of (date, semesterStart); identical inputs always yield the same group (NFR determinism). |
| Performance | `resolve` and `everCoOccur` are O(1); `getOccurrenceDates` is O(weeks-in-semester) with one `isWorkingDay` check per candidate — well within read-endpoint budget. |

## 7. Integration Contracts

| Service | Method | Contract | Owner |
|---|---|---|---|
| AcademicCalendarRepository | findByCampusIdAndSemester(...) → AcademicCalendar (semesterStartDate, semesterEndDate) | Throws/empty → 422 CALENDAR_ANCHOR. | A4-9 |
| CalendarQueryService | isWorkingDay(campusId, date) | Skips holiday/exam/orientation occurrences (PD-82). Throws 422 if no working-day pattern (KD-39). | A4-9 |
| ScheduledSessionRepository | findByIdAndDeletedAtIsNull(id); save(session) | Existing repo, extended with two columns. | A4-11 |
| AuditEventPublisher | publish(action, entity, before, after, actor) | Same contract as A4-2/A4-11; called within transaction. | A4-2 / A4-43 |
| **RecurrenceOverlapEvaluator** | **everCoOccur(a, b) : boolean** | **Provided by this story; consumed by A4-16 conflict detection.** | **A4-13 (this)** |
| **SessionRecurrenceService** | **getOccurrenceDates(sessionId) : List<LocalDate>** | **Provided by this story; consumed by A4-39 feed and A4-15 display.** | **A4-13 (this)** |

## 8. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| OQ#1 | Week-parity anchor definition. | Academic Affairs / System Design | PROVISIONAL (PD-81: semester start, first week = WEEK_A) — needs ratification |
| OQ#2 | Fortnightly occurrence on a holiday. | Academic Affairs / Registrar | PROVISIONAL (PD-82: skip, no shift) — needs ratification |
| OQ#3 | Same day/slot for both week groups vs. cross-day A/B. | Academic Affairs | PROVISIONAL (PD-84: same day/slot; cross-day deferred) |
| OQ#4 | Week-group naming/representation. | Academic Affairs / UX | PROVISIONAL (PD-83: WEEK_A/WEEK_B enum; labels in A4-15) |
| OQ#5 | Engine auto-assigns opposite groups to split labs? | Academic Affairs / System Design | CARRIED FORWARD (SC-FN-1 deferred; manual assignment only in this story) |
| OQ#6 | Recurrence periods beyond fortnightly. | Academic Affairs | CARRIED FORWARD (enum extensible; out of scope) |
| OQ#7 | Mid-semester calendar change re-maps parity? | Academic Affairs / System Design | CARRIED FORWARD (parity recomputed live from current semesterStart; re-mapping impact on already-placed sessions not handled here) |
| OQ-D1 | Should `everCoOccur` also be surfaced as a REST endpoint, or only as an in-process service for A4-16? | System Design | Provisional: in-process service only (A4-16 is in the same monolith) |

## 9. Consistency Notes

- **Migrations:** V12 follows V11. Uses unqualified table name `scheduled_sessions` to match V10 (engine tables were created without the `utms.` schema qualifier, unlike A4-9's `academic_calendars`).
- **Numbering:** KD-59..64 continue after KD-58 (V11). PD-81..84 continue after PD-80 (V11).
- **Audit columns:** No new table; recurrence columns live on `scheduled_sessions`, which already carries the full BaseEntity audit column set from V10.
- **Enums:** `@Enumerated(STRING)` per data-access standard; DB `CHECK` mirrors `HC-FN-4`.
- **Error envelope:** identical to A4-11 (`timestamp/status/error/message/path/details`).
- **Decision interaction resolved:** parity uses calendar-week index (KD-61), so PD-82's holiday-skip removes an occurrence without renumbering weeks — the A/B alternation between paired lab groups is preserved. Two weekly sessions still always co-occur, so existing conflict behavior is unchanged.
- **No dead storage:** the two columns are written by the PUT endpoint and read by GET `/occurrences`, `getOccurrenceDates`, and `everCoOccur`; downstream A4-16/A4-39/A4-15 consume the same service methods.

## 10. Requirement Survival (Traceability)

| Requirement item | Design element |
|---|---|
| FR-1 (pattern assignment / default weekly / revert) | KD-59/KD-60 columns; `setRecurrence` (§5.3); PUT & DELETE endpoints |
| FR-1.5 (recurrence tied to existing day, not moved) | KD-64; §5.3 (day/slot untouched) |
| FR-2 (week parity anchoring) | `WeekParityResolver` (§5.1); PD-81 |
| FR-3 / HC-FN-1 / HC-FN-2 (non-conflict rule) | `RecurrenceOverlapEvaluator.everCoOccur` (§5.2); KD-63 |
| FR-4.1 (expose pattern for detection) | `everCoOccur` + DTO fields; Integration Contracts §7 |
| FR-4.2 (check only occurring weeks) | `getOccurrenceDates` (§5.4); PD-82 |
| FR-4.3 (all other hard constraints still apply) | §6 Constraint scope row |
| FR-5 (display recurrence) | recurrence fields on session DTO; GET `/occurrences` |
| FR-6 (calendar feed correctness) | `getOccurrenceDates` (§5.4); FR-6.2 weekly unchanged |
| FR-7 (pattern travels through lifecycle) | KD-59 (columns on the session row that already travels draft→publish) |
| HC-FN-3 | §6 Constraint scope; enforced by A4-11 owners per occurring week |
| HC-FN-4 | `validate` (§5.3) + DB CHECK (§4) |
| SC-FN-1 | Deferred — OQ#5 (engine auto-assignment not in this story) |
| NFR audit / backward-compat / determinism / performance | §6 rows |

## 11. Testing Strategy

| Level | Scenario |
|---|---|
| Unit | `WeekParityResolver`: semester-start week → WEEK_A; +1 week → WEEK_B; +2 → WEEK_A; date mid-week resolves by its Monday. |
| Unit | `RecurrenceOverlapEvaluator`: WEEKLY×anything → true; FORTNIGHTLY opposite groups → false (HC-FN-1); FORTNIGHTLY same group → true (HC-FN-2). |
| Unit | `setRecurrence` validation: WEEKLY+weekGroup → 422; FORTNIGHTLY+null → 422; revert clears week_group (KD-64); day/slot unchanged. |
| Unit | `getOccurrenceDates`: fortnightly WEEK_A returns only WEEK_A occurrences; holiday date excluded (PD-82); weekly returns all working occurrences. |
| Integration (Testcontainers) | PUT then GET reflects persisted pattern; DB CHECK rejects invalid combo; 422 when no calendar anchor; 403 cross-department; existing weekly sessions unaffected after V12 migration. |
