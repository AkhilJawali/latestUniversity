-- V8__create_assets_tables.sql
-- Creates the schedulable_assets and asset_availability_windows tables for non-room asset management

CREATE TABLE utms.schedulable_assets (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(200) NOT NULL,
    identifier              VARCHAR(50) NOT NULL,
    asset_type              VARCHAR(50) NOT NULL,
    owning_department_id    BIGINT NOT NULL,
    campus_id               BIGINT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100) NOT NULL,
    updated_by              VARCHAR(100) NOT NULL,
    deleted_at              TIMESTAMP NULL,
    CONSTRAINT fk_schedulable_assets_departments FOREIGN KEY (owning_department_id) REFERENCES utms.departments(id),
    CONSTRAINT fk_schedulable_assets_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id)
);

-- Partial unique on identifier: reusable after soft-delete
CREATE UNIQUE INDEX uq_schedulable_assets_identifier ON utms.schedulable_assets(identifier) WHERE deleted_at IS NULL;

-- Standard indexes
CREATE INDEX idx_schedulable_assets_department_id ON utms.schedulable_assets(owning_department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_schedulable_assets_campus_id ON utms.schedulable_assets(campus_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_schedulable_assets_asset_type ON utms.schedulable_assets(asset_type) WHERE deleted_at IS NULL;

CREATE TABLE utms.asset_availability_windows (
    id              BIGSERIAL PRIMARY KEY,
    asset_id        BIGINT NOT NULL,
    day_of_week     VARCHAR(10) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_asset_availability_windows_assets FOREIGN KEY (asset_id) REFERENCES utms.schedulable_assets(id),
    CONSTRAINT chk_asset_availability_windows_time CHECK (start_time < end_time)
);

-- Index for querying availability by asset and day
CREATE INDEX idx_asset_availability_windows_asset_day ON utms.asset_availability_windows(asset_id, day_of_week) WHERE deleted_at IS NULL;
