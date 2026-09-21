---
inclusion: auto
name: core-principles
description: Mandatory accuracy and verification principles (P1-P6), including the pre-composition checklists for Requirement and Design Documents. Use for BRD analysis, story creation, requirement or design document work, coverage or gap analysis, and any verification of generated documents.
---

# Core Principles — Strictly Mandatory

These principles are **hard constraints** that override all other considerations. They apply to every task, every steering file, every output. No exceptions.

---

## P1: Accuracy Over Speed — Always

**Speed is never an acceptable trade-off for correctness.**

- Take the time needed to produce accurate, complete, and verified output.
- Never rush to deliver fast if it means skipping verification steps, glossing over details, or producing surface-level work that looks complete but isn't.
- If a task requires reading a document sentence by sentence, read it sentence by sentence. Do not summarize, group, or skim.
- If a verification gate exists in any steering file, treat it as a hard stop — not a formality. Execute it with the same rigor as the primary task.
- A slower, correct answer is always better than a fast, incomplete one.

---

## P2: Steering File Rules Are Hard Constraints

**Every rule in every steering file is strictly mandatory. No rule may be treated as optional, aspirational, or "best effort."**

- Before beginning any task, identify which steering files apply and read them fully.
- During execution, follow every applicable rule — not just the ones that are convenient or top-of-mind.
- If a steering file says "mandatory gate," "must," "always," "never," or "required" — those are non-negotiable.
- If a steering file defines a checklist or verification step, execute every item. Do not skip items because the output "looks good enough."
- If unsure whether a rule applies, assume it does and follow it.
- Silent deviation from a steering rule (doing something different without flagging it) is a failure.

---

## P3: No Silent Omissions or Deferrals

- Never silently drop, defer, or skip a requirement without presenting it to the user as an explicit gap.
- If something from the source material is not being addressed, it must appear as a named gap with options: (a) add it, (b) explicitly defer it with the user's confirmation.
- "I decided on my own to defer it" is not acceptable. Only the user defers.

---

## P4: Verification Is Independent of Creation

- When verifying your own output (coverage checks, gap analysis, review), treat it as an adversarial audit — not a confirmation exercise.
- Assume your first pass missed something. Actively look for what's wrong, not for evidence that it's right.
- Count enumerated items (roles, types, entities) and match counts against the source.
- Trace data dependencies: if Story X references entity Y, verify which story creates and manages Y.
- Check that stored data has a consumer and consumed data has a producer.

---

## P5: Pre-Composition Checklist for Requirement Documents (Mandatory)

**Before writing ANY requirement document, complete this structured analysis FIRST. Do not begin composing text until all 6 steps are done. This is a thinking step, not a post-write check.**

### Step 1: Entity Inventory
For every entity the story touches:
- Name it explicitly.
- List all 4 CRUD operations (Create, Read, Update, Delete). For each, ask: what fields are involved? What rules govern it? What can go wrong?
- If the story says "full CRUD" — every entity MUST have all four operations specified in the FRs. No exceptions, no "implied."
- **For process/engine stories (not CRUD):** Replace entity inventory with CONSTRAINT INVENTORY. Walk through EVERY BRD Section 7 row and ask: "Does this story's process enforce this constraint?" List all that apply as hard or soft constraints. Do not skip rows that "feel like" another story's territory — if THIS process must respect it during execution, it belongs here.

### Step 2: Uniqueness and Identity Audit
For every field that could be an identifier, code, or name:
- Ask: unique within what scope? (system-wide? within parent? within sibling set?)
- Document the answer explicitly in both the Validation Rules table AND the Constraints table.
- If the scope is ambiguous, it's an Open Question — not an assumption.

### Step 3: Lifecycle Trace (Before / During / After / Long-After)
For every entity:
- What creates it? Under what preconditions?
- What updates it? Which fields are mutable? Which are immutable? Is that a BRD rule or a design decision? If design decision — flag it.
- What deletes it? What blocks deletion?
- What references it from OUTSIDE this story? (other stories, historical archives, reports, feeds)
- What happens to it AFTER this story's lifetime? (archival, retention, soft-delete vs hard-delete)
- If deletion interacts with data retention requirements — that's an Open Question, not something to silently decide.

