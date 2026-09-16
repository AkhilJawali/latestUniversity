# Task 6.6: Implement Generation Orchestrator and API

## Jira Reference
- **Issue Key**: SPTV2-157
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-157
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-151 — Scheduling Engine
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 6: Scheduling Engine
- **Complexity**: L
- **Requirements**: 4.1, 4.5, 2.3, 3.4, 12.3, 12.4
- **Design Reference**: Engine orchestration

## Description
- Create SchedulingEngineService that coordinates pre-processing, generation, and post-processing
- Load master data, constraints, locked slots from database
- Apply holiday exclusion from academic calendar
- Apply campus-specific time-slot grid conformance
- Implement partial re-generation (subset only, preserve approved sections)
- Output feasibility score, quality score, unresolved violations list

## Dependencies
- **Depends on**: Task 6.5 — SPTV2-156
- **Blocks**: Task 18.2 (drag-drop editor)
- **Cross-role**: Yes — blocks SPTV2-216 (18.2 drag-drop editor)

## Definition of Done
- [ ] End-to-end generation pipeline working
- [ ] Holiday exclusion applied
- [ ] Grid conformance enforced
- [ ] Partial re-generation supported
- [ ] Feasibility/quality scores output
