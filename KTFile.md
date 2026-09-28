# Kiro KT Guide — Quick Reference

## What is Kiro?
Kiro is an AI-powered development environment that assists developers in coding, documentation, and workflow management within the IDE.

---

## BRD to Jira Workflow

### Step 1: BRD Analysis
- **Steering file used:** `brd-to-jira.md`
- **What happens:** Kiro reads the BRD, extracts requirements, identifies functional and non-functional needs.

### Step 2: Create Epic & Stories
- **Steering file used:** `workflow.md` + `brd-to-jira.md`
- **What happens:** One Epic is created; BRD is broken into User Stories (each ≤ 8 story points).
- **Rules:** Every story must reference BRD point numbers in its description.

### Step 3: Default Subtasks (Created Automatically)
Every User Story gets 4 default subtasks:
1. **Requirement Generation** — Create requirements doc (`docs/requirements/`)
2. **Requirement Design Derivation** — Create design doc (`docs/design/`)
3. **Code Review** — Peer review of code
4. **Testing** — End-to-end testing

---

## Gating Rules (Sequential Flow)

| Gate | Condition to Pass |
|------|-------------------|
| Requirement → Design | Requirement Generation subtask status = "Approved" |
| Design → Development | Design Derivation subtask status = "Approved" |
| Development → Unit Test | All development subtasks complete |
| Unit Test → Code Coverage | Unit tests pass, coverage doc generated (≥80%) |
| Code Coverage → Code Review | Coverage doc exists |
| Code Review → Testing | No open issues in code review |
| Testing → Done | Testing subtask status = "Approved" (by Lead) |

---

## Key Steering Files

| File | Purpose | When Used |
|------|---------|-----------|
| `workflow.md` | Mandatory development workflow, gating rules, Jira enforcement | All work in workspace |
| `brd-to-jira.md` | BRD → Epic → Stories → Subtasks breakdown | When processing a BRD |
| `squad-rules.md` | Working agreements, review standards, DoD | All team work |
| `tech.md` | Tech stack, libraries, infrastructure | Coding tasks |
| `structure.md` | Folder layout, doc locations | File organization |
| `token-efficiency.md` | Token usage limits, one review pass, diff-only review | AI operations |

---

## Important Rules

### Assignment & Access Control
- Work starts only when a Story is **assigned to you** in Jira.
- All subtasks inherit assignment from the parent Story.
- You cannot create/edit requirement docs for stories not assigned to you.

### Document Locations
```
docs/
├── requirements/    → {ISSUE-KEY}-{title}-requirements.md
├── design/          → {ISSUE-KEY}-{title}-design.md
├── code-coverage/   → {ISSUE-KEY}-{task-id}-coverage.md
├── code-review/     → {ISSUE-KEY}-code-review.md
└── testing/         → {ISSUE-KEY}-testing-results.md
```

### Git Rules
- **Feature branch per story:** `feature/{STORY-KEY}-{description}`
- **Never push directly to main** — always use feature branches.
- **Pre-push verification:** Build and tests must pass before pushing.
- **Merge only after:** All subtasks Done, Code Review approved, Testing passed.

### Labels on Every User Story (Mandatory)
1. **Role label (one required):** `backend` / `frontend` / `fullstack`
2. **Feature area label (at least one):** e.g., `master-data`, `scheduling-engine`, etc.
3. **Functionality type label (at least one):** e.g., `crud`, `algorithm`, `workflow`, etc.

---

## Frontend Story Pairing Rule
- For every backend story exposing REST APIs, create a corresponding frontend story.
- Both are separate stories under the same Epic.
- Frontend story naming: `Frontend — {Module Name} ({Description})`

---

## Final Flow (Linear)
```
BRD → Epic → User Story
  → Requirement Generation → Approval
  → Design Derivation → Approval
  → Development Subtasks
  → Unit Test → Code Coverage
  → Code Review
  → Testing → Approval (Lead)
  → Done
```

---

## Quick Tips
- Always check the Testing subtask status to confirm story completion.
- Attach `.md` files to Jira subtasks before submitting for approval.
- Run `mvn clean package` (backend) or `pnpm build` (frontend) before pushing.
- Use conventional commit format: `feat(AID-123): description`
