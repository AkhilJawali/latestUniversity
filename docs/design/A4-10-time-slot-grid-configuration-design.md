# Design: Time-Slot Grid Configuration

**Jira Reference:** A4-10
**Source Requirements:** docs/requirements/A4-10-time-slot-grid-configuration-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes: overlap vs day-override logic, slot soft-delete, deletion guard honest)

## Key Decisions

KD-41: Day-aware overlap (ALL-days + FRIDAY = override not overlap). KD-42: Slots soft-deleted (not hard). KD-43: Removal guard honest (blocks if sessions ref). KD-44: applicable_day column for day-specific overrides.

## Provisional Decisions

PD-61 (one grid per campus), PD-62 (fixed for semester), PD-63 (removal blocked if sessions), PD-64 (old grids soft-deleted), PD-65 (day-specific via applicable_day), PD-66 (any positive duration allowed).

## Migration: V9 — time_slot_grids (unique per campus) + slot_definitions (with applicable_day, soft-delete).

## Service Logic

Overlap validation: day-scope-aware (NULL vs NULL = overlap; NULL vs FRIDAY = no overlap/override; FRIDAY vs FRIDAY = overlap). Slot removal: soft-delete + session ref guard. Grid resolution: getEffectiveSlotsForDay() merges day-specific overrides with ALL-days slots.

## Traceability + Testing

All FRs and HCs mapped. Tests: overlap validator (3 cases per KD-41), soft-delete slots, session-blocking guard, effective-slots resolution, any-duration acceptance.
