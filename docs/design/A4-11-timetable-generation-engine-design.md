# Design: Timetable Generation Engine

**Jira Reference:** A4-11
**Source Requirements:** docs/requirements/A4-11-timetable-generation-engine-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes 10 review items: duration matching, workload data source, partial feasibility, pattern precondition, AbortPolicy, transactional bean, common-slot ownership, sessionId linkage, sort stability, dept scoping)

## 1. Overview

This design implements the core timetable generation engine — a constraint satisfaction problem (CSP) solver that auto-generates a first-draft weekly timetable for a department. The engine loads master data from existing services (A4-2 through A4-10), derives sessions from L-T-P splits (with duration matching to the campus grid), enforces 12 hard constraints, optimizes 6 soft constraints, and produces a scored draft within a 2-minute time bound.

The engine runs **asynchronously** in a dedicated thread pool (PD-67). Coordinators trigger generation and poll for status. Output is a `TimetableDraft` with placed sessions, feasibility/quality scores, and a violations list. Partial results are preserved via best-so-far checkpointing (preparing for A4-12 timeout handling).

**Not in scope:** Timeout/infeasibility reporting (A4-12), fortnightly patterns (A4-13), locked slots/partial re-gen (A4-14), drag-drop editing (A4-15), real-time conflict detection (A4-16).

## 2. Architecture

```
Controller Layer
    └── SchedulingController
         │  POST /generate (trigger — with dept ownership check)
         │  GET /generate/{id}/status (poll)
         │  GET /timetables/{id} (view draft)
         │
Service Layer
    ├── SchedulingEngineService (orchestrator: validate → spawn async)
    ├── SchedulingDataLoader (loads all input from master data + cadre norms)
    ├── SessionDeriver (L-T-P → sessions with duration attribute)
    ├── ConstraintSolver (CSP: propagation + backtracking + optimization)
    │   ├── HardConstraintValidator (12 constraints, zero-tolerance)
    │   └── SoftConstraintOptimizer (6 constraints, weighted scoring)
    ├── ScoreComputer (feasibility + quality score calculation)
    └── SchedulingResultPersister (separate @Transactional bean for storage)
         │
Event Publishing
    ├── AuditEventPublisher (same contract as A4-2)
    └── GenerationCompletedEvent (consumed by A4-18 approval module when built)
         │
Repository Layer
    ├── GenerationRequestRepository
    ├── TimetableDraftRepository
    ├── ScheduledSessionRepository
    └── SoftConstraintViolationRepository
         │
Master Data Read Contracts (consumed, not owned)
    ├── CourseRepository (L-T-P, equipment tags, type)
    ├── FacultyService (profiles, min/max weekly load)
    ├── AvailabilityQueryService (hard blocks, soft preferences)
    ├── RoomService (capacity, equipment, campus)
    ├── CalendarQueryService (isWorkingDay — throws if no pattern)
    ├── TimeSlotGridService (getEffectiveSlotsForDay — with durations)
    ├── BlockService (active blocks, recordSoftBlockOverride)
    ├── BatchRepository (strength, program, section)
    ├── SessionDerivationRuleRepository (L-T-P mapping + duration config)
    ├── ComplianceNormService [A4-32] (maxDailyHours, maxConsecutiveHours per cadre)
    └── WorkingDayPatternRepository (pattern existence check for preconditions)
         │
Database (schema: utms)
    ├── generation_requests
    ├── timetable_drafts
    ├── scheduled_sessions
    ├── soft_constraint_violations
    ├── session_derivation_rules
    ├── soft_constraint_weights
    └── institution_common_slots
```

## Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-45 | CSP approach: AC-3 for domain pruning → Backtracking with MRV/LCV + best-partial checkpoint → Hill-climbing for soft optimization. Three distinct phases. | Industry-standard for university timetabling. Best-partial checkpoint enables partial feasibility scores and prepares for A4-12. | Performance: must complete within 2 min (FR-7.1). |
| KD-46 | Dedicated thread pool (size=2) with AbortPolicy when full → 503. NOT CallerRunsPolicy. | CallerRunsPolicy would run on web thread, defeating isolation. AbortPolicy rejects cleanly. | Pool size 2 allows 2 departments simultaneously. Overflow returns 503. |
| KD-47 | Session = one atomic placement unit with a **required duration** (minutes). Placed WHERE slot.durationMinutes == session.requiredDurationMinutes. | Duration matching is critical on mixed grids (BRD 7.7: 60/90/180). Without it, a 60-min lecture lands in a 180-min lab slot. | Domain builder filters by duration. |
| KD-48 | BitSet-based occupancy maps for O(1) conflict detection. | Must be fast for backtracking (millions of checks). | Memory trivial (~30K bits). |
| KD-49 | Quality score = weighted average of soft constraint satisfaction ratios. | Normalized, interpretable, comparable. | PD-70: weights configurable. |
| KD-50 | Determinism applies ONLY with explicit seed. Sorting uses stable sequence counters (not indexOf). | indexOf was O(n^2) and undefined during sort. Default seed varies intentionally. | PD-69: default seed varies per minute. |
| KD-51 | Soft-block override recorded with ACTUAL session ID (after persistence). | A4-8's table stores sessionId — null loses linkage. | Within SchedulingResultPersister's transaction. |
| KD-52 | Common slots (CCC/UWE) pre-placed as immovable before solving. | FR-4.10: pre-placed hard constraints. | CRUD owned by future story (OQ-D3). |
| KD-53 | Workload load counts **hours** (sum of slot durations), not session count. | Mixed durations make session-count meaningless. 3x60 + 1x180 = 6h, not 4 sessions. | Data from A4-32 + A4-4. |
| KD-54 | Result persistence in separate `SchedulingResultPersister` bean with @Transactional. | Private @Transactional bypassed by Spring proxy on self-invocation. | Audit events now correctly within transaction. |

## Provisional Decisions

| # | Decision | Resolves | Default | Rationale |
|---|---|---|---|---|
| PD-67 | Async with polling (202 + status). | OQ#1 | Async | 2-min makes sync impractical. |
| PD-68 | Reject concurrent per dept+semester. | OQ#2 | Reject | Waste to duplicate. |
| PD-69 | Default seed varies per minute. Explicit seed = deterministic. | OQ#3 | Varying | Production explores; tests pin. |
| PD-70 | Weights in DB config table. Default equal. | OQ#4 | Equal | Institutions differ. |
| PD-71 | Pre-assigned faculty only. | OQ#5 | Pre-assigned | Auto-assign is separate problem. |
| PD-72 | New version, supersede old. | OQ#6 | Version | History for A4-19. |
| PD-73 | Travel-time deferred to A4-35. | OQ#7 | Deferred | Needs distance matrix. |
| PD-74 | Derivation rules per campus (includes slot_duration_minutes). | OQ#9 | Configurable | BRD 7.7: mixed durations. |

## 3. API Design

| Method | Path | Description | Auth | Traces To |
|---|---|---|---|---|
| POST | `/api/v1/timetables/generate` | Trigger | COORDINATOR (own dept) / HOD / REGISTRAR | FR-1.1 |
| GET | `/api/v1/timetables/generate/{requestId}/status` | Poll | COORDINATOR+ | FR-1.4 |
| GET | `/api/v1/timetables/{draftId}` | Draft with scores | COORDINATOR+ | FR-6.1-6.4 |
| GET | `/api/v1/timetables/{draftId}/sessions` | Sessions (paginated) | COORDINATOR+ | FR-6.1 |
| GET | `/api/v1/timetables/{draftId}/violations` | Soft violations | COORDINATOR+ | FR-6.4 |

**Security:** Coordinator restricted to own department. HOD/Registrar can access any.

**503 response** when queue full (AbortPolicy):
```json
{"timestamp":"...","status":503,"error":"Service Unavailable","message":"Scheduling engine queue is full. Try again later.","path":"/api/v1/timetables/generate"}
```

**422 response** includes WORKING_DAY_PATTERN check:
```json
{"details":[{"check":"WORKING_DAY_PATTERN","message":"No working-day pattern configured for campus 1"}]}
```

## 4. Data Model

Migration V10 — same 7 tables as v1. Duration matching handled at application level (slot_definitions already have start_time/end_time from which durationMinutes is computed).

## 5. Service Logic (Key Changes from v1)

