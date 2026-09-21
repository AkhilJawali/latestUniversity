---
inclusion: always
---

# Development Workflow — Epic → User Story → Done

This steering file defines the mandatory workflow for all development work in the UTMS project.
Every team member and Kiro must follow this flow. No shortcuts.

---

## Structure

Every User Story in Jira has exactly **4 default subtasks** created at story creation time. Development subtasks are added later from the approved design document, at the same level as the default subtasks.

```text
Epic
└── User Story (≤ 8 Story Points, Testable)
    ├── Subtask: Requirement Generation
    ├── Subtask: Requirement Design Derivation
    ├── Subtask: Code Review
    ├── Subtask: Testing
    │
    │   (Added after design approval — same level:)
    ├── Subtask: Development Task 1
    ├── Subtask: Development Task 2
    ├── Subtask: Development Task 3
    ├── Subtask: Unit Test
    └── Subtask: Code Coverage
```

The 4 default subtasks are created when the story is created. Development subtasks are created after the design is approved — as siblings at the same level as the Requirement/Design/Review/Testing subtasks. Work begins only when the story is assigned to a team member.

---

## Jira Issue Type Mapping

| Level | Jira Issue Type | Parent |
|-------|----------------|--------|
| Top | Epic | None |
| Mid | Story | Epic (via parent ID) |
| Bottom | Subtask | Story (via parent ID) |

**Important:** When creating subtasks via API, always use `"parent": {"id": "<numeric_id>"}` — never use `"key"`. This is a known requirement for the AID project.

---

## Flow

### 1. Epic & User Story

- Epics are created from the BRD.
- Epics are broken into **testable User Stories**, each ≤ 8 story points.
- Each User Story gets the 4 default subtasks immediately on creation.
- Work starts only when the story is assigned.
- **Story Description Rule:** Every User Story description must comprehensively cover all aspects of that story's scope. It must also explicitly reference the BRD point number(s) (e.g., "BRD Section 6.3", "Requirement 9.1, 9.2") that the story addresses. No story should exist without clear traceability back to the BRD.

### 2. Requirement Generation (Default Subtask 1)

- The assigned team member creates the Requirement Document locally.
- File location: `docs/requirements/{ISSUE-KEY}-{kebab-case-title}-requirements.md`
- Uploads/syncs it to the Requirement Generation subtask in Jira.
- Assigns the subtask to the Team Lead for review.
- **Lead approves → Design starts.**
- **Lead rejects → Update document → Resubmit.**

### 3. Requirement Design Derivation (Default Subtask 2)

- The assigned team member creates the Design Document based on the approved requirements.
- File location: `docs/design/{ISSUE-KEY}-{kebab-case-title}-design.md`
- Uploads/syncs it to the Design Derivation subtask in Jira.
- Assigns the subtask to the Team Lead for review.
- **Lead approves → Development starts.**
- **Lead rejects → Update document → Resubmit.**

### 4. Code Development (Subtasks created from Design)

- Once the design is approved, Kiro generates development subtasks from the approved Design Document.
- Development subtasks are created **at the same level** as the default subtasks (all are Subtask type under the Story).
- Kiro completes the development subtasks one by one.
- Once all development subtasks are completed:
  - **Unit Testing** is performed (created as a subtask).
  - Any issues found are documented, fixed, and retested.
  - A **Code Coverage** subtask is created and a coverage document is generated.

#### Code Coverage Document

- Location: `docs/code-coverage/{ISSUE-KEY}-{task-id}-coverage.md`
- Must include: task ID, story key, coverage percentage, and uncovered areas.
- The developer runs the coverage tool locally. The document is built from the tool's summary output only; the AI does not estimate or narrate per-class tables (see `shared/token-efficiency.md` T4).

### 5. Code Review (Default Subtask 3)

- After development and unit testing, the code goes for Code Review.
- Before the review, the developer runs build, lint, and tests locally.
- The AI review is a single pass over the story's `git diff` or changed files only (see `shared/token-efficiency.md` T2, T3).
- **No issues found → Continue to Testing.**
- **Issues found:**
  - Issues are documented in: `docs/code-review/{ISSUE-KEY}-code-review.md`
  - Issues are fixed and retested.
  - Do not run another AI review pass unless a fix introduces a new build or test failure; the human reviewer decides whether another review round is needed.

#### Code Review Document

- Location: `docs/code-review/{ISSUE-KEY}-code-review.md`
- Must include: reviewer, date, issues found (with file/line references), severity, fix status.

