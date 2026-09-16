---
inclusion: always
---

# Requirement Document Quality Guardrails

These rules apply whenever Kiro creates, updates, or reviews Requirement Documents. They prevent recurring anti-patterns identified during document analysis.

---

## Principle

A Requirement Document describes **what the system must do** (observable behaviors, constraints, boundaries) — never **how it should be built** (algorithms, data structures, architecture). It is the contract between the business and the development team, written for both technical and non-technical reviewers.

---

## Rules

### R1: Organize by User Behavior, Not by Algorithm

**Structure FRs around what the user experiences or what the system observes**, not around internal mechanisms.

- Bad FR sections: "FR-2: Constraint Propagation (AC-3)", "FR-3: Backtracking Search", "FR-4: Worker Thread"
- Good FR sections: "FR-2: Timetable Generation", "FR-3: Infeasibility Handling", "FR-4: Partial Re-Generation"

**Test:** A non-technical HOD should be able to read every FR heading and understand what behavior it describes without knowing computer science.

---

### R2: User Journey First, Then FRs

Before writing any functional requirement, list the **user journeys** for this story:

1. What does the actor do step by step?
2. What does the system respond with at each step?
3. What can go wrong at each step?

Then write FRs that cover each journey step. If a journey step has no FR, the document is incomplete. If an FR belongs to no journey step, it may be gold-plating.

---

### R3: Lifecycle Coverage (Before / During / After)

Every requirement document must address:

- **Before:** What preconditions are required? What data must exist? What state must the system be in?
- **During:** What happens in the normal flow? What are the system's responses?
- **After:** What state changes? What events fire? What other modules are notified? What can the user do next?

If the document only covers "during" — it's incomplete. Add explicit FRs for setup validation and downstream effects.

---

### R4: Single Ownership for Every Constraint

Every constraint, validation rule, or business rule must be **owned by exactly one requirement document**.

- The owning document **defines** the constraint (full specification, edge cases, validation rules).
- Other documents that need the constraint **reference** it: "Per AID-184 HC-1, faculty single-assignment is enforced."
- **Never duplicate** a constraint definition across two documents. If SC-1 to SC-7 are defined in the engine doc, the optimization doc references them — it does not re-define them.

Include a section: "Constraints Owned by This Document" and "Constraints Referenced from Other Documents."

---

### R5: No Invented Specifics

Never invent numbers, thresholds, weights, or vendor names that the BRD does not state:

- Default weights (7, 5, 4, 6, 8, 9, 3) — invented
- "90% capacity threshold" — invented
- "3 retries with exponential backoff" — invented
- "SendGrid for email, Twilio for SMS" — invented

**Instead:** Write `[configurable, default TBD — confirm with stakeholder]` for any value the BRD doesn't specify.

**Exception:** Values explicitly stated in the BRD (e.g., "< 2 minutes", "50+ coordinators") may be used directly with a BRD citation.

---

### R6: Contradiction Check

After writing the document, perform an explicit contradiction check:

1. List all assumptions in one place.
2. For each assumption, verify no FR contradicts it.
3. For each pair of FRs that touch the same entity, verify they don't conflict.

Common contradictions to watch for:
- "Canonical week" model vs. date-specific exceptions (holidays)
- "Timeout returns partial solution" vs. "prove infeasibility" (these are mutually exclusive states)
- Shared budget vs. separate budget for sub-phases
- Include-list semantics vs. exclude-list semantics (e.g., `excludeBatchIds` vs. "re-generate only these batches")

Document contradictions found and their resolution in a "Consistency Notes" section.

---

### R7: Traceability Verification

For every row in the traceability table:

1. Read the BRD requirement sentence.
2. Read the FR it maps to.
3. Verify the FR **actually implements** the BRD requirement — not just that it's tangentially related.

**Red flags:**
- BRD requirement mapped to "Assumption" → this is a deferral, not traceability. Mark it as "DEFERRED — Phase 2" explicitly.
- BRD requirement mapped to a vaguely related FR → wrong mapping. Fix it or add the missing FR.
- BRD requirement with no mapping at all → gap. Either add an FR or explicitly defer.

**Rule:** Every BRD requirement in scope must map to at least one FR that directly fulfills it, OR be explicitly marked "DEFERRED — [reason]."

---

### R8: Depth Proportional to User Interaction

The level of detail in the requirement document must be proportional to how often a user interacts with that feature:

- A screen used daily (timetable editor) → more FRs, more acceptance criteria, more edge cases
- A config page used once per semester (constraint weights) → fewer FRs, basic CRUD coverage
- A background job (generation engine) → focus on inputs, outputs, error handling — not internal steps

**Anti-pattern:** 40 FRs for the engine internals, 0 FRs for the editor the coordinator uses every day. That's depth-inversion.

---

### R9: Mechanism Stress Testing

For every mechanism described in the document (timeout, retry, restart, swap, rollback):

