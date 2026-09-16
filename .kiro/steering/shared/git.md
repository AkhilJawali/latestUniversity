---
inclusion: always
---

# Git Standards

## Branch Strategy
- Main branch: `main` (always deployable)
- Development branch: `develop` (integration branch)
- Feature branches: `feature/<ticket-id>-short-description`
- Bugfix branches: `bugfix/<ticket-id>-short-description`
- Hotfix branches: `hotfix/<ticket-id>-short-description`
- Release branches: `release/<version>`

## Commit Messages
Follow conventional commit format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: feat, fix, docs, style, refactor, perf, test, chore, ci, build

## Pull Requests
- Title must reference the ticket ID
- Description must include: what changed, why, how to test
- Keep PRs small and focused (under 400 lines when possible)
- Squash merge to main for clean history
- Delete branches after merge

## Git Hooks
- Pre-commit: lint and format checks
- Commit-msg: validate conventional commit format
- Pre-push: run unit tests