### 6. Testing (Default Subtask 4)

- Complete code is tested end-to-end.
- **No issues found →** the tester attaches the testing results document, then **assigns the Testing subtask to the Team Lead (nagesh) and transitions it to "Pending Approval"**. The Lead reviews and approves (→ "Approved") or rejects (→ "Rejected").
- **Issues found:**
  - Issues are documented in: `docs/testing/{ISSUE-KEY}-testing-results.md`
  - Issues are fixed → Code Review again → Testing again.
  - Continue until all issues are resolved.
- **Lead approves the Testing subtask → Story is Done.**

#### Testing Results Document

- Location: `docs/testing/{ISSUE-KEY}-testing-results.md`
- Must include: test scenarios, pass/fail, issues found, fix references.

---

## Final Flow (Linear)

```
BRD → Epic → User Story
  → Requirement Generation → Requirement Approval
  → Design Derivation → Design Approval
  → Development Subtasks (from design)
  → Unit Test → Code Coverage
  → Code Review
  → Testing → Testing Approval (Lead)
  → Done
```

---

## Jira Rules

### Default Subtasks (Created for Every User Story)

Every User Story must have these 4 subtasks created at story creation time, regardless of assignment:

| # | Subtask Name | Purpose |
|---|-------------|---------|
| 1 | Requirement Generation — {Story Title} | Requirement document creation and approval |
| 2 | Requirement Design Derivation — {Story Title} | Design document creation and approval |
| 3 | Code Review — {Story Title} | Peer review and issue resolution |
| 4 | Testing — {Story Title} | End-to-end testing and issue resolution |

### Frontend Story Pairing Rule

**For every backend story that exposes REST APIs, a corresponding frontend story must be created as a sibling under the same Epic.**

- Backend story: handles API, service logic, database, validation
- Frontend story: handles the admin/user UI that consumes those APIs
- Both are separate stories with their own 4 default subtasks and development subtasks
- Frontend story is created at the same time as the backend story (not as an afterthought)
- Frontend story naming convention: `Frontend — {Module Name} ({Brief Description})`
- Frontend story depends on the backend story (APIs must exist first)
- This applies to ALL modules: master data, scheduling, approval, reporting, etc.

**Kiro enforcement:** When creating stories from a BRD or design, always create both backend and frontend stories as a pair. Never create a backend-only story without its frontend counterpart.

### Development Subtasks (Created After Design Approval)

After the design is approved, Kiro creates additional subtasks under the same Story at the same level:
- One subtask per development task derived from the design document
- A "Unit Test" subtask
- A "Code Coverage" subtask
- All are Subtask issue type with parent = Story ID

### Assignment Rule

- All subtasks inherit assignment from the parent User Story (per squad rules).
- Work on any subtask begins only after the story is assigned.
- The flow is sequential — each phase is blocked until the previous one is completed/approved.

### Gating Rules

| Gate | Condition to Pass |
|------|-------------------|
| Requirement → Design | Requirement Generation subtask status = "Approved" |
| Design → Development | Design Derivation subtask status = "Approved" |
| Development → Unit Test | All development subtasks complete |
| Unit Test → Code Coverage | Unit tests pass, unit test doc generated |
| Code Coverage → Code Review | Coverage doc generated, meets 80% target |
| Code Review → Testing | Code review clean (no open issues) |
| Testing → Testing Approval | All test scenarios pass, no open issues; results doc attached; assigned to Lead (nagesh); status = "Pending Approval" |
| Testing Approval → Done | Testing subtask status = "Approved" (by Lead) |

---

## Folder Structure for Artifacts

```text
docs/
├── requirements/          # Requirement documents
│   └── {ISSUE-KEY}-{title}-requirements.md
├── design/                # Design documents
│   └── {ISSUE-KEY}-{title}-design.md
├── code-coverage/         # Coverage reports per task
│   └── {ISSUE-KEY}-{task-id}-coverage.md
├── code-review/           # Code review findings
│   └── {ISSUE-KEY}-code-review.md
└── testing/               # Testing results
    └── {ISSUE-KEY}-testing-results.md
```

---

## Kiro Enforcement

**Project Java Version Rule:** The project must target **Java 17 or Java 21 (LTS only)**. Never set the Java version to a non-LTS release (e.g., 22, 23, 24, 25). If the developer's local JDK is newer, the pom.xml `<java.version>` property ensures compilation targets the correct LTS version. Always use Spring Boot 3.x (latest stable GA for Java 21).

Kiro must:

