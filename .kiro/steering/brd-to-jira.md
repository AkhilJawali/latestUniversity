---
inclusion: auto
name: brd-to-jira
description: Generate a Jira breakdown from a Business Requirements Document (BRD). Use whenever the user provides or references a BRD and asks to create an epic, user stories, tasks, or sub-tasks in Jira.
---

# BRD → Jira Breakdown

This steering guides Kiro to turn a Business Requirements Document (BRD) into a
structured Jira hierarchy using the Atlassian MCP server.

## Golden rules

- All Jira writes go through the Atlassian MCP server.
- This is a **two-phase** workflow. First produce the full breakdown as a
  reviewable outline and get the user's approval. Only then create anything
  in Jira.
- Never create Jira issues silently. Create them top-down (Epic → Stories →
  Tasks → Sub-tasks) and report the issue key + link after each one.
- Ask for the target Jira **project key** before creating anything. If unknown,
  list available projects first.
- **All stories must comply with `story-quality.md` guardrails.** Before finalizing
  any story, validate against R1–R12 in that file (no implementation details,
  no algorithm lock-in, no gold-plating, mandatory acceptance criteria, etc.).

## Input — find the BRD in the workspace

Search the workspace (project root, and common docs locations like `docs/`) for
a BRD document. Candidate files are `.md` or `.txt` files whose name suggests a
BRD (e.g. contains "brd", "business", or "requirements").

**Important — supported formats:**
- ✅ `.md` (Markdown) — readable directly
- ✅ `.txt` (Plain text) — readable directly
- ❌ `.docx` (Word) — binary format, cannot be read by Kiro
- ❌ `.pdf` — binary format, cannot be read by Kiro

**If a `.docx` or `.pdf` is found:**
Inform the user:
> "I found **{filename}** but it's a `.docx`/`.pdf` file which I can't read directly.
> Please save it as a `.md` or `.txt` file and place it in the workspace folder:
> `{workspace_path}`
> Then ask me again and I'll process it immediately."

Do NOT ask the user to paste the content — direct them to convert the file instead.

1. **Exactly one readable BRD found** → use it, and state which file:
   > "Using **{filename}** as the BRD. I'll analyze it now."
2. **Multiple candidates found** → list them and ask which to use.
3. **None found, but a .docx/.pdf exists** → show the conversion message above.
4. **Nothing found at all** → ask the user to add the BRD to the workspace folder.

Once the file is chosen, read it fully and identify the goal, scope, actors,
functional requirements, and any non-functional requirements. Do not invent
requirements — everything must come from the document.

## Phase 1 — Analyze and draft (no writes)

Produce a nested outline for review:

### 1. Epic

- Derive **one Epic** that captures the overall initiative described in the BRD.
- Give it a clear name and a one-paragraph goal summarizing the BRD.

### 2. User Stories

- Generate **all** user stories that the BRD implies — cover the full scope.
- Write each in the form: *"As a `<role>`, I want `<capability>` so that
  `<benefit>`."*
- **Sizing:** each story must be **≤ 8 story points**. If a story would exceed
  8 points, split it into smaller stories until each is ≤ 8.
- **Testability:** each story must be independently testable. Include clear
  **acceptance criteria** (Given/When/Then) that a tester can verify. If a
  story cannot be expressed with testable acceptance criteria, split or
  rewrite it until it can.

### 3. Default Subtasks under each User Story

For every user story, create the following **4 default subtasks** (all Subtask
issue type, parent = Story):

- **Requirement Generation** — capture and document the detailed requirements
  for this story.
- **Requirement Design Derivation** — derive the technical/functional design
  from the requirements.
- **Code Review** — peer review of all code developed for this story.
- **Testing** — end-to-end testing of the implemented story.

Development subtasks (from design doc) are NOT created at this stage — they are
created later after the design is approved.

## Phase 1.5 — Source Document Coverage Verification (Mandatory Gate)

Before presenting the story outline for user approval, Kiro must perform a
line-by-line verification of the source document to ensure no requirement is missed.

**This gate is mandatory. No stories are created in Jira until it passes with zero unacknowledged gaps.**

### Steps:

1. **Functional requirements coverage:** List every functional requirement section
   in the source document. For each section, state which story covers it. If no
   story covers a section → flag as **GAP**.

