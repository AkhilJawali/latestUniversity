# Task 1.4: Set Up Express API Server with WebSocket Support

## Jira Reference
- **Issue Key**: SPTV2-125
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-125
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-121 — Project Setup and Infrastructure
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 1: Project Setup and Infrastructure
- **Complexity**: L
- **Requirements**: 22.3, 11.2, 20.3
- **Design Reference**: API layer setup

## Description
- Create Express application with middleware stack (helmet, cors, compression, rate-limiting)
- Configure WebSocket server (ws library) for real-time conflict feedback
- Set up request validation middleware (zod schemas)
- Configure structured JSON logging (pino) with security event categories
- Implement global error handler that never exposes internals

## Dependencies
- **Depends on**: Task 1.2 — SPTV2-123, Task 1.3 — SPTV2-124
- **Blocks**: Task 4.11, 8.4
- **Cross-role**: No

## Definition of Done
- [ ] Express server starts and responds
- [ ] WebSocket connections working
- [ ] Validation middleware rejects invalid input
- [ ] Error handler returns safe responses
- [ ] Structured logging operational
