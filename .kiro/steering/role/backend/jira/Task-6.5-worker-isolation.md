# Task 6.5: Implement Worker Thread Isolation and Timeout

## Jira Reference
- **Issue Key**: SPTV2-156
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-156
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: M
- **Requirements**: 4.2, 22.1
- **Design Reference**: Engine isolation

## Description
- Spawn scheduling engine in isolated worker thread (worker_threads module)
- Implement 120-second timeout with best-partial-solution return
- Implement progress reporting from worker to main thread
- Handle infeasibility: identify minimal unsatisfiable subset (MUS)
- Return structured EngineError with partial result and constraint report

## Dependencies
- **Depends on**: Task 6.4 — SPTV2-155
- **Blocks**: Task 6.6 (generation orchestrator)
- **Cross-role**: No

## Definition of Done
- [ ] Worker thread isolation working
- [ ] 120s timeout returns partial solution
- [ ] Progress reporting functional
- [ ] MUS identification on infeasibility
