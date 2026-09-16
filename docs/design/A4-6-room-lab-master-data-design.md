# Design: Room and Lab Master Data Management

**Jira Reference:** A4-6
**Source Requirements:** docs/requirements/A4-6-room-lab-master-data-requirements.md
**Application:** Existing (UTMS monolith)
**Stack:** Java 21 (LTS) · Spring Boot 3.x (latest stable GA) · Maven
**Generated:** 2026-08-25 (v2 — fixes: soft-delete paradox resolved, capacity/equipment changes emit persisted events)

## 1. Overview

Room/Lab master data CRUD with capacity, type, equipment tags, and location. Consumed by scheduling (assignment), conflict detection (double-booking, capacity mismatch), resource blocking (A4-8), shared booking (A4-36), utilization reporting (A4-41).

## Key Decisions

KD-23: Historical refs do NOT block soft-delete (only active refs block). KD-24: Capacity/equipment changes emit persisted events consumed by A4-16. KD-25: Case-sensitive tags (consistent with A4-3). KD-26: Building=text field Phase 1. KD-27: Availability checks sessions AND blocks.

## Provisional Decisions

PD-31 (soft-delete), PD-32 (code per campus), PD-33 (building text), PD-34 (type change + event), PD-35 (tags free-form), PD-36 (ADMIN role for facilities), PD-37 (floor VARCHAR).

## API: POST/GET/PUT/DELETE /api/v1/rooms + GET /rooms/availability

## Migration: V5__create_rooms_table.sql

rooms table with capacity CHECK>0, room_type CHECK enum, equipment_tags TEXT[], partial unique (campus_id, code), GIN index on tags.

## Service Logic

create: validate campus + code unique + capacity + type → persist → audit.
update: detect changes → persist → emit RoomCapacityChangedEvent / RoomEquipmentChangedEvent / RoomTypeChangedEvent → audit.
delete: check ACTIVE refs only (sessions, blocks, bookings — NOT historical per KD-23) → soft-delete → audit.
availability: check sessions + blocks → return FREE/OCCUPIED/BLOCKED per room.

## Deletion Checks (KD-23)

Active sessions: blocks. Active blocks: blocks. Active bookings: blocks. Historical: does NOT block.

## Traceability

All FRs (1.1-1.3, 2.1-2.4, 3.1-3.5, 4.1-4.3, 5.1-5.4), all HCs (1-6) mapped. Events for FR-3.3/3.4 (not transient warnings).

## Testing

DELETE with historical-only refs → 200 (paradox test). PUT capacity decrease → event. GIN tag search. Availability statuses.
