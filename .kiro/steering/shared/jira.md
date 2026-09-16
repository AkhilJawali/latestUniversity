---
inclusion: manual
---

# Jira Integration Steering

## Connection Details
- **Instance**: (set per project — e.g., https://yourorg.atlassian.net)
- **Project Key**: (set per project — e.g., `AID`, `SPT`)

## Issue Type Hierarchy

```
Epic
└── Story (≤ 8 story points, testable)
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

- **4 default subtasks** are created with each Story.
- **Development subtasks** are created after the design is approved, at the same level.
- Use **Bug** only for bugfix specs derived from a bugfix workflow.

## Field Mapping

### Epic
| Spec Element | Jira Field |
|---|---|
| Feature name (kebab-case → Title Case) | `summary` |
| Introduction section of requirements | `description` |
| `High` for core features, `Medium` for enhancements | `priority` |

### Story
| Spec Element | Jira Field |
|---|---|
| Requirement title | `summary` |
| Full User Story sentence | `description` (first line) |
| Acceptance Criteria as numbered list | `description` (body) |
| Parent Epic | `parent` |

### Subtask
| Spec Element | Jira Field |
|---|---|
| Task description | `summary` |
| Full description | `description` |
| Parent Story | `parent` |

## Labels Convention

| Source | Labels |
|---|---|
| Requirement derived from BRD | `brd-derived`, `requirement` |
| Acceptance Criterion | `acceptance-criterion` |
| Technical task | `tech-task` |
| Bugfix spec | `bugfix` |

## Priority Mapping

| Spec Priority | Jira Priority |
|---|---|
| P0 (blocking / auth / data integrity) | High |
| P1 (core feature) | Medium |
| P2 (enhancement / compatibility) | Low |
| Unspecified | Medium |

## Workflow
- Tickets move through: To Do → In Progress → Pending Approval → Approved → Done
- Every code change must reference a Jira ticket
- Ticket status must be updated when work begins and completes

## Linking
- Commits reference ticket ID in the message
- PRs reference ticket ID in the title
- Branch names include the ticket ID

## Sprint Practices
- Sprint duration: 2 weeks
- Sprint goals are defined at planning
- Unfinished work returns to backlog with notes

## Known Jira Limitation — Parent Field

For team-managed projects, subtask creation requires **numeric ID** (not key) for the parent:

- **Stories under Epic:** `customFields: { "parent": { "key": "<EPIC-KEY>" } }` — works.
- **Subtasks under Story:** `customFields: { "parent": { "id": "<numeric_id>" } }` — key format fails.

Always get the numeric ID from the Story creation response and use it for subtask creation.

## Do Not

- Do not create duplicate issues if the Epic already exists — check for existing issues with the same summary before creating.
- Do not change the project key unless the user explicitly specifies a different one.
- Do not create issues for the Glossary section or non-requirement sections of requirements.
- Do not invent requirements or acceptance criteria not present in the spec files.