1. **Never start design** unless the Requirement Generation subtask is "Approved".
2. **Never start code development** unless the Design Derivation subtask is "Approved".
3. **Never proceed to code review** until all development subtasks are done and coverage doc exists.
4. **Never mark testing as done** if there are unresolved issues.
5. **Create development subtasks in Jira** under the Story (same level as default subtasks) based on the approved design. Always use `"parent": {"id": "<story_numeric_id>"}`.
6. **Generate coverage documents** after unit testing completes, using the coverage tool's output (the developer runs the tool; Kiro does not compute or estimate coverage).
7. **Document code review issues** in `docs/code-review/` if any are found.
8. **Document testing issues** in `docs/testing/` if any are found.
9. **Always attach documents to Jira subtasks.** When a document (requirement, design, unit test results, code coverage, code review) is generated:
    - Save as `.md` in the repo (for version control).
    - Do NOT create `.txt` copies — only `.md` files.
    - Once the document is reviewed and approved locally by the member, upload the `.md` file to the corresponding Jira subtask in the Attachments section.
    - This is mandatory — no subtask should be submitted for lead review without the `.md` attached in Jira.
9. **Transition subtasks to Done** in Jira immediately after completing the code changes for each subtask. Never leave a completed subtask in "To Do" or "In Progress".
10. **Generate a unit test results document** after completing unit tests. File: `docs/testing/{ISSUE-KEY}-unit-test-results.md`. Build it from the test runner's summary (totals, pass/fail, and failing test names only). Put a short summary (not the full document) in the Unit Test subtask description, add a comment referencing the file, attach the file to the subtask, then transition to Done.
11. **Generate a code coverage document** after unit testing completes. File: `docs/code-coverage/{ISSUE-KEY}-{task-id}-coverage.md`. Build it from the coverage tool's summary report (the developer runs the tool). Must include: actual coverage percentage (line/branch) from the tool, uncovered gaps with plan, and requirement traceability. Do not estimate coverage or list every covered class/method. Put a short summary in the Code Coverage subtask description, add a comment, attach the file, then transition to Done.
12. **Always update Jira status** based on subtask type:
    - **Requirement Generation / Design Derivation subtasks:**
      - Created → **To Do** (default)
      - Document completed, assigned to lead → **Pending Approval**
      - Lead approves → **Approved** (done by lead)
      - Lead rejects → **Rejected** (done by lead)
    - **Development subtasks (from design doc):**
      - Created → **To Do** (default)
      - Code changes completed → **Done**
    - **Code Review subtask:**
      - Created → **To Do** (default)
      - Submitted to reviewer → **Pending Approval**
      - Reviewer approves → **Approved** (done by reviewer)
    - **Testing subtask:**
      - Created → **To Do** (default)
      - All tests pass, no issues, results doc attached, assigned to Lead (nagesh) → **Pending Approval**
      - Lead approves → **Approved** (done by Lead)
      - Lead rejects → **Rejected** (done by Lead) → fix issues → re-review → re-test → resubmit
    - Never leave a ticket in a stale status. If work is done, the ticket must reflect it immediately.
13. **Parent Story status must reflect progress.** If any subtask under a Story moves out of "To Do" (e.g., to In Progress, Pending Approval, Approved, Done), the parent Story must be transitioned to "In Progress" if it's still in "To Do". A Story should never remain in "To Do" once work has begun on any of its subtasks.
13a. **Testing approval = story complete (authoritative completion signal).** A User Story is considered **complete** the moment its **Testing subtask is Approved by the Lead (nagesh)** — regardless of what the parent Story's own status field currently shows. The Testing subtask's "Approved" status is the authoritative signal of completion, because it is the final gate in the workflow (all prior gates — Requirement, Design, Development, Unit Test, Code Coverage, Code Review — must already be satisfied for Testing to have been reached and approved).
    - **Do NOT judge story completion by the parent Story status field alone.** The parent field frequently lags (stuck at "In Progress") even after the Testing subtask is approved. Treating a lagging parent field as "not finished" is incorrect.
    - **When reporting story status,** always check the Testing subtask's status. If Testing = "Approved", report the story as **complete/done**, even if the parent still reads "In Progress".
    - **Kiro enforcement:** Whenever the Testing subtask transitions to "Approved", transition the parent Story to "Done" to keep the field in sync. If you observe an approved Testing subtask under a parent Story that is not yet "Done", transition the parent to "Done" to correct the lag.
