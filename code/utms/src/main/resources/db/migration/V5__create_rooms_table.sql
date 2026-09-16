-- V5__create_rooms_table.sql
-- Creates the rooms table for room and lab master data management

CREATE TABLE utms.rooms (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    campus_id       BIGINT NOT NULL,
    capacity        INTEGER NOT NULL CHECK (capacity > 0),
    room_type       VARCHAR(30) NOT NULL CHECK (room_type IN ('CLASSROOM', 'LAB', 'SEMINAR_HALL', 'AUDITORIUM')),
    equipment_tags  VARCHAR(2000) NOT NULL DEFAULT '',
    building        VARCHAR(100),
    floor           VARCHAR(20),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_rooms_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id)
);

-- Partial unique on (campus_id, code) for active rooms only
CREATE UNIQUE INDEX uq_rooms_campus_code ON utms.rooms(campus_id, code) WHERE deleted_at IS NULL;

-- Standard indexes
CREATE INDEX idx_rooms_campus_id ON utms.rooms(campus_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_rooms_room_type ON utms.rooms(room_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_rooms_capacity ON utms.rooms(capacity) WHERE deleted_at IS NULL;

-- equipment_tags stored as comma-separated VARCHAR, searched at application level
