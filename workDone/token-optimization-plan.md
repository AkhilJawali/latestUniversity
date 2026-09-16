# Token Optimization Plan — Kiro AIDLC

**Project:** UTMS / Jira A4
**Created:** 2026-09-08
**Status:** Living document — add findings from ongoing R&D below.

---

## Baseline (from `tokenConsumption-2.md` / `daily-work-summary.txt`)

| Story | Total credits |
|-------|---------------|
| A4-410 | ~128.09 |
| A4-415 | ~79.05 |
| A4-420 | ~116.20 |
| A4-425 | ~122.75 |

**Key observation:** ~70% of every story is consumed by a single step —
*design approval + create dev sub-tasks + code review* (54–94 credits each).
Requirement, design and testing steps are light (2–13 credits).

That one step is where optimization effort should go. Backend stories will
amplify it further (more layers: controller → service → repository → DTO).

**Target:** ~120 credits/story → **~40–50 credits/story**.

---

## 1. Stop paying for the same design 8 times
**Priority: HIGH — biggest single win**

A4-410 / 415 / 420 / 425 / 430 / 435 / 440 / 445 are all CRUD master data.
Each one currently gets a full requirement doc + design doc + from-scratch
code generation. The work is ~85% identical every time.

**Do instead:**
- Pick the cleanest finished story (A4-415) as the **reference pattern**.
- For each remaining story, prompt: *"Implement exactly like A4-415 — same
  structure, same shared table component, same hooks. Only the entity and its
  fields differ."*
- Write **one** design doc for the whole master-data group, not one per story.

**Expected:** ~80 → ~25–35 credits per remaining story.

---

## 2. Audit the hooks
**Priority: HIGH — silent, invisible cost**

Agent hooks that fire on save / on file-change trigger a **full agent run every
time a file is touched**. During an active dev session that is dozens of runs
that never show up as a "prompt" in the token log.

**Do instead:**
- Change on-save hooks → **manual trigger**, run once at the end of a story.
- Narrow file patterns: `src/features/faculty/**` instead of `**/*.ts`.
- Delete any hook that duplicates what the code-review step already does —
  otherwise the same check is paid for twice.

---

## 3. Cap the review → fix → review loop
**Priority: HIGH**

The heavy step "runs review, fixes issues, produces coverage docs" — that is a
loop that can run 3–4 rounds before it settles. Each round re-reads the files.

**Do instead:** state explicitly — **one review pass, apply fixes, done.**
A second pass only if something actually broke.

---

## 4. Don't make the AI write coverage / test reports
**Priority: MEDIUM**

Run the coverage tool yourself (`npm run test -- --coverage`) and paste only
the failing lines. Having the model re-narrate a coverage table in prose is
pure waste — the tool already prints it.

---

## 5. Shrink what gets loaded into every prompt
**Priority: MEDIUM**

- **Steering files** (`.kiro/steering/`): set inclusion to `fileMatch` or
  manual, not always-on. An always-included steering file rides along in
  *every single prompt* of the session.
- Keep requirement + design docs **short** — bullets, not paragraphs. They get
  re-read on every dev sub-task.
- Split large components. A 900-line file in context for a 10-line change
  means paying for all 900 lines.

---

## 6. Review only the diff
**Priority: MEDIUM**

Code review should look at changed files only, not the whole feature folder.
Say this explicitly in the review prompt.

---

## 7. Backend-specific: define the API contract once
**Priority: HIGH (before backend work starts)**

Define endpoints, request/response shapes and error format for **all** entities
up front, in one document. Then generate controllers/services against that
contract.

Without this, each backend story re-derives the same repository/service/DTO
layering from scratch — this is where backend token cost explodes relative to
frontend.

---

## Action checklist

- [ ] Nominate A4-415 as the reference pattern story
- [ ] Write one shared master-data design doc covering A4-430/435/440/445
- [ ] Audit `.kiro/hooks/` — switch on-save hooks to manual
- [ ] Narrow hook file patterns
- [ ] Set steering files to `fileMatch` / manual inclusion
- [ ] Add "one review pass only" to the review prompt
- [ ] Add "review changed files only" to the review prompt
- [ ] Stop requesting generated coverage docs
- [ ] Draft the backend API contract before the first backend story
- [ ] Re-measure on A4-430 and compare against the ~120 baseline

---

## Measurements

| Story | Credits | Notes |
|-------|---------|-------|
| A4-430 | TBD | first story after optimization |

---

## R&D findings

_(Additions from ongoing research go here.)_