14. **Pre-push build verification (mandatory).** Before pushing any code to Git:
    - **Backend:** Run the full Maven build (`mvn clean package`). The build must succeed with zero errors and produce the JAR/WAR package. If the build fails, do NOT push. Fix the issue first.
    - **Frontend:** Run the Vite build (`pnpm build` or `node node_modules/vite/bin/vite.js build`). The build must succeed with zero errors. If it fails, do NOT push.
    - **Tests:** All unit tests must pass before pushing. Do not push code with failing tests.
    - **Kiro enforcement:** Before executing any `git push`, verify the build passes. If it doesn't, refuse the push and report the errors.
15. **Feature branch per story (mandatory).** All development work happens on a feature branch — never directly on `main`.
    - **Branch creation:** When a story is assigned and development begins, create a new branch: `feature/{STORY-KEY}-{short-description}` (e.g., `feature/AID-183-academic-calendar`).
    - **All commits go to the feature branch** during development, unit tests, code review, and testing.
    - **Merge to main only when:** The entire story is complete (all subtasks Done, Code Review approved, Testing passed).
    - **Before merging:** Ask the team member for explicit permission: "Story {KEY} is complete. All subtasks done, review approved, tests passed. Ready to merge to main?"
    - **Only merge after confirmation.** Never push or merge to `main` without explicit user approval.
    - **Delete the feature branch** after successful merge to main.
16. **Story description completeness (mandatory).** When creating a User Story in Jira:
    - The description must cover **all parts** of what the story delivers (scope, acceptance criteria, key behaviors).
    - The description must explicitly list the **BRD point number(s)** the story is derived from (e.g., "BRD Requirements: 9.1, 9.2, 9.3").
    - If a story spans multiple BRD sections, all relevant section/point numbers must be referenced.
    - Kiro must refuse to create a story without BRD traceability in the description.
17. **Mandatory labels on every User Story.** When creating or updating a User Story in Jira, the following labels MUST be applied. No story enters the backlog without all three label categories:
    - **Role label (exactly one required):**
      - `backend` — story involves only server-side work (APIs, services, database, algorithms)
      - `frontend` — story involves only client-side work (UI components, pages, state management)
      - `fullstack` — story involves both backend and frontend work
    - **Feature area label (at least one required):** Identifies which module/domain the story belongs to. Use the applicable label(s) from:
      - `master-data`, `campus-hierarchy`, `course-management`, `faculty-management`, `resource-management`, `room-management`
      - `scheduling-engine`, `timetable-generation`, `scheduling-constraints`, `scheduling-patterns`, `partial-regeneration`, `time-slot-grid`, `academic-calendar`
      - `conflict-detection`, `conflict-resolution`, `conflict-log`, `alternative-suggestions`
      - `approval-workflow`, `multi-level-approval`, `version-management`, `publication`, `timetable-lifecycle`
      - `exam-scheduling`, `seating-plan`, `invigilation`
      - `lab-scheduling`, `batch-splitting`, `block-scheduling`
      - `elective-registration`, `student-portal`, `roster-management`, `waitlist-management`, `add-drop`, `personal-timetable`, `course-catalogue`
      - `leave-management`, `substitution`, `workload-computation`, `violation-detection`
      - `compliance`, `accreditation`, `norm-configuration`, `publication-gating`
      - `multi-campus`, `travel-time-buffer`, `resource-sharing`, `resource-blocking`
      - `notifications`, `multi-channel`, `user-preferences`
      - `calendar-export`, `ical-feed`, `subscription-tracking`, `bulk-export`
      - `reporting`, `analytics`, `room-utilization`, `historical-archive`
      - `rbac`, `authentication`, `security`, `audit-trail`, `access-control`
      - `data-integration`, `enrolment-sync`, `external-systems`, `bulk-import`, `data-export`
    - **Functionality type label (at least one required):** Describes the nature of the work:
      - `crud` — basic create/read/update/delete operations
      - `algorithm` — computational logic, scheduling, optimization
      - `workflow` — multi-step process, state machine, approval chain
      - `real-time` — WebSocket, live updates, instant feedback
      - `analytics` — reporting, dashboards, metrics computation
      - `import-export` — data import, export, synchronization
      - `async-processing` — background jobs, queues, scheduled tasks
      - `validation` — input validation, compliance checks, constraint enforcement
    - **Kiro enforcement:** Refuse to create a User Story without at least one label from each of the three categories (Role, Feature area, Functionality type). If updating an existing story that lacks labels, add them before proceeding with any other work on that story.
