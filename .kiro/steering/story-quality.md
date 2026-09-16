---
inclusion: always
---

# Story Quality Guardrails

These rules apply whenever Kiro creates, updates, or reviews User Stories. They prevent recurring anti-patterns identified during backlog analysis.

---

## Principle

A User Story describes **what the system does for the user** — never **how it is built internally**. Stories are scoped, testable, and traceable to the BRD. Implementation decisions belong in the Design Document.

---

## Rules

### R1: No Implementation Details in Stories

**Never include** in story scope or description:
- Database technology (Flyway, migrations, indexes, JSONB, partitioning)
- Code structure (entities, repositories, mappers, DTOs, service layer, controller layer)
- Specific libraries or frameworks (MapStruct, SendGrid, Twilio, Redis, Bull queue)
- Internal data formats (composite indexes, GIN indexes, FK naming)

**Instead:** Describe the behavior. "The system stores room equipment tags and supports tag-based search" — not "GIN index on equipment_tags JSONB column."

These details belong in the **Design Document**, not the story.

---

### R2: No Algorithm Lock-In

**Never name a specific algorithm** as a requirement:
- Bad: "Implement AC-3 arc consistency", "Use MRV/LCV heuristics", "Hill-climbing with random restarts"
- Good: "Generate a valid timetable satisfying all hard constraints within 2 minutes", "Improve solution quality by optimizing soft constraints"

The BRD describes **outcomes** (conflict-free timetable, < 2 min, quality score). The algorithm choice is a **design decision** made during Design Derivation.

---

### R3: No Gold-Plating

For every feature in a story, ask: **"Does the BRD explicitly require this?"**

- If YES → include it.
- If NO but logically necessary → include it, mark as "implied by [BRD section]."
- If NO and not necessary → remove it or mark as "stretch / Phase 2."

Examples of gold-plating to avoid:
- MUS (Minimal Unsatisfiable Subset) identification — BRD says "handle infeasibility", not "prove why"
- Conflict-directed backtracking — BRD says "generate", not "generate using advanced CS techniques"
- Offline PWA — BRD says "mobile-responsive", not "works without internet"
- Version vectors — BRD says "concurrent coordinators", not "CRDT-level consistency"

---

### R4: Match Infrastructure to Scale

Before adding infrastructure, cite the NFR that demands it:
- 50 concurrent coordinators does NOT require Redis caching, version vectors, or WebSocket pub/sub
- 10,000 students viewing timetables does NOT require offline-first PWA with service workers

**Rule:** If the stated performance target (BRD Section 8) can be met with simpler architecture, use simpler architecture. Justify any infrastructure additions with a specific NFR number and load calculation.

---

### R5: One Story = One Deliverable Demo

A story must be **demonstrable in 5 minutes** at sprint review.

**Size test:** If a story has more than 3 distinct functional areas, it's too big. Split it.

**The newspaper test:** Can you write a one-sentence headline for what this story delivers? If you need a paragraph, it's an epic compressed into a story.

Examples:
- Bad: "Implement constraint model, propagation, backtracking, worker isolation, orchestrator, API, and output" (that's 7 things)
- Good: "Generate a draft timetable for a department" (one thing, demonstrable)

---

### R6: Mandatory Acceptance Criteria

Every story MUST have acceptance criteria **before leaving creation**:
- Format: Given / When / Then
- Minimum: 3 criteria per story
- Maximum: 8 criteria per story
- Must cover: happy path, one error path, one edge case

**No story enters the backlog without acceptance criteria.** Zero-AC stories are rejected.

---

### R7: Frontend Pairing — Every Interaction Needs a Screen

For every backend capability, ask: **"What does the user see and do?"**

If the answer involves a screen that doesn't have its own story, create one. This applies to ALL interactions, not just CRUD:
- Timetable editor (coordinator's workspace) — needs its own story
- Approval review screen (HOD's view) — needs its own story
- Version diff viewer — needs its own story
- Configuration screens (weights, parameters) — need their own story

**Rule:** No backend story is complete without identifying which frontend story consumes it.

---

### R8: BRD Workflow Completeness Check

After breaking down the BRD into stories, run this checklist. Each item must either have a story or be explicitly deferred:

- [ ] Login / User management / Password reset
- [ ] Bulk data import (CSV/Excel for master data)
- [ ] Leave & substitution workflow
- [ ] Elective-group timetable generation
- [ ] Post-publication change management
- [ ] Error recovery / undo
- [ ] Data export (reports, CSV, PDF)

If any is in the BRD but not in the backlog, create the story or document the deferral.

---

### R9: No Invented Numbers

Never invent specific values that the BRD doesn't state:
- Weights (7, 5, 4, 6, 8, 9, 3)
- Thresholds (90%, 80%)
- Retry counts (3 retries)
- Time limits (30 seconds, 120 seconds — unless the BRD says so)
- Service providers (SendGrid, Twilio)

**Instead:** Write `[configurable, default TBD — confirm with stakeholder]` or cite the exact BRD line.

Exception: If the BRD states "< 2 minutes" — use that number. Only invent when the BRD is silent, and always mark as TBD.

---

### R10: No Ambiguous Bullets

Every scope bullet must pass this test: **"Can I write a testable acceptance criterion from this bullet alone?"**

- Bad: "Bulk operations" — bulk what? For whom? What's the input/output?
- Bad: "Priority-based delivery" — what priorities? Who sets them? What changes?
- Good: "Coordinator can import up to 500 courses via CSV upload with error report for invalid rows"

If a bullet can't produce a test, rewrite it or remove it.

---

### R11: Constraint Ownership

Every constraint, rule, or validation must be owned by **exactly one story**.

- If a constraint appears in multiple stories, designate one as the **owner** (defines and implements it) and others as **consumers** (reference it).
- The owning story is stated explicitly in the description: "This story OWNS the faculty-double-booking constraint. AID-186 and AID-187 consume it."

This prevents the "seam-blindness" pattern where everyone assumes someone else handles it.

---

### R12: Time Lifecycle Coverage

Every story involving a workflow must address three phases:

1. **Before:** What preconditions must be true? What setup is needed?
2. **During:** What happens in the normal flow?
3. **After:** What happens on completion? What side effects trigger? What cleanup occurs?

If a story only covers "during" — it's incomplete.

---

## Enforcement

Kiro must validate these rules before finalizing any story creation or update:
1. Scan for implementation keywords (R1) — reject if found in scope/description
2. Scan for algorithm names (R2) — reject if found as requirements
3. Verify acceptance criteria exist (R6) — reject if count < 3
4. Verify no unattributed numbers (R9) — flag any specific values without BRD citation
5. Check bullet testability (R10) — flag vague bullets
