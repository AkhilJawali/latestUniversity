# Requirement Document Gaps Tracker

Tracks all gaps found during external AI verification of requirement documents. Each story's verification flows as: initial review → gaps found → fixes applied → re-verification → additional gaps (if any) → fixes → clean.

---

## A4-11: Timetable Generation Engine

### Requirement Document Created
- File: `docs/requirements/A4-11-timetable-generation-engine-requirements.md`
- Created: 2026-08-26
- Contains: 7 FRs, 10 hard constraints (initial), 6 soft constraints, 7 open questions

---

### First Verification (External AI vs BRD)

**Issues Found (5):**

1. **Faculty workload bounds missing as engine hard constraints**
   - BRD Source: 6.10 ("accreditation norms"), 7.4 ("max consecutive teaching hours")
   - Severity: High
   - Problem: Engine can produce a "zero hard violations" draft that exceeds max daily/weekly load or max consecutive hours — immediately flagged by A4-16 and compliance.

2. **Cross-listed course synchronization impossible with one-dept generation**
   - BRD Source: 7.1 ("Cross-listed courses requiring synchronized slots")
   - Severity: High
   - Problem: Assumption 1 (one dept at a time) makes cross-listed synchronization unsolvable. No FR or OQ addresses the contradiction.

3. **Validation table says "available+competent" but competency is a warning (A4-4 PD-24), not a hard block**
   - BRD Source: A4-4 PD-24
   - Severity: Low
   - Problem: Internal inconsistency — validation implies hard enforcement of competency, but the owning story's decision says it's just a warning.

4. **FR-3.1 "L hours → L sessions" formula is invented**
   - BRD Source: 7.1 ("determines number and length"), 7.7 ("mixed slot durations")
   - Severity: Medium
   - Problem: With 90-min slots, 3 lecture hours ≠ 3 sessions. The 1:1 mapping was invented; BRD doesn't specify the formula.

5. **Score representation inconsistent (FR-6.2/6.3 say "0-100%", validation table says "0.0 to 1.0")**
   - BRD Source: Internal inconsistency
   - Severity: Low
   - Problem: Same field described differently in two sections.

**Fixes Applied:**
- Added FR-4.11 (max daily/weekly load) and FR-4.12 (max consecutive hours) to Section 5
- Added OQ#8: How are cross-listed courses synchronized across independently generated department timetables?
- Removed "competent" from validation table, replaced with "within load limits"
- Added OQ#9: How does L-T-P map to sessions with mixed slot durations?
- Aligned FR-6.2/6.3 to 0.0-1.0 (matches validation table)

---

### Second Verification (External AI re-check after fixes)

**Issues Found (3) — all propagation failures:**

6. **Section 6 constraint table still ended at HC-ENG-10**
   - Problem: FR-4.11 and FR-4.12 were added to Section 5, but Section 6 (Constraints Owned table) was not updated.
   - Root Cause: Fixed the primary location but didn't propagate to dependent sections.

7. **Consistency note now false — said "10 hard constraints" but it's 12**
   - Problem: The delta explanation ("10 vs 9 in A4-16") was stale after adding HC-ENG-11/12.
   - Root Cause: Same propagation failure.

8. **Traceability table missing rows for new FRs**
   - Problem: BRD 6.10 → FR-4.11 and BRD 7.4 → FR-4.12 had no traceability entries.
   - Root Cause: Same propagation failure.

