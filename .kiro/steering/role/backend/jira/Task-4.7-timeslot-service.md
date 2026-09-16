# Task 4.7: Implement Time-Slot Grid Service

## Jira Reference
- **Issue Key**: SPTV2-146
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-146
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-139 — Master Data Module
- **Status**: To Do

## Task Details
- **Role**: backend
- **Phase**: Phase 4: Master Data Module
- **Complexity**: M
- **Requirements**: 3.1, 3.2, 3.3
- **Design Reference**: Master data services

## Description
- Create TimeSlotGridService with configurable slots per campus
- Support mixed slot durations (60, 90, 180 minutes)
- Implement non-overlap validation (reject overlapping slots, validate daily bounds)

## Dependencies
- **Depends on**: Task 2.4 — SPTV2-132
- **Blocks**: Task 6.6 (generation orchestrator)
- **Cross-role**: No

## Definition of Done
- [ ] Slot grid CRUD per campus
- [ ] Mixed durations supported
- [ ] Non-overlap validation enforced