### 5.1 Department Scoping (Fix #10)
Controller checks `user.getDepartmentId().equals(request.getDepartmentId())` for COORDINATOR role.

### 5.2 Preconditions (Fix #4)
Added check (c): `workingDayPatternExists(campusId)` — prevents CalendarQueryService 422 mid-generation.

### 5.3 Session Derivation (Fix #1, Fix #9)
- SessionVariable now carries `requiredDurationMinutes` from derivation rule's `slot_duration_minutes`
- Sort uses `sequenceIndex` (AtomicInteger counter) instead of `indexOf`

### 5.4 Domain Builder (Fix #1)
Filters: `if (slot.getDurationMinutes() != var.getRequiredDurationMinutes()) continue;`

### 5.5 Workload (Fix #2)
- Data from `ComplianceNormService.getLimitsForDesignation(designation)` → maxDailyHours, maxWeeklyHours, maxConsecutiveHours
- Per-faculty override from A4-4 (PD-19: null = inherit from cadre)
- KD-53: load = hours (sum of slot durations), not session count

### 5.6 Backtracking (Fix #3)
Maintains `bestPartialState` checkpoint. On timeout/failure, returns partial. Feasibility = bestPartialCount / total.

### 5.7 Result Persistence (Fix #6, Fix #8)
`SchedulingResultPersister` (separate @Service, @Transactional). Override recording uses actual `persistedSessions.get(i).getId()`.

### 5.8 Thread Pool (Fix #5)
AbortPolicy. Controller catches `RejectedExecutionException` → 503.

## 6. Cross-Cutting Concerns

| Concern | Design |
|---|---|
| Audit | Within SchedulingResultPersister @Transactional (KD-54). |
| Security | Dept scoping on trigger (Fix #10). RBAC on all endpoints. |
| Concurrency | Partial unique index + AbortPolicy (never runs on web thread). |
| Error handling | 422/409/503/500. Same GlobalExceptionHandler envelope. |

## 7. Integration Contracts

| Service | Method | Contract | Owner |
|---|---|---|---|
| CalendarQueryService | isWorkingDay(campusId, date) | Throws 422 if no pattern (KD-39). | A4-9 |
| TimeSlotGridService | getEffectiveSlotsForDay(gridId, day) | Slots with durationMinutes. | A4-10 |
| AvailabilityQueryService | getHardBlockedSlots / getSoftPreferences | TimeRange / Preferences. | A4-5 |
| BlockService | recordSoftBlockOverride(blockId, justification, sessionId) | Non-null sessionId required. ACTIVE SOFT only. | A4-8 |
| FacultyService | findById(id) | min/maxWeeklyLoad. | A4-4 |
| **ComplianceNormService** | **getLimitsForDesignation(designation)** | **maxDailyHours, maxWeeklyHours, maxConsecutiveHours.** | **A4-32** |
| WorkingDayPatternRepository | existsByCampusIdAndDeletedAtIsNull | Boolean for precondition. | A4-9 |
| RoomService | findAll(campusId, ...) | Capacity, equipment, building. | A4-6 |
| CourseRepository | findByDepartmentIdAndDeletedAtIsNull | L-T-P, equipment. | A4-3 |
| BatchRepository | findByProgram...AndDeletedAtIsNull | Strength. | A4-2 |

## 8. Open Questions

| # | Question | Owner | Status |
|---|---|---|---|
| OQ#8 | Cross-listed sync. | Academic Affairs | CARRIED FORWARD |
| OQ-D1 | GenerationCompletedEvent sufficient for A4-18? | System Design | Provisional: yes |
| OQ-D2 | Seed derivation rules or require admin config? | System Design | Require config; seed for dev |
| OQ-D3 | Who owns institution_common_slots CRUD? Needs story. | Product Owner | NEW |

## 9. Consistency Notes

- V10 follows V9. PD-67–74. KD-45–54 (added KD-53, KD-54).
- CalendarQueryService THROWS (KD-39 confirmed in code). Precondition prevents.
- Workload: A4-32 cadre defaults + A4-4 PD-19 per-faculty override.
- Score: DECIMAL(4,3). Feasibility achievable 0–1 via best-partial.
- institution_common_slots: seeded for dev, requires admin story for production (OQ-D3).
