---
inclusion: manual
name: requirements-generation
---

# Requirements Generation Guide

When the user asks to generate a requirements file, follow the steps below strictly and in order.

This flow produces the requirements for a **Requirement Generation** task in the
board hierarchy (Epic → User Story → Requirement Generation / Requirement Design
Derivation / Code Development). All Jira actions use the Atlassian MCP server.

---

## Step 1 — Select the User Story

Fetch the user stories currently assigned to the requesting user and let them
pick one, rather than asking for a key up front.

**IMPORTANT: Assignment Verification (Mandatory)**
Before proceeding with ANY requirement generation, verify that the story is
assigned to the current user. The current user's email is available from the
MCP Jira config (`JIRA_EMAIL`). If the story is NOT assigned to the current
user, STOP immediately and respond:

> "You are not assigned to story {KEY}. Only the assigned team member can
> create or modify its requirement document. Ask your lead for reassignment
> if you need to work on this story."

Do NOT proceed, offer alternatives, or create drafts. This is a hard rule.

1. Query Jira via the MCP for stories assigned to the current user. Use a JQL
   filter equivalent to:
   `assignee = currentUser() AND issuetype = Story AND statusCategory != Done ORDER BY updated DESC`
2. **If one or more stories are found**, present them as a numbered list showing
   the issue key, summary, and current status, and ask:

   > "Here are the user stories assigned to you. Which one should I generate requirements for? (reply with the number or the issue key)
   > 1. KAN-64 — {summary} — {status}
   > 2. KAN-71 — {summary} — {status}
   > ..."

   Wait for the user to choose one.
3. **If no stories are assigned**, fall back to asking for the key directly:

   > "You don't have any user stories assigned right now. Which Jira user story should I use? Please provide the issue key (e.g. KAN-64)."

Once the user has selected (from the list) or provided an issue key:
- Fetch the full issue details from Jira (summary, description, acceptance criteria)
- Use the fetched content as the source of truth for requirement generation
- Derive the output filename from the issue key: `requirements-{ISSUE-KEY}.md` (e.g. `requirements-KAN-64.md`)

---

## Step 2 — Ask for a Custom Prompt (Optional)

Ask the user:

> "Do you have a custom prompt or specific instructions for how the requirements document should look? If not, I'll use the default format."

- If the user provides instructions, incorporate them into the document structure
- If the user says no or skips, use the Default Requirements Format below

---

## Step 3 — Generate the Requirements File

Write the requirements file to the workspace root using the format below.
Do NOT include any approval checkbox or approval section in the document.

---

## Default Requirements Format

```markdown
# Requirements: {User Story Summary}

**Jira Reference:** {ISSUE-KEY}
**Generated:** {DATE}

---

## 1. Overview

{2–3 sentence summary of the feature, its business goal, and primary stakeholders derived from the user story.}

---

## 2. Glossary

| Term | Definition |
|------|------------|
| {Term} | {Definition} |

---

## 3. Functional Requirements

### FR-01: {Requirement Title}

- **Description:** {What the system must do}
- **Source:** {Acceptance criterion or user story text this was derived from}
- **Acceptance Criteria:**
  - WHEN {trigger}, the system SHALL {behaviour} WITHIN {constraint}.
  - IF {condition}, the system SHALL {behaviour}.
- **Priority:** Must Have | Should Have | Could Have

{Repeat for each functional requirement}

---

## 4. Non-Functional Requirements

### NFR-01: {Requirement Title}

- **Description:** {Quality attribute or constraint}
- **Acceptance Criteria:**
  - THE system SHALL {measurable behaviour}.
- **Priority:** Must Have | Should Have | Could Have

---

## 5. Security Requirements

### SR-01: {Requirement Title}

- **Description:** {Security constraint or control}
- **Acceptance Criteria:**
  - THE system SHALL {measurable security behaviour}.
- **Priority:** Must Have

---

## 6. Out of Scope

- {List what is explicitly NOT included}

---

## 7. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | {Question} | {Owner} | Open |
```

---

## Writing Rules

- Use **EARS notation** for all acceptance criteria: `WHEN`, `WHILE`, `IF`, `THEN`, `THE system SHALL`
- Every requirement must be **testable** — avoid vague terms like "fast", "secure", "easy"
- Assign a unique ID to every requirement: `FR-01`, `FR-02`, `NFR-01`, `SR-01`, etc.
- Derive requirements directly from the user story's description and acceptance criteria
- Do NOT add an approval section or checkbox to the document
- **All requirement documents must comply with `requirement-quality.md` guardrails.** Before finalizing
  any document, validate against R1–R13 in that file (organize by behavior not algorithm, user journeys
  first, no invented specifics, contradiction check, traceability verification, constraint coverage, etc.).