**Assumption Stress-Test:** For every assumption you make (e.g., "one department at a time"), ask: "Which BRD requirements does this assumption make impossible or unsolvable?" Walk through BRD Sections 6 and 7 checking for cross-department, cross-campus, or cross-program requirements that contradict the assumption. If a contradiction exists — it's an Open Question, not a silent deferral.

### Step 4: Cross-Story Seam Scan
For every field or entity this document references that is owned by another story:
- Name the owning story explicitly.
- Define the contract: what must exist before this story can reference it?
- **Read the owning story's PDs and KDs** (if design exists) or OQ resolutions (if req exists). Check HOW the referenced data is used — is it a hard constraint? A soft warning? Configurable? Do NOT assume usage semantics without checking the owner's decisions.
- Add to "Constraints Referenced from Other Documents."
- If ownership is unclear — it's an Open Question.

### Step 5: Number and Logic Audit
For every specific number, threshold, duration, or limit in the document:
- Is it directly quoted from the BRD? → Cite the exact sentence.
- Is it inferred or invented? → Mark as [TBD — confirm with stakeholder]. NEVER attribute an invented number to the BRD.
- Is it a reasonable default that needs confirmation? → Mark as [TBD] with the proposed default noted.

**For every rule, formula, or mapping** (not just numbers):
- Is this logic stated in the BRD, or did you invent it? (e.g., "L hours → L sessions" is an invented 1:1 mapping)
- If the BRD says "X determines Y" but doesn't specify HOW — the mapping is an Open Question, not something to invent as a "shall" requirement.
- If the logic depends on configurable data (e.g., slot durations vary per campus), the formula cannot be hardcoded — tag with [subject to OQ#N].

### Step 6: Open Question Collection + Propagation Check
Before writing Section 16 (Open Questions):
- Scan the ENTIRE document for: every [TBD], every inference labeled as such, every "if configured," every "governed by Open Question," every internal consistency conflict.
- Each one IS an open question. Collect them ALL into Section 16.
- If Section 16 says "None" but the document contains TBDs — the document is defective. This is a hard check.

**Propagation Check (after ANY edit to the document):**
- If you add/remove/change a constraint in one section, scan ALL other sections that reference constraint counts or lists: Section 6 (Constraints Owned table), Section 14 (Consistency Notes), Section 17 (Traceability table), and any Acceptance Criteria that mention constraint counts.
- If you add an Open Question, verify it appears in BOTH the relevant FR (tagged with [see OQ#N]) AND Section 16 (the OQ table).
- A change in one section that isn't propagated to dependent sections is a DEFECT — treat it as seriously as a missing FR.

**If any step reveals a gap or ambiguity, it must appear as either:**
- A flagged TBD in the relevant section, AND
- A corresponding entry in the Open Questions table

**The document CANNOT be written until all 6 steps are complete.**

---

## P6: Pre-Composition Checklist for Design Documents (Mandatory)

**Before writing ANY design document, complete this structured analysis FIRST. Design errors are interactions and consequences, not just omissions. A requirement surviving into the design is necessary but insufficient — the design must also verify that its own decisions don't collide with each other.**

### Step 1: Requirement Survival Check
For every FR, HC, and NFR in the source requirement document:
- Confirm it has a corresponding design element (endpoint, service method, validation, constraint, component).
- If a requirement has NO design element — it's a gap. Add it before writing.
- Pay special attention to NFRs that require mechanisms (audit trail integration, performance targets) — a one-sentence mention is not a design.

### Step 2: TBD Disposition Audit
For every [TBD] and Open Question in the source requirement document:
- If the design RESOLVES it (picks a concrete value or approach): mark it as "Provisional Decision — pending stakeholder ratification" in the design. Do NOT silently hardcode it as if confirmed.
- If the design DEFERS it: carry it forward into the design's Open Questions with the same wording.
- If the design IGNORES it: that's a gap. Fix it.
- A TBD that disappears between requirements and design without explicit resolution is a defect.

### Step 3: Decision Interaction Matrix
After drafting decisions, check every pair of non-trivial design decisions for unintended consequences:
- Does Decision A constrain Decision B in ways neither acknowledges?
- Does Decision A + Decision B produce a user-visible behavior that no requirement asks for (or contradicts one)?
- Common collisions to check:
  - Soft-delete + unique constraints = codes locked forever?
  - Caching + audit = stale reads bypass audit visibility?
  - Scoped access + tree queries = what does a scoped user see at levels above their scope?
  - Optimistic locking + soft-delete = version conflicts on deleted entities?

For each collision found: either resolve it in the design (with a Key Decision note) or add it to Open Questions.

### Step 4: Internal Consistency Verification
Before writing the final document, verify that every section agrees with every other section:
- If the error response example shows a check, the service code must perform that check.
- If the traceability table claims coverage, the named design element must actually exist in the document.
- If the architecture diagram shows a component, it must appear in at least one other section (service logic, API, or cross-cutting).
- If the testing strategy mentions a scenario, the service logic must support that scenario being testable.

Do NOT write a section in "confidence register" (asserting correctness) — write each section by re-deriving from the source. If you cannot point to the specific FR/HC/NFR that justifies a design element, the element is either gold-plating or the traceability is wrong.

### Step 5: Mechanism Completeness Check
For every NFR that requires a mechanism (not just a configuration):
- Name the component/pattern that implements it.
- Show how it integrates (listener? interceptor? outbox? event? same-transaction call?).
- If the mechanism is owned by another story (e.g., audit trail = A4-43), specify the integration contract: what does THIS module emit/call, and what does it expect back?
- A one-sentence "emitted to the audit trail service" with no mechanism is NOT a design — it's a placeholder. Either design the integration or mark it as a dependency with a specified contract.

### Step 6: Layer-Boundary Transfer Verification
After completing the design, perform a final pass specifically for the "fresh transformation problem":
- Re-read the source requirement document's Section 16 (Open Questions). For each OQ: is it addressed, carried forward, or silently dropped?
- Re-read the source requirement document's Constraints Owned table. For each constraint: does the design enforce it? Where (DB? Service? Both?)?
- Re-read the source requirement document's Validation Rules table. For each rule: does the design implement it? In which DTO/service?
- If ANY item from the source is missing from the design with no explicit acknowledgment — the design is incomplete.

### Step 7: End-to-End Path Simulation
For every entity or record the design creates/stores:
- Trace WHO reads it. Verify a read path exists (an endpoint, a service method, a consumer module).
- If nothing reads it, the entity is dead storage — either the consumer is missing from the design, or the entity shouldn't exist.

For every service method the design defines:
- Trace its FULL execution: what it validates, what it persists, what events it emits, what response it returns.
- Verify the response examples in Section 3 can ACTUALLY be produced by the logic in Section 5.
- If an error example shows data (e.g., a reconstructed path), verify the algorithm CAN produce that data. If it can't — either fix the algorithm or fix the example.

For every "flag" or "warning" behavior described:
- Identify the mechanism: is it a persisted flag column? A transient response field? An event emitted? An email sent?
- If no mechanism exists, the behavior is aspirational, not designed.

### Step 8: Cross-Document Consistency Check
Before finalizing any design document, verify against ALL previously written designs in the same project:
- **Migration versions:** must be sequential with no collisions (V1, V2, V3...). Check the last version used.
- **Column types for shared patterns:** audit columns (created_by type, timestamp type), soft-delete pattern (deleted_at type), BaseEntity fields — must be identical across all tables.
- **Error response format:** must use the same envelope structure (status, error, message, path, details) as established in the first design.
- **Shared enum values:** if the same concept (e.g., designation) appears in multiple designs, the values must be consistent.
- **Integration contracts:** if Design A says "calls X from Module B," and Design B exists, verify Design B actually exposes X with the expected signature.
- **Provisional Decision numbering:** must be sequential across the project (PD-1 through PD-N), not restarting per document.

---

## Enforcement

If Kiro violates any of these principles, the output is considered defective regardless of how polished or complete it appears. The user may reject it and require a redo.
