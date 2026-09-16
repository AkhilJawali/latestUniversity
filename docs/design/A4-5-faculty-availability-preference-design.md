# Design: Faculty Availability and Preference Management

**Jira Reference:** A4-5
**Source Requirements:** docs/requirements/A4-5-faculty-availability-preference-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes: soft-preference read contract, PD labeling, designation-change model migration)

## 1. Overview

This design implements faculty availability and preference management — CRUD for hard unavailability windows, soft time preferences, and the part-time/visiting "declare available" model. Hard windows are inviolable constraints consumed by the scheduling engine and conflict detection. Soft preferences are optimization hints consumed by the engine's soft-constraint optimizer. The design explicitly addresses: (a) the read contract for soft preferences (who reads them, how), (b) the designation-change model transition, and (c) conflict surfacing when availability changes post-scheduling.

## 2. Architecture

```
Controller Layer
    └── FacultyAvailabilityController

Service Layer
    ├── FacultyAvailabilityService (hard window CRUD, conflict surfacing)
    ├── FacultyPreferenceService (soft preference CRUD)
    └── AvailabilityQueryService (read contract for A4-11, A4-16, A4-30)

Event Consumption
    └── FacultyDesignationChangedEventListener (from A4-4 KD-15)

Audit
    └── AuditEventPublisher (A4-2 contract)

Repository Layer
    ├── FacultyAvailabilityWindowRepository
    └── FacultyPreferenceRepository

Database (schema: utms)
    ├── faculty_availability_windows
    └── faculty_preferences
```

## Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-18 | AvailabilityQueryService provides the read contract: getHardBlockedSlots(), getSoftPreferences(), isAvailable(), getMode() | Without explicit read service, soft preferences stored but never consumed. This IS the consumer API. | A4-11 calls getSoftPreferences(). A4-16 calls isAvailable(). A4-30 calls isAvailable(). |
| KD-19 | Model determined by designation: Visiting/Adjunct = "declare available"; others = "declare blocked". No explicit toggle. | Simplest. Designation IS the toggle. | Interacts with KD-20. |
| KD-20 | Designation change across models → flag windows as REVIEW_REQUIRED. No auto-delete/migrate. | Auto-deletion destructive. Flagging + admin review safest. | Consumes A4-4 FacultyDesignationChangedEvent. |
| KD-21 | Both models use same table. Interpretation inverted by AvailabilityQueryService based on mode. | Single schema. BLOCKED mode: available if NOT in window. AVAILABLE mode: available ONLY if IN window. | — |
| KD-22 | Conflict surfacing via persisted event (AvailabilityConflictDetectedEvent), not transient HTTP warning. | Transient warnings lost. Conflicts must appear in conflict log. | A4-16 consumes to create conflict records. |

## Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-25 | Admin can manage availability on behalf | Req OQ #1 | Allow proxy | Operational need for bulk setup. |
| PD-26 | Times not required to align with grid | Req OQ #2 | Arbitrary times | Faculty constraints don't always align. |
| PD-27 | Reason: predefined list + free-text | Req OQ #3 | Both | Reporting + flexibility. |
| PD-28 | Date exceptions: Phase 2 only | Req OQ #4 | Weekly only | Complexity. Leave handles dates (A4-30). |
| PD-29 | Model by designation (no toggle) | Req OQ #5 | Designation | KD-19. |
| PD-30 | Preferences: categorical only | Req OQ #6 | Categorical | Numeric needs comparison framework. Phase 2. |

## 3. API Design

| Method | Path | Description | Auth | Traces To |
|--------|------|-------------|------|-----------|
| POST | `/api/v1/faculty/{id}/availability` | Create window | SELF or ADMIN | FR-1.1 |
| GET | `/api/v1/faculty/{id}/availability` | List windows | SELF or ADMIN | FR-1.3 |
| PUT | `/api/v1/faculty/{id}/availability/{windowId}` | Update window | SELF or ADMIN | FR-1.4 |
| DELETE | `/api/v1/faculty/{id}/availability/{windowId}` | Delete window | SELF or ADMIN | FR-1.5 |
| GET | `/api/v1/faculty/{id}/preferences` | Get preferences | SELF or ADMIN | FR-2.1 |
| PUT | `/api/v1/faculty/{id}/preferences` | Set preferences | SELF or ADMIN | FR-2.3 |

**Internal contract (service-to-service, not HTTP):**
```java
public interface AvailabilityQueryService {
    List<TimeRange> getHardBlockedSlots(Long facultyId, DayOfWeek day);
    FacultyPreferences getSoftPreferences(Long facultyId);
    boolean isAvailable(Long facultyId, DayOfWeek day, LocalTime start, LocalTime end);
    AvailabilityMode getMode(Long facultyId);
}
```

## 4. Data Model — V4__create_faculty_availability_tables.sql

```sql
CREATE TABLE utms.faculty_availability_windows (
    id BIGSERIAL PRIMARY KEY, faculty_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time TIME NOT NULL, end_time TIME NOT NULL,
    reason_code VARCHAR(50) NOT NULL, reason_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL, updated_by VARCHAR(100) NOT NULL, deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_faw_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT chk_faw_time_order CHECK (start_time < end_time)
);
CREATE INDEX idx_faw_faculty_day ON utms.faculty_availability_windows(faculty_id, day_of_week) WHERE deleted_at IS NULL;

CREATE TABLE utms.faculty_preferences (
    id BIGSERIAL PRIMARY KEY, faculty_id BIGINT NOT NULL UNIQUE,
    preferred_time_of_day VARCHAR(20) CHECK (preferred_time_of_day IN ('MORNING','AFTERNOON','NO_PREFERENCE')),
    session_distribution VARCHAR(20) CHECK (session_distribution IN ('CONSECUTIVE','SPREAD','NO_PREFERENCE')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL, updated_by VARCHAR(100) NOT NULL,
    CONSTRAINT fk_fp_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id)
);
```

## 5. Service Logic

**5.1 createWindow:** validate → persist → check session overlap → emit conflict event if found → audit
**5.2 updateWindow:** validate → persist → if expanded, check overlap → emit → audit
**5.3 deleteWindow:** soft-delete → no auto-reschedule (FR-4.2) → audit
**5.4 setPreferences:** upsert (one record per faculty) → audit
**5.5 AvailabilityQueryService:** full implementation with mode-based logic (BLOCKED vs AVAILABLE inversion)
**5.6 DesignationChangedListener:** if model changes → flag windows REVIEW_REQUIRED
**5.7 Conflict surfacing:** TODO query sessions (pending A4-11); emit AvailabilityConflictDetectedEvent

## 6–7. Cross-cutting + NFR

Errors: 404/400/403. Security: SELF or ADMIN. Audit: AuditEventPublisher. Performance: index on (faculty_id, day_of_week); isAvailable() called per-slot during scheduling.

## 8. Testing

Unit: window CRUD + conflict events, preference upsert, isAvailable (both modes), designation change listener
Integration: POST/GET/PUT/DELETE windows, preference upsert + retrieval, designation change → flag

## 9. Traceability

All FRs (1.1-1.7, 2.1-2.4, 3.1-3.3, 4.1-4.2), all constraints (HC-AVL-1-3, SC-AVL-1-2) mapped with explicit read contract for SC-AVL-1/2 via getSoftPreferences().

## 10. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| 1 | Window model mismatch resolution (flag vs delete vs migrate) | Stakeholder | Resolved as flag (KD-20) — confirm |
| 2 | Numeric weights migration path for Phase 2 | System Design | Deferred |

## 11. Provisional Decisions

PD-25 through PD-30 (see table above).
