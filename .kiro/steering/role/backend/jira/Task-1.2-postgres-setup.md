# Task 1.2: Set Up PostgreSQL Database with Migrations Framework

## Jira Reference
- **Issue Key**: SPTV2-123
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-123
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-121 — Project Setup and Infrastructure
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 1: Project Setup and Infrastructure
- **Complexity**: M
- **Requirements**: 20.2, 22.4
- **Design Reference**: Database infrastructure

## Description
- Install and configure Knex.js (or TypeORM) with PostgreSQL driver
- Create initial migration for extension setup (uuid-ossp, pgcrypto)
- Configure connection pooling and environment-based config
- Set up row-level security infrastructure (app.current_user_campus, app.current_user_dept settings)

## Dependencies
- **Depends on**: Task 1.1 — SPTV2-122
- **Blocks**: Task 2.x (all schema migrations)
- **Cross-role**: No

## Definition of Done
- [ ] PostgreSQL connection established
- [ ] Migration framework running
- [ ] RLS infrastructure configured
- [ ] Connection pooling working
