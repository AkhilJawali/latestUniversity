---
inclusion: always
---

# Squad Rules

## Working Agreements
- All work is driven from Jira tickets — no untracked work enters the sprint
- Every task maps to a BRD requirement or a technical enabler; traceability is mandatory
- Prefer small, focused PRs (< 400 lines) over large monolithic changes
- Blocked items are raised in standup immediately, not end-of-day
- All communication about decisions lives in Jira comments or ADRs, not Slack/email threads
- Demo what you ship — every completed story gets a brief walkthrough in sprint review

## Code Review Standards
- All PRs require at least 1 approval (2 for security-sensitive or schema changes)
- Reviewers respond within 4 business hours
- Author resolves all comments before merge
- Use "Request Changes" for blocking issues, "Comment" for suggestions
- PRs must include:
  - Link to Jira ticket
  - Brief description of what changed and why
  - Steps to test (or automated test evidence)
- No self-merging unless explicitly approved by tech lead for hotfixes

### AI-Automated Code Review

Before submitting code for human review, Kiro must perform an automated AI code review using the `semantic_reviewer` sub-agent. This catches issues before they reach the reviewer's eyes.

**Process:**
1. When the Code Review subtask is about to be submitted (before assigning to human reviewer), Kiro triggers an AI code review.
2. The `semantic_reviewer` analyzes the local diff and produces a behavioral review organized by concern.
3. Review output is saved to `docs/code-review/{ISSUE-KEY}-ai-review.md`.
4. **If verdict is NEEDS_CHANGES:**
   - Kiro lists the issues found.
   - Kiro asks the user if they want the issues auto-fixed.
   - If yes: Kiro fixes the issues, re-runs the review until verdict is APPROVED or COMMENT.
   - The AI review doc is attached to the Code Review subtask in Jira.
5. **If verdict is APPROVED or COMMENT:**
   - Code is ready for human review.
   - AI review doc is attached to the Code Review subtask.
   - Subtask is transitioned to Pending Approval and assigned to the designated reviewer.

**What the AI reviews:**
- Routing completeness (are all pages reachable?)
- Accessibility (focus trapping, ARIA, keyboard nav)
- Code duplication (repeated patterns that should be extracted)
- Security (XSS vectors, hardcoded secrets, unsafe patterns)
- Standards compliance (naming, structure, patterns from steering files)
- Error handling completeness
- Test coverage gaps

**When to trigger:**
- During the Code Review step — after dev + unit tests + coverage are done, before human reviewer is assigned
- On-demand when user says "review my code" or "run code review"

**Rules:**
- AI review does NOT replace human review — it's a pre-check that runs during the Code Review step
- AI review findings are documented and tracked
- Blocking issues must be fixed before human reviewer is assigned
- The AI review doc is always attached to the Jira Code Review subtask

## Definition of Done
- [ ] Code complete and passes linting
- [ ] Unit tests written and passing (minimum 80% coverage on new code)
- [ ] Integration tests passing where applicable (API endpoints, DB queries)
- [ ] No new critical/high Sonar issues introduced
- [ ] API documentation updated (OpenAPI spec for new/modified endpoints)
- [ ] Database migrations reviewed and reversible
- [ ] PR reviewed and approved
- [ ] Jira ticket updated and moved to Done
- [ ] No regression in existing functionality (CI pipeline green)
- [ ] Security checklist verified (input validation, parameterized queries, no secrets in code)

## Sprint Ceremonies (2-Week Sprints)

| Ceremony | Cadence | Duration | Attendees |
|----------|---------|----------|-----------|
| Standup | Daily (Mon–Fri) | 15 min | Full squad |
| Sprint Planning | Day 1 of sprint | 2 hrs | Full squad + PO |
| Backlog Refinement | Mid-sprint (Wed, Week 1) | 1 hr | Full squad + PO |
| Sprint Review / Demo | Last day of sprint | 1 hr | Squad + stakeholders |
| Retrospective | Last day of sprint (after review) | 45 min | Full squad |

## Branch Strategy
- `main` — production-ready, protected
- `develop` — integration branch, all feature PRs target here
- `feature/<TICKET-ID>-short-description` — feature branches
- `bugfix/<TICKET-ID>-short-description` — bug fixes
- `hotfix/<TICKET-ID>-short-description` — production hotfixes (cherry-picked to main)

## Commit Message Format
```
<type>(<TICKET-ID>): <short description>

[optional body]

[optional footer: breaking changes, references]
```
Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`

## Subtask Assignment Inheritance

**If a User Story is assigned to a team member, all subtasks under that story are implicitly assigned to the same person.**

This means:
- Kiro does not need to check subtask-level assignment separately — checking the parent story assignment is sufficient.
- If the parent story is assigned to you, you are authorized to work on any subtask (Requirement Generation, Design Derivation, etc.) under that story.
- This applies to access control checks: verifying story assignment grants access to all its subtasks.

### Auto-Assignment on Story Assignment

**When a story is assigned to someone, all its subtasks must also be assigned to that person by default.**

- When Kiro assigns a story (or is told a story has been assigned), it must also assign all subtasks under that story to the same assignee **in Jira** — not just implicitly.
- This includes all 4 default subtasks (Requirement Generation, Design Derivation, Code Review, Testing) and any development subtasks already created.
- The assignee can later be changed on individual subtasks if needed (e.g., Code Review assigned to a reviewer).
- This is a default behavior — not a lock. Reassignment is always allowed.
- **Kiro enforcement:** After assigning a story, query its children (`parent = {STORY-KEY}`) and assign each one to the same person.

---

## Requirement Document Access Control

### The Rule

**If you are not assigned to a User Story in Jira, you are blocked from creating or editing its requirement document. No exceptions.**

### How It's Enforced

Kiro checks Jira assignment (via MCP) before any requirement document work. The current user is identified by their `JIRA_EMAIL` in their MCP config.

When any team member asks to create or edit a requirement doc:

1. Kiro identifies the Jira story key (from the request or filename).
2. Kiro queries Jira to check who the story is assigned to.
3. Compares the assignee against the current user's `JIRA_EMAIL`.
4. **If assigned to them** → proceeds.
5. **If not assigned** → refuses entirely. No drafts, no workarounds, no temp files.

### Workflow

```
1. Story Assigned     → PO/Lead assigns a User Story to a team member in Jira
2. Requirement Doc    → The assigned member (and ONLY them) creates the requirement doc
3. Submit for Review  → Author assigns the Jira subtask to their lead
4. Review             → Lead reviews the requirement doc
5. Approval/Rejection → Lead approves or requests changes via Jira transition
6. Iterate            → If rejected, author revises and resubmits
```

### Rules

1. **Not assigned = blocked.** If the story is unassigned or assigned to someone else, you cannot create, edit, or append to its requirement file.
2. **One owner per story.** Only one team member is assigned the story at a time.
3. **Submit to lead, not self-approve.** After writing the doc, assign the subtask to the lead for review.
4. **Wait for approval before proceeding.** Do not begin design derivation or code until the requirement doc is approved (status = "Approved").
5. **This is a team-wide rule.** Everyone gets it. No one is exempt.

---

## Escalation Path
1. Blocker raised in standup
2. If unresolved in 4 hours → escalate to tech lead
3. If cross-squad dependency → escalate to engineering manager
4. If timeline risk → PO and stakeholders informed same day
