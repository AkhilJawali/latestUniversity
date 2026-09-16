---
inclusion: manual
name: design-derivation
---

# Requirement Design Derivation Guide

Produces the technical design for a **Requirement Design Derivation** task in the
board hierarchy (Epic → User Story → Requirement Generation → **Requirement
Design Derivation** → Code Development). The design is derived from the approved
requirements and is written with a Java / Spring Boot backend in mind.

All Jira actions use the Atlassian MCP server. This step produces a **design
document only** — it does not write application code. Code is generated later
under the Code Development task, following the `backend-tech` steering file.

---

## Precondition — Requirements must be Approved

Design generation starts **only** when the requirements are approved.

1. Identify the user story in scope (ask the user, or reuse the story already
   under discussion).
2. Locate its **Requirement Generation** task and check its status via the MCP.
3. **If the status is not `Approved`**, stop and tell the user:
   > "Design can't start yet — the Requirement Generation task {REQ-KEY} is
   > currently '{status}', not 'Approved'. Please get the requirements approved
   > first."
   Do not proceed.
4. Only when it is `Approved`, continue.

---

## Step 1 — Read and understand the requirements

Get the source requirements, in this order of preference:

1. A requirements file in the repo named `requirements-{ISSUE-KEY}.md`.
2. If not in the repo, read the requirements from the Jira task/story
   (description + comments) via the MCP.

If neither is available, ask the user to point to the requirements file or key.

Read it fully. Extract the functional requirements (FR-xx), non-functional
requirements (NFR-xx), and security requirements (SR-xx). Every design element
you produce must trace back to one or more of these IDs.

---

## Step 2 — New application vs existing codebase

Determine whether this is a greenfield application or a change to an existing one.

- **New application:** scaffold with the **latest stable GA** of the tech stack.
  Default target (verify current values at scaffold time — see `backend-tech`):
  Java LTS (21 or 25) + latest Spring Boot GA (4.1.x line). Do not pick a
  non-LTS Java for a new service. Design the module/package layout from scratch
  per `backend-tech`.
- **Existing application:** match the versions, structure, and conventions
  already in the repo. Do not force a framework/Java upgrade as part of design
  unless the requirements explicitly call for it — flag it as a separate concern
  if an upgrade seems necessary.

State clearly in the design which case applies and the chosen versions.

---

## Step 3 — Produce the design document

Write the design to the repo root as `design-{ISSUE-KEY}.md`, following the
`backend-tech` steering file for all code-generation conventions (package
structure, layering, API and persistence patterns). Use this structure:

```markdown
# Design: {User Story Summary}

**Jira Reference:** {ISSUE-KEY}
**Source Requirements:** requirements-{ISSUE-KEY}.md
**Application:** New | Existing
**Stack:** Java {version} · Spring Boot {version} · {build tool}
**Generated:** {DATE}

## 1. Overview
{What is being built and why, in design terms.}

## 2. Architecture
{High-level component/layer diagram in words: controller → service →
repository. External integrations. Sync vs async.}

## 3. API Design
For each endpoint, per backend-tech REST conventions:
- Method + path (versioned, e.g. /api/v1/...)
- Request DTO / response DTO (never expose entities)
- Status codes and error responses
- Auth/authorization required
- Traces to: FR-xx

## 4. Data Model
- Entities / tables, key fields, relationships
- Migration approach (Flyway/Liquibase)
- Traces to: FR-xx

## 5. Service / Business Logic
- Key services and their responsibilities
- Validation rules (Jakarta Validation)
- Transaction boundaries

## 6. Cross-cutting Concerns
- Error handling strategy (@RestControllerAdvice / Problem Details)
- Security (authn/authz, per SR-xx)
- Configuration & profiles (no secrets in code)
- Logging / observability

## 7. Non-Functional Design
- How each NFR-xx is met (performance, scalability, etc.)

## 8. Testing Strategy
- Unit test approach (feeds the Unit Test sub-task)
- Integration tests (Testcontainers where relevant)

## 9. Requirement Traceability
| Requirement | Design element(s) |
|-------------|-------------------|
| FR-01 | ... |

## 10. Open Questions
| # | Question | Owner | Status |
```

---

## Step 4 — Ask for approval in chat

Present the design and ask:

> "The design document has been generated. Do you approve it? (yes / no / request changes)"

- No / changes → apply and re-present.
- Yes → continue to Step 5.

Never sync to Jira before this approval.

---

## Step 5 — Locate the Requirement Design Derivation task

Do not ask for a ticket up front. The design belongs on the **Requirement
Design Derivation** task under the same user story. Find it automatically:

1. Search the children of the story via the MCP:
   `parent = {STORY-KEY} AND summary ~ "Requirement Design Derivation"`
2. **Exactly one match** → use it, and state which one.
3. **No match** → ask the user for the target issue key.
4. **Multiple matches** → list them and ask the user to pick.

The resolved key becomes `{ISSUE-KEY}` for the next steps.

---

## Step 6 — Ask for the Assignee Email

Ask:

> "Who should this design task be assigned to? Please provide their email address."

- Look up the Jira account ID using the provided email.
- If no account is found, inform the user and ask for a different email, or let
  them skip assignment (note that it was skipped).

---

## Step 7 — Confirm, then sync to Jira

### 7a. Pre-sync confirmation (required)

Fetch the task and show all pending changes, then wait for a "yes":

> "About to update the Requirement Design Derivation task **{ISSUE-KEY}**:
> - Description → the approved design
> - Comment → added (source file: design-{ISSUE-KEY}.md)
> - Assignee → {email}
> - Status → {current status} → PENDING APPROVAL
>
> Proceed? (yes / no)"

### 7b. Apply, then report the actual result

1. Update the task description with the approved design.
2. Add a comment noting the source file `design-{ISSUE-KEY}.md`.
3. Assign the task to the user identified by the provided email (skip if no
   account was found in Step 6, and note that it was skipped).
4. Transition to **PENDING APPROVAL**:
   - Fetch available transitions first.
   - If PENDING APPROVAL is available, apply it.
   - If not, do NOT substitute another status — stop and tell the user it's a
     workflow-wiring issue and ask how to proceed.
5. Confirm with the **actual** resulting status, the assignee, and the issue link.

---

## Notes

- Design starts only when the Requirement Generation task is `Approved`.
- This step outputs a design document only — no application code.
- Follow the `backend-tech` steering file for all stack and code conventions.
- Always ask for the assignee email (Step 6) before syncing to Jira.
- Never sync to Jira before the chat approval (Step 4) and the pre-sync
  confirmation (Step 7a).
- Never invent or silently substitute a Jira status; only use valid transitions.