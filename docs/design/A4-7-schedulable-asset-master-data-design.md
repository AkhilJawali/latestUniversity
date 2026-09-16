# Design: Schedulable Asset Master Data Management

**Jira Reference:** A4-7
**Source Requirements:** docs/requirements/A4-7-schedulable-asset-master-data-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes: calendar in create DTO, availability search respects blocks, soft-delete paradox)

## 1. Overview

Non-room schedulable asset CRUD (movable equipment, projector sets, sports facilities) with owning department, campus, and availability calendar. Provides the inventory that resource blocking (A4-8) operates on.

## Key Decisions

| # | Decision | Rationale | Interaction |
|---|---|---|---|
| KD-28 | Calendar included in CREATE DTO (optional). Default=always available. | FR-5.1 + FR-1.1 list it. v1 had no field — false traceability. | — |
| KD-29 | Search RESPECTS active blocks. Blocked assets show as BLOCKED not AVAILABLE. | v1 ignored blocks — misleading. | Depends on A4-8 data. |
| KD-30 | Historical block records do NOT block soft-delete. Active blocks DO. | Same fix as A4-6 KD-23. | Consistent. |
| KD-31 | Calendar = child rows (AssetAvailabilityWindow). Outside calendar = unavailable. No calendar = always available. | Consistent with A4-5 pattern. | AND logic with blocks (PD-41). |

## Provisional Decisions

PD-38 (identifier reusable partial unique), PD-39 (type predefined), PD-40 (soft-delete), PD-41 (calendar AND not-blocked), PD-42 (not session-assigned Phase 1), PD-43 (single campus).

## API

POST/GET/PUT/DELETE /api/v1/assets. POST includes availabilityWindows[]. GET supports availableAt filter (checks calendar + blocks).

## Migration: V6__create_assets_tables.sql

schedulable_assets + asset_availability_windows tables. Partial unique on identifier.

## Service Logic

create: validate + persist asset + calendar windows + audit.
update: validate + replace windows + audit.
delete: active-only ref check (KD-30) + soft-delete + audit.
isAssetAvailable: within calendar (if defined) AND not blocked.

## Traceability

FR-1.1 (calendar in DTO — KD-28), FR-2.2 (search respects blocks — KD-29), FR-4.1 (active-only deletion — KD-30), FR-5.1-5.4 (calendar CRUD). All HCs mapped.
