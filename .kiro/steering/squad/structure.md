---
inclusion: always
---

# Project Structure & Hierarchy

This file documents the folder hierarchy and how steering files work together. It is project-agnostic and applies to any project using this `.kiro` configuration.

## Hierarchy

- Organization: (defined per project)
- Squad: (defined per project)
- Roles:
  - Backend-Developer
  - Frontend-Developer

## Folder Layout

```text
code/                   # All source code
  <project-name>/      # Project root (Maven/Gradle for backend)
    src/               # Java source
    pom.xml            # Build config

frontend/              # React/frontend app (if applicable)

docs/                  # Project documentation
  brd/                 # BRD files
  requirements/        # Requirement documents (per story)
  design/              # Design documents (per story)
  testing/             # Unit test result documents
  code-coverage/       # Code coverage reports
  code-review/         # Code review findings
  manualWork.md        # Manual work log

.kiro/steering/        # Steering files (this folder)
  workflow.md          # Development workflow & Jira enforcement rules
  brd-to-jira.md      # BRD → Jira breakdown process
  design-derivation.md # Design document generation guide
  requirements-generation.md # Requirements generation guide
  squad/
    squad-rules.md     # Working agreements, review standards, DoD
    product.md         # Product context & goals
    tech.md            # Technology stack & design preferences
    structure.md       # This file — folder hierarchy
  shared/
    jira.md            # Jira integration rules
    git.md             # Git conventions
    mcp.md             # MCP server config
    testing.md         # Testing standards
  role/
    backend/           # Backend coding standards
    frontend/          # Frontend coding standards
```

## Steering File Roles

| File | Purpose |
|------|---------|
| `workflow.md` | Mandatory dev flow, gating rules, Jira status enforcement |
| `story-quality.md` | Guardrails for User Story creation — prevents scope creep, gold-plating, missing ACs |
| `requirement-quality.md` | Guardrails for Requirement Document writing — prevents algorithm lock-in, contradictions, gaps |
| `squad/squad-rules.md` | Working agreements, assignment rules, review standards, DoD |
| `squad/product.md` | Product context, personas, priorities, metrics |
| `squad/tech.md` | Tech stack, libraries, infrastructure, DB conventions |
| `shared/jira.md` | Jira project config, issue types, field mappings, API notes |
| `shared/git.md` | Branch strategy, commit format, PR rules |
| `shared/testing.md` | Test pyramid, naming, coverage requirements |
| `role/backend/` | Backend standards (package structure, layering, coding conventions) |
| `role/frontend/` | Frontend standards (component structure, state management) |
| `brd-to-jira.md` | Process for converting BRD → Epic → Stories → Subtasks |
| `design-derivation.md` | Process for generating design docs from requirements |

## How Kiro Uses These Files

- Load `squad/squad-rules.md` and `workflow.md` for all work in this workspace.
- Load `story-quality.md` when creating, updating, or reviewing User Stories.
- Load `requirement-quality.md` when creating, updating, or reviewing Requirement Documents.
- Include `role/backend/` steering files when working on backend/Java code.
- Include `role/frontend/` steering files when working on frontend code.
- When suggesting new files or code, place them according to the Folder Layout above.
- When generating requirements/design, output files to `docs/requirements/` and `docs/design/`.
- Do not restructure the project unless explicitly asked.

## Docs Folder Convention

| Folder | File Naming | Purpose |
|--------|-------------|---------|
| `docs/requirements/` | `{ISSUE-KEY}-{kebab-title}-requirements.md` | Requirement documents |
| `docs/design/` | `{ISSUE-KEY}-{kebab-title}-design.md` | Design documents |
| `docs/testing/` | `{ISSUE-KEY}-unit-test-results.md` | Unit test results |
| `docs/code-coverage/` | `{ISSUE-KEY}-{subtask-key}-coverage.md` | Coverage reports |
| `docs/code-review/` | `{ISSUE-KEY}-code-review.md` | Code review findings |

## Rules

- All code goes inside `code/<project-name>/` — never at the repo root.
- All documentation goes inside `docs/` — never mixed with code.
- Steering files are reusable across projects — they reference patterns, not specific issue keys or project names.
- Project-specific details (Jira project key, DB name, etc.) go in config files, not steering files.