---

## Step 4 — Ask for Approval in Chat

After generating the file, present it to the user and ask in chat:

> "The requirements document has been generated. Do you approve it? (yes / no / request changes)"

- If the user says **no** or requests changes — apply the changes and re-present for approval
- If the user says **yes** or equivalent (e.g. "approved", "looks good", "proceed") — move to Step 5

Never sync anything to Jira before this explicit approval.

---

## Step 5 — Locate the Requirement Generation Task

Do not ask the user for a ticket up front. The approved requirements belong on
the **Requirement Generation** task that sits under the user story selected in
Step 1. Find it automatically:

1. Search Jira via the MCP for the child of the selected story whose summary is
   the Requirement Generation task. Use a JQL filter equivalent to:
   `parent = {STORY-KEY} AND summary ~ "Requirement Generation"`
   (If that returns nothing, also list all children of the story and look for
   the one named "Requirement Generation".)
2. **Exactly one match** → use it as the target task. State which one:
   > "Found the Requirement Generation task for this story: **{TASK-KEY} — {summary}**. I'll attach the approved requirements there."
3. **No match** → the task couldn't be found, so ask:
   > "I couldn't find a 'Requirement Generation' task under {STORY-KEY}. Which Jira issue should I attach the approved requirements to? Please provide the issue key (e.g. KAN-65)."
4. **More than one match** (ambiguous) → list the candidates and ask the user to
   pick the correct one, or to provide the issue key.

The resolved task key becomes `{ISSUE-KEY}` for the remaining steps.

---

## Step 6 — Ask for the Assignee Email

Ask:

> "Who should this ticket be assigned to? Please provide their email address."

- Look up the Jira account ID using the provided email
- If no account is found, inform the user and ask for a different email or skip assignment

---

## Step 7 — Confirm, then Sync to Jira

The statuses `In Progress`, `PENDING APPROVAL`, `Approved`, `Hold`, `Rejected`
already exist for Requirement Generation tasks, so no status creation is needed.

### 7a. Pre-sync confirmation (required)

Before writing anything, fetch the target ticket and show the user a summary of
every change that is about to be made, then wait for a final "yes":

> "About to update the Requirement Generation task **{ISSUE-KEY}**:
> - Description → replaced with the approved requirements
> - Comment → added
> - Assignee → {email}
> - Status → {current status} → PENDING APPROVAL
>
> Proceed? (yes / no)"

If the user says no, stop and ask what to change. Do not write anything.

### 7b. Apply the changes

After confirmation, perform these in order and report the result of each:

1. **Update the Jira issue description** with the full approved requirements content.
2. **Add a comment** on the issue:
   > "Requirements document approved and attached. Source file: `requirements-{ISSUE-KEY}.md`"
3. **Assign the ticket** to the user identified by the provided email (skip if
   no account was found in Step 6, and note that it was skipped).
4. **Transition the ticket to "PENDING APPROVAL":**
   - Fetch the issue's **available transitions** first (a status existing in the
     project does not guarantee it is reachable from the current status).
   - If "PENDING APPROVAL" is an available transition, apply it.
   - If it is **not** an available transition from the current status, do NOT
     substitute a different status. Stop and inform the user:
     > "'PENDING APPROVAL' exists but isn't a valid transition from the current
     > status '{current status}'. This is a workflow wiring issue (Project
     > Settings → Workflow). How would you like to proceed — route through an
     > intermediate status, or fix the workflow first?"

### 7c. Confirm with the actual result

Report the **actual** resulting status (not an assumed one):

> "Done! Requirements attached to {ISSUE-KEY}, assigned to {email}, status is now **{actual resulting status}**. Link: {issue URL}"

If any individual step failed, say which one and why, rather than reporting
overall success.

---

## Notes

- Never sync to Jira before explicit chat approval in Step 4, and never write
  in Step 7 before the pre-sync confirmation in Step 7a.
- Resolve the target automatically (Step 5): it is the Requirement Generation
  task under the selected story, not the story itself. Only ask for a key if the
  task can't be found or the match is ambiguous.
- Always ask for the assignee email (Step 6) before updating the ticket.
- Never invent or silently substitute a Jira status; only use valid transitions.