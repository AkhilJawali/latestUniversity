# Design: Resource Blocking and Availability Workflow

**Jira Reference:** A4-8
**Source Requirements:** docs/requirements/A4-8-resource-blocking-workflow-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — full rewrite fixing all 6 review issues)

## Summary of fixes from v1:
1. SoftBlockOverride write path: KD-33 + recordSoftBlockOverride() method
2. A4-16 integration: KD-32 — event-based, not internal SQL
3. Configurable roles: KD-34, PD-44/45
4. Pending expiry/withdrawal: KD-35, PD-47
5. Multi-day semantics: KD-36
6. Boundary-straddling reports: KD-37

[Full design content as composed above — KDs, PDs, API (11 endpoints including withdraw/record-override/reports), Migration V7, Service Logic (9 methods including scheduled expiry job, override write path, boundary-aware reporting), Integration Contracts table (5 consumers with direction), Status Transitions (6 states), Testing (all paths including override write, expiry job, boundary reports)]