2. **Constraints and parameters coverage:** List every constraint/parameter table
   or row in the source document. For each row, state which story's scope or
   acceptance criteria addresses it. If none → flag as **GAP**.

3. **Non-functional requirements coverage:** List every non-functional requirement
   in the source document. For each, state which story addresses it. If none →
   flag as **GAP**.

4. **Cross-cutting concerns check:** For each entity/concept that appears in
   multiple sections of the source document (e.g., a resource that needs both a
   master data story AND a blocking/scheduling story), verify that ALL aspects
   have a story. If only some aspects are covered → flag as **PARTIAL GAP**.

5. **Present all gaps to the user.** Each gap must be resolved as one of:
   - (a) Added as a new story to the outline
   - (b) Added as scope to an existing story
   - (c) Explicitly deferred with user confirmation and documented reason

**Only after all gaps are resolved (added or deferred) may the outline be presented for final approval and Phase 2 creation begin.**

## Phase 2 — Create in Jira (after approval)

Create issues in this order and link them correctly:

1. Create the **Epic**.
2. Create each **User Story** linked to the Epic (parent field with Epic key).
   Set the story-point estimate (≤ 8).
3. Under each Story, create the **4 default Subtasks** using `parent: { "id": "<story_numeric_id>" }`.

Report each created issue's key and URL as you go. Pause and ask if any
required field (project, issue type, estimate field) is missing or ambiguous.

**Important API note:** For subtask creation, always use the numeric ID of the
parent Story (from the creation response), not the key. Format:
`customFields: { "parent": { "id": "<numeric_id>" } }`

## Statuses (workflow)

These statuses already exist in the project's workflow, so no status creation
is needed:

- **Requirement Generation** and **Requirement Design Derivation** tasks:
  `In Progress`, `PENDING APPROVAL`, `Approved`, `Hold`, `Rejected`.
- **Unit Test** and **API Documentation** sub-tasks:
  `In Progress`, `DONE`.

Do not transition issues into any status automatically unless the user asks —
creation should leave issues in their default initial status.


```
Epic
 └── User Story  (<= 8 story points, testable)
      ├── Subtask: Requirement Generation
      ├── Subtask: Requirement Design Derivation
      ├── Subtask: Code Review
      ├── Subtask: Testing
      │
      │   (Added after design approval — same level:)
      ├── Subtask: Development Task 1
      ├── Subtask: Development Task 2
      ├── Subtask: Unit Test
      └── Subtask: Code Coverage
```

## Post-Creation Workflow — Assignment & Approval Gate

This section defines the mandatory workflow that team members must follow after
stories are created. **Kiro must enforce these rules in all interactions.**

### Story Assignment

- After the Epic and Stories are created, a **lead/manager** assigns each Story
  to a specific team member.
- Only once a Story is assigned does it appear on that team member's dashboard.
- **Kiro rule:** Do not begin any requirement or design work on a Story until
  the user confirms it has been assigned to them.

### Requirement Generation Flow

1. The assigned team member reads the Story and its acceptance criteria.
2. They create a requirements document locally in `docs/requirements/`.
3. They review the document locally until satisfied.
4. Once satisfied, they upload the `.md` file to the Requirement Generation subtask attachment in Jira.
5. After uploading, they assign the subtask to their lead for approval.
6. The lead reviews and either Approves or Rejects:
   - **Approved** → Design Derivation unlocked.
   - **Rejected** → Revise and re-upload.

### Design Derivation Gate (CRITICAL RULE)

> **A team member is ONLY allowed to begin the Requirement Design Derivation
> subtask if and only if the corresponding Requirement Generation subtask has
> been approved (`Approved` status).**

- If Requirement Generation is NOT in `Approved` status → **BLOCK** any
  attempt to create, write, or upload a design document.
- Kiro must **refuse to generate or assist with design derivation** for a
  Story whose Requirement Generation subtask is not approved.

### Enforcement Summary

```
Story Assigned → Member reads story
  → Creates requirements doc locally
  → Reviews locally
  → Uploads to Requirement Generation subtask (attachment)
  → Assigns to lead for approval
  → Lead Approves / Rejects
      ├── Approved → Design Derivation subtask UNLOCKED
      └── Rejected → Revise requirements, re-upload, re-submit
```

## Output summary

After creation, give the user a summary table: Epic key, and for each Story its
key, point estimate, its Task keys, and the Sub-task keys — each with a link.
