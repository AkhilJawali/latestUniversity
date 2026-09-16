# Task 1.3: Set Up Redis for Cache, Pub/Sub, and Job Queues

## Jira Reference
- **Issue Key**: SPTV2-124
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-124
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-121 — Project Setup and Infrastructure
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 1: Project Setup and Infrastructure
- **Complexity**: M
- **Requirements**: 22.3, 15.1
- **Design Reference**: Cache and messaging infrastructure

## Description
- Install ioredis and Bull queue libraries with pinned versions
- Configure Redis connection with environment variables
- Create Bull queue factory for notification and scheduling job queues
- Set up pub/sub channels for real-time conflict detection events

## Dependencies
- **Depends on**: Task 1.1 — SPTV2-122
- **Blocks**: Task 8.1 (conflict detection caching)
- **Cross-role**: No

## Definition of Done
- [ ] Redis connection established
- [ ] Bull queues created and tested
- [ ] Pub/sub channels working
- [ ] Environment-based config
