-- V3__create_time_slot_tables.sql
-- Time-slot grid configuration: one grid per campus, slot definitions with day-aware overrides and soft-delete

-- Time Slot Grid (one active grid per campus — PD-61)
CREATE TABLE utms.time_slot_grids (
    id              BIGSERIAL PRIMARY KEY,
    campus_id       BIGINT NOT NULL,
    grid_name       VARCHAR(200) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_time_slot_grids_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id)
);

-- Only one active (non-deleted) grid per campus
CREATE UNIQUE INDEX uq_time_slot_grids_campus ON utms.time_slot_grids(campus_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_time_slot_grids_campus_id ON utms.time_slot_grids(campus_id) WHERE deleted_at IS NULL;

-- Slot Definitions (with applicable_day for day-specific overrides — KD-44, PD-65)
-- applicable_day NULL = applies to ALL days; non-NULL = day-specific override
CREATE TABLE utms.slot_definitions (
    id              BIGSERIAL PRIMARY KEY,
    grid_id         BIGINT NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    slot_type       VARCHAR(20) NOT NULL CHECK (slot_type IN ('TEACHING', 'BREAK', 'LUNCH')),
    applicable_day  VARCHAR(10) NULL CHECK (applicable_day IS NULL OR applicable_day IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    label           VARCHAR(100) NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_slot_definitions_grids FOREIGN KEY (grid_id) REFERENCES utms.time_slot_grids(id),
    CONSTRAINT chk_slot_time_order CHECK (start_time < end_time)
);

CREATE INDEX idx_slot_definitions_grid_id ON utms.slot_definitions(grid_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_slot_definitions_grid_day ON utms.slot_definitions(grid_id, applicable_day) WHERE deleted_at IS NULL;
