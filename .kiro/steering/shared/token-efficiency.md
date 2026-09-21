---
inclusion: always
---

# Token Efficiency Rules

These rules limit token usage without changing the workflow gates. If a rule here conflicts with a step that only produces extra AI output (repeated passes, narrated reports, reading more than needed), this file wins. Gates, approvals, and Jira updates stay as defined in `workflow.md`.

## T1: No Automatic Agent Runs While Coding
- Do not start agent runs when a file is saved or edited during active development.
- Run validation and review once, after the story's implementation is complete.
- Hooks must be manual, or narrowly scoped (a specific folder or path pattern, not `**/*.<ext>`).
- No hook may duplicate the Code Review step.

## T2: One AI Review Pass
- Before the AI review, the developer runs build, lint, and tests locally.
- Run one AI review pass. Report only real issues: functional bugs, incorrect API usage, security issues, broken logic, missing validation, and obvious regressions.
- Apply fixes directly. Do not refactor unrelated files. Do not rewrite correct code.
- Do not run a second review pass unless a fix causes a new build or test failure. If one is needed, check only that failure.
- Style nits and optional suggestions do not trigger another pass.

## T3: Review Only the Diff
- The source of truth for a review is `git diff` against the story's base branch, or the list of changed files.
- Do not review unchanged files or unrelated feature folders.
- Open surrounding code only when a change can't be judged without it, and read only the relevant section.

## T4: Coverage and Test Results Come from Tools
- The developer runs the coverage and test tools (for example `mvn test jacoco:report` or `pnpm test --coverage`).
- Build coverage and test-result documents from the tool's summary output. Do not estimate, recompute, or narrate per-class or per-line tables.
- When a test fails, give the AI only the failing test's name, expected vs actual values, and the relevant stack lines, not the full log.

## T5: Load Only the Context You Need
- Read only the files the task touches. Use search, then targeted line ranges, instead of whole large files.
- Do not re-read a file you just edited, or a file already in context, unless it changed.
- Do not load entire folders, the whole codebase, or unrelated steering files.
- Keep chat replies short: state what changed and what's left. Do not repeat document content back into chat.
- Steering files should use `fileMatch`, `auto`, or `manual` inclusion. Use `always` only for short rules that apply to every task.

## T6: Keep Source Files Small and Focused
- When a component or class grows large (roughly more than 150–200 lines, or more than one responsibility), split it into focused files: page/container, table/list, form, hook/service, API client, validation, and types/DTOs.
- Keep each change confined to the files it concerns, so later edits and reviews load only those files.