**Additional issues from same review:**
- FR-3.1/FR-3.3 still stated the 1:1 formula as "shall" while OQ#9 questions it — needed [subject to OQ#9] tags
- Score scale in FR-6.2/6.3 still said "0-100%" (the fix hadn't been applied to the full-length version of those FRs)

**Fixes Applied:**
- Added HC-ENG-11 + HC-ENG-12 rows to Section 6 constraint table
- Rewrote consistency note: "12 hard constraints (HC-ENG-1 through HC-ENG-12). A4-16 detects 9 types. Delta: HC-ENG-8/9/10 are structural exclusions."
- Added traceability rows: 6.10 → FR-4.11, 7.4 → FR-4.12
- Tagged FR-3.1 and FR-3.3 with [subject to OQ#9]
- Fixed FR-6.2/6.3 to say "0.0 to 1.0"

---

### Current State: v3 — Clean (no open gaps)

- Total gaps found: 8
- Total gaps fixed: 8
- Open gaps: 0
- Document version on Jira (A4-52): v3 (attachment ID 10627)

---

### Lessons Learned (fed back into P5 steering)

| Failure Mode | P5 Amendment Added |
|---|---|
| Engine hard constraints missing because they "felt like" another story's territory | Step 1: Constraint Inventory for process stories |
| Assumption contradicts BRD requirement without flagging | Step 3: Assumption Stress-Test |
| Used data without checking owning story's PD (warning vs block) | Step 4: Read owning story's PDs/KDs |
| Invented formula where BRD only says "determines" | Step 5: Rule/Formula Audit (renamed "Number and Logic Audit") |
| Fixed one section, didn't propagate to others | Step 6: Propagation Check |

---

---

## A4-11: Timetable Generation Engine — Design Document

### Design Document Created
- File: `docs/design/A4-11-timetable-generation-engine-design.md`
- Created: 2026-08-25
- Contains: 8 KDs (45-52), 8 PDs (67-74), 5 API endpoints, V10 migration (7 tables), full solver logic

---

### First Verification (External AI vs Requirements + BRD)

**Issues Found (10):**

#### High Severity

1. **Session duration never matched to slot duration**
   - Source: BRD 7.7 (mixed 60/90/180 min grid), PD-74 (has slot_duration_minutes in rules table)
   - Problem: SessionVariable has no duration attribute. Domain builder doesn't filter by duration fit. Solver places 60-min lecture into 180-min lab slot. HC-ENG-9 covers "teaching slots only" not duration fit.
   - Fix: Add `requiredDurationMinutes` to SessionVariable. Domain filter: only include slots where slot.durationMinutes == session.requiredDurationMinutes.

2. **Workload limits (FR-4.11/4.12) have no data source**
   - Source: A4-4 (only stores min/max weekly load), A4-32 (cadre norms — absent from integration contracts)
   - Problem: wouldExceedDailyLoad()/wouldExceedConsecutive() methods exist but have no data to check against. No daily field in A4-4. Consecutive hours limit lives in A4-32 cadre norms. Also unstated: does load count hours or sessions?
   - Fix: Add A4-32 to integration contracts. Add maxDailyHours, maxConsecutiveHours to SchedulingInput (loaded from cadre norms with per-faculty override from A4-4 PD-19). Clarify load = hours (sum of slot durations), not session count.

#### Medium Severity

3. **Feasibility score unreachable between 0 and 1**
   - Problem: Backtracking is all-or-nothing (restore unwinds everything). assignedCount = 0 or total. FR-6.2 implies partial results.
   - Fix: Maintain best-partial checkpoint during solving. On timeout/failure, return checkpoint. (Also prepares for A4-12 best-partial-solution.)

4. **FR-1.2(c) working-day-pattern precondition not checked**
   - Problem: validatePreconditions() checks calendar/grid/rules/courses/rooms but NOT the pattern. CalendarQueryService throws 422 mid-generation if pattern missing. Also: contract row says A4-9 throws without pattern, but A4-9 design silently defaults to 5-day — contradiction.
   - Fix: Add pattern check to preconditions. Clarify CalendarQueryService contract: it DOES throw (KD-39 in actual code).

5. **CallerRunsPolicy defeats KD-46**
   - Problem: When queue (10) is full, rejected generation runs on Tomcat web thread — a 2-minute computation, exactly what the dedicated pool prevents.
   - Fix: Change to AbortPolicy + catch RejectedExecutionException → return 503 "Service busy, try later."

6. **@Transactional on private storeResults won't work**
   - Problem: Private method self-invoked from executeGeneration. Spring proxy bypassed = no transaction. Audit events "within same transaction" claim is false.
   - Fix: Extract storeResults into a separate @Service bean (SchedulingResultPersister) with @Transactional method.

7. **institution_common_slots is an orphaned data source**
   - Problem: KD-52 invents the table, but no story owns CRUD, no admin endpoint, "Until A4-XX defines it" names no story.
   - Fix: Add explicit note: "Common slots managed by Registrar via a future story (OQ-D3). Until then, seeded in migration for dev/demo. Production requires admin endpoint before generation works with common slots."

#### Low Severity

8. **recordSoftBlockOverride passes null sessionId**
   - Problem: Sessions are persisted BEFORE the call, so session IDs exist. Override loses its session linkage.
   - Fix: Pass the actual persisted session.getId() by iterating sessions after saveAll().

9. **Determinism sort uses variables.indexOf(v) — O(n²) and undefined**
   - Problem: indexOf on the list being sorted = undefined behavior. Also KD-50 "same input = same output" conflicts with PD-69's minute-varying default seed.
   - Fix: Replace with a stable sequence counter assigned during creation. Clarify KD-50 applies only with explicit seed; PD-69 default seed intentionally varies.

10. **Security: no department scoping check on trigger**
    - Problem: triggerGeneration never verifies coordinator belongs to request.departmentId. Any coordinator can generate for any department.
    - Fix: Add departmentId ownership check in controller/service using SecurityContext + RLS department claim.

**Fixes Applied:** Applying to design doc v2 (see below)

---

## A4-12: Timeout and Infeasibility Handling

### Requirement Document Created
- File: `docs/requirements/A4-12-timeout-infeasibility-handling-requirements.md`
- Created: 2026-08-27
- Contains: 7 FR groups, 8 acceptance criteria, 3 system constraints owned, 7 open questions

---

### First Verification (External Review)

**Issues Found (3) — all consistency-level:**

1. **FAILED state unaccounted for in state model**
   - Source: A4-11's GenerationStatus enum (already has FAILED + CANCELLED)
   - Severity: Medium
   - Problem: FR-5.1 claimed TIMED_OUT/INFEASIBLE/CANCELLED as new "beyond COMPLETED" — but CANCELLED already exists in A4-11, and FAILED (unexpected error) appeared nowhere in the state model, journeys, or audit scope (FR-7.1 only named 3 of 4 terminal outcomes).

2. **FR-1.3 clock-start contradicts A4-11's design**
   - Source: A4-11 design starts timer before data loading; this doc stated "solving phase only" as fact
   - Severity: Medium
   - Problem: Quietly relaxed the BRD's "within 2 minutes" without flagging it as a decision or OQ. Contradicts the owning story's design.

3. **FR-4.2 "sessions placed so far" ill-defined mid-search**
   - Source: Backtracking solver behavior (count rises and falls during unwinding)
   - Severity: Low
   - Problem: During backtracking, the current assignment count is non-monotonic. Reporting it produces a confusing progress bar that goes backwards. The meaningful metric is the best-so-far checkpoint count (monotonically non-decreasing).

**Fixes Applied:**
- FR-5.1: Rewritten to acknowledge COMPLETED/FAILED/CANCELLED as pre-existing from A4-11; only TIMED_OUT + INFEASIBLE are new
- Added FR-5.5: Defines FAILED for completeness (already in A4-11, not redefined here)
- FR-7.1: Audit scope now lists all four non-COMPLETED states (TIMED_OUT, INFEASIBLE, CANCELLED, FAILED)
- Data model: Status enum lists all 6 states with ownership annotation
- FR-1.3: Removed assertion; converted to reference OQ#7
- Added OQ#7: When does the 2-minute clock start? (with A4-11 contradiction noted)
- Assumption #4: Updated to reflect unresolved OQ
- FR-4.2: Changed to "best-so-far session count (monotonically non-decreasing, from the checkpoint)" with explanation
- Propagated "best-so-far" to: validation table (field renamed bestSoFarCount), AC#5, Journey 1 step 1, Journey 3 step 2

---

### Current State: v2 — Clean (no open gaps)

- Total gaps found: 3
- Total gaps fixed: 3
- Open gaps: 0
- Document version on Jira (A4-56): v1 (attachment ID 10661) — needs re-upload with fixes

---

### Lessons Learned

| Failure Mode | Root Cause |
|---|---|
| Missed FAILED state from A4-11's enum | Didn't inventory owning story's full state model (P5 Step 4 insufficient depth) |
| Stated clock-start as fact when it contradicts A4-11 | Invented a decision without cross-checking design doc (P5 Step 5 violation) |
| Imprecise progress metric during backtracking | Didn't think about runtime behavior of the metric (algorithm awareness gap) |

---

## A4-13: Fortnightly and Alternate-Week Patterns

### Requirement Document Created
- Not yet created

---

## A4-14: Locked Slot Preservation and Partial Re-Generation

### Requirement Document Created
- Not yet created

---

## A4-16: Real-Time Conflict Detection Engine

### Requirement Document Created
- Not yet created

---

## Summary

| Story | Gaps Found | Fixed | Open | Doc Version |
|---|---|---|---|---|
| A4-11 | 8 | 8 | 0 | v3 |
| A4-12 | 3 | 3 | 0 | v2 |
| A4-13 | — | — | — | Not created |
| A4-14 | — | — | — | Not created |
| A4-16 | — | — | — | Not created |
