# Task 2.1: Campus Schema

## Jira Reference
- **Issue Key**: SPTV2-129
- **URL**: https://akhiljawali.atlassian.net/browse/SPTV2-129
- **Epic**: SPTV2-120 — UTMS
- **Parent Story**: SPTV2-128 — Database Schema and Migrations
- **Status**: Done

## Task Details
- **Role**: backend
- **Phase**: Phase 2: Database Schema and Migrations
- **Complexity**: M
- **Requirements**: 1.1, 1.2, 20.2
- **Design Reference**: Database schema

## Description
- Create tables: campuses, departments, programs, batches, sections
- Define foreign key constraints enforcing parent-child hierarchy
- Add composite indexes for common query patterns
- Apply RLS policies scoped to campus/department

## Dependencies
- **Depends on**: Task 1.2 — SPTV2-123
- **Blocks**: Task 4.1 (campus CRUD service)
- **Cross-role**: No

## Definition of Done
- [ ] Migration file created and runs successfully
- [ ] Indexes created
- [ ] Foreign keys properly named
- [ ] Reversible migration
