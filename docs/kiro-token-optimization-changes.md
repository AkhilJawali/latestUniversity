# Kiro Token Optimization — What Changed

2026-09-17

## In one minute

Kiro now loads about **74% less text into every prompt**: roughly 42 KB instead of 162 KB.

- **The problem:** every prompt carried large files that had nothing to do with the task. Hooks also started extra AI runs on every save and every message, and code reviews could repeat without limit.
- **The fix:** load files only when they're needed, run hooks only on demand, and cap AI reviews at one pass over changed code.
- **Result:** fewer credits per story, with the same gates, approvals and Jira steps.

## The 6 new rules

These live in one new file, `steering/shared/token-efficiency.md`. The rules apply to any project.

| # | Rule | In plain words | Why it saves tokens |
| --- | --- | --- | --- |
| 1 | No automatic AI runs while coding | Saving a file doesn't wake the AI. Review happens once, at the end. | Stops an agent run on every save |
| 2 | One AI review pass | Review once, fix, done. Review again only if a fix breaks the build or tests. | Ends open-ended review/fix loops |
| 3 | Review only the diff | The AI looks only at what this story changed. | No re-reading of whole folders |
| 4 | Coverage comes from tools | You run the test and coverage tools; the AI copies the summary. | No long tables or guessed numbers |
| 5 | Load only what's needed | Read the files the task touches, and only the parts that matter. | Smaller context on every call |
| 6 | Keep files small | Split big components into page, table, form, hook, API and validation files. | Later edits load one small file |

## What changed, file by file

### When files load

Most of the savings come from here. Only the loading setting changed; the text inside these files did not.

| File | Before | Now |
| --- | --- | --- |
| 93 Jira task files (`role/*/jira/`) | Likely loaded on every prompt (no setting) | Only when you mention one |
| `core-principles.md` | Loaded on every prompt | Only for story, requirement or design work |
| `story-quality.md` | Every prompt | Only for story work |
| `requirement-quality.md` | Every prompt | Only for requirement docs |
| `shared/testing.md` | Every prompt | Only when a test file is open |
| `shared/mcp.md` | Every prompt | Only when `mcp.json` is open |

### Rules text

- **`workflow.md`:**
    - Code review is one AI pass on changed files, and the human reviewer decides on more rounds.
    - Coverage and test-result docs are built from tool output.
    - Jira descriptions get a short summary, not the full document.
- **`squad-rules.md`:** the AI review looks only at the diff, does one pass, reports real issues only, and doesn't refactor unrelated code.
- **`structure.md`:** lists the new `token-efficiency.md` file.

### Hooks

| Hook | Before | Now |
| --- | --- | --- |
| AI code review | Ran the AI on every file save | Manual. One review pass on changed files. |
| Workflow phase guide | Ran on every message you sent | Manual. Its review and coverage steps now follow the new rules. |
| Doc self-check | Ran before every file write, including code | Skips any file outside `docs/` |
| Jira rules check | Ran before Jira actions | No change |

## What did not change

- **Requirement and design doc process:** the steps, templates and checklists (P5, P6) are exactly as before.
- **Gates and approvals:** requirement approval, design approval, lead review and the Testing approval all still apply.
- **Jira steps:** subtasks, assignment, status updates and attachments work the same way.
- **Coding standards:** the backend and frontend standards files are untouched.
- **Project-specific ideas left out:** the shared CRUD pattern, shared design and API contract from the plan only fit this project, so they weren't added.

## What the team does now

**Do**

- Finish the story's code, then run build, lint and tests yourself.
- Run the coverage tool yourself and give Kiro the summary.
- Start the AI code review manually, once.
- When a test fails, paste only the failing test name, expected vs actual values, and the key error lines.
- Split big components into small, focused files.

**Don't**

- Ask Kiro to "review the whole feature" or "review again until clean".
- Ask Kiro to write coverage tables or guess coverage numbers.
- Paste full test logs or whole folders into the chat.

## To check in Kiro

- [ ] Confirm the two hooks now set to `"trigger": "Manual"` still appear in Kiro and can be run by hand. Your Kiro version may name the manual trigger differently.
- [ ] On the next story, check that requirement and design work still loads `core-principles.md` and the quality rules automatically.
- [ ] Compare the next story's credit usage with the old baseline of about 120 credits per story.

`workflow.md` (about 20 KB) still loads on every prompt because it holds the gating rules. Trimming it is the next possible saving.