1. Write an acceptance criterion for the **happy path**.
2. Write an acceptance criterion for the **boundary** (what happens at the limit?).
3. Write an acceptance criterion for **failure** (what happens when it breaks?).
4. Verify the mechanism is **physically possible** — does it violate its own constraints?

Example of a broken mechanism:
- "Random restart from a random valid permutation" — generating a random valid permutation while maintaining all hard constraints is itself computationally hard. The mechanism as described may be impossible.

If a mechanism can't be stress-tested, it's under-specified.

---

### R10: No Algorithm Prescription

The requirement document describes **what** the system must achieve, not **how** to achieve it:

- Bad: "The system shall implement AC-3 arc consistency for domain pruning"
- Good: "The system shall reduce the search space by eliminating assignments that would immediately violate hard constraints"

- Bad: "Use hill-climbing with random restarts (5 restarts, 1000 iterations each)"
- Good: "The system shall iteratively improve the quality score within a configurable time budget"

Algorithm choices, data structures, and implementation strategies belong in the **Design Document**.

**Test:** If removing the algorithm name makes the requirement meaningless, the requirement is written wrong — it's describing implementation, not behavior.

---

### R11: Complete Constraint Coverage

After drafting the constraint catalogue, walk through **every row** in BRD Section 7 (Scheduling Parameters):

| BRD Section 7 Sub-table | Check |
|---|---|
| 7.1 — Structural/Curriculum | Each parameter has a constraint or FR |
| 7.2 — Faculty Parameters | Each parameter has a constraint or FR |
| 7.3 — Room/Infrastructure | Each parameter has a constraint or FR |
| 7.4 — Time & Calendar | Each parameter has a constraint or FR |
| 7.5 — Student/Batch | Each parameter has a constraint or FR |
| 7.6 — Compliance | Each parameter has a constraint or FR |
| 7.7 — Institution-Specific | Each parameter has a constraint or FR |

For each row that has no corresponding constraint: either add it or mark it "DEFERRED — not in scope for this story."

Common gaps to watch for:
- Faculty hard unavailability (declared blocked windows — distinct from "soft preference")
- Min/max weekly load as a hard constraint (not just soft daily hours)
- Max consecutive teaching hours for faculty
- Multi-slot sessions (a 3-hour lab = one session spanning 3 periods, not 3 separate 1-hour sessions)
- Cross-listed course synchronization (same course, two departments, must be same time)
- Student-level clash (individual student, not just batch-level)

---

### R12: Cascading Effect Analysis

For every action the system takes (place a session, move a session, suggest alternative, approve, reject):

Ask: **"What second-order effects does this action have?"**

- Moving session A frees room R1 → does another session now want R1?
- Suggesting alternative slot X → does placing in X cause a NEW conflict?
- Approving a draft → what notifications fire? What calendar feeds update? What blocks are checked?

If the document doesn't address cascading effects, add an FR: "The system shall verify that any proposed change does not introduce new conflicts."

---

### R13: Scope Boundary Markers

The document must explicitly state what is **OUT of scope**:

- "This document does NOT cover the UI for [X] — see story AID-YYY."
- "This document does NOT define the approval workflow — see AID-188."
- "Alternate-week patterns are DEFERRED to Phase 2 per team decision on [date]."

Ambiguous boundaries lead to scope creep or gaps. Be explicit about where this document ends and the next one begins.

---

## Document Structure Template

Every requirement document should follow this structure:

```
1. Introduction (what this document covers, 2-3 sentences)
2. User Story (the story statement)
3. Actors (who interacts)
4. User Journeys (step-by-step flows — BEFORE writing FRs)
5. Functional Requirements (organized by behavior/journey, not by algorithm)
6. Constraints Owned (full definitions — single source of truth)
7. Constraints Referenced (from other documents — link only, no re-definition)
8. Validation Rules (input validation)
9. Non-Functional Requirements (performance, security, audit)
10. Acceptance Criteria (Given/When/Then — minimum 5)
11. Data Model (conceptual only — no column types or index strategies)
12. Dependencies (what must exist before this works)
13. Assumptions (explicit, numbered)
14. Consistency Notes (contradiction check results)
15. Out of Scope (explicit boundaries)
16. Open Questions (unresolved items needing stakeholder input)
17. Traceability (verified BRD mappings)
```

---

## Enforcement

Kiro must validate these rules before finalizing any requirement document:
1. Check FR section headings for algorithm names (R1, R10) — reject if FRs named after algorithms
2. Verify user journeys exist before FRs (R2) — warn if missing
3. Verify lifecycle coverage: before/during/after (R3) — warn if "before" or "after" missing
4. Check for duplicated constraint definitions (R4) — flag if same constraint defined in two docs
5. Scan for invented numbers without TBD markers (R5) — flag all unattributed specifics
6. Verify traceability table completeness (R7) — flag BRD requirements without FR mapping
7. Check constraint coverage against BRD Section 7 (R11) — flag missing rows
8. Verify out-of-scope section exists (R13) — warn if missing
