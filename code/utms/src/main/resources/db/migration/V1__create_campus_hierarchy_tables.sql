-- V1__create_campus_hierarchy_tables.sql
-- Creates the foundational campus hierarchy: Campus -> Department -> Program -> Batch -> Section

CREATE SCHEMA IF NOT EXISTS utms;

-- Campus
CREATE TABLE utms.campuses (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    location        VARCHAR(500) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL
);
CREATE UNIQUE INDEX uq_campuses_code ON utms.campuses(code) WHERE deleted_at IS NULL;

-- Department
CREATE TABLE utms.departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    campus_id       BIGINT NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,
    CONSTRAINT fk_departments_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id)
);
CREATE UNIQUE INDEX uq_departments_campus_code ON utms.departments(campus_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_departments_campus_id ON utms.departments(campus_id) WHERE deleted_at IS NULL;

-- Program
CREATE TABLE utms.programs (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    code                VARCHAR(20) NOT NULL,
    department_id       BIGINT NOT NULL,
    duration_semesters  INTEGER NOT NULL,
    degree_type         VARCHAR(50) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMP NULL,
    CONSTRAINT fk_programs_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id)
);
CREATE UNIQUE INDEX uq_programs_department_code ON utms.programs(department_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_programs_department_id ON utms.programs(department_id) WHERE deleted_at IS NULL;

-- Batch
CREATE TABLE utms.batches (
    id                BIGSERIAL PRIMARY KEY,
    year_identifier   VARCHAR(20) NOT NULL,
    strength          INTEGER NOT NULL CHECK (strength > 0),
    elective_basket   VARCHAR(200),
    program_id        BIGINT NOT NULL,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    deleted_at        TIMESTAMP NULL,
    CONSTRAINT fk_batches_programs FOREIGN KEY (program_id) REFERENCES utms.programs(id)
);
CREATE INDEX idx_batches_program_id ON utms.batches(program_id) WHERE deleted_at IS NULL;

-- Section
CREATE TABLE utms.sections (
    id                  BIGSERIAL PRIMARY KEY,
    section_identifier  VARCHAR(10) NOT NULL,
    sub_strength        INTEGER CHECK (sub_strength IS NULL OR sub_strength > 0),
    batch_id            BIGINT NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    deleted_at          TIMESTAMP NULL,
    CONSTRAINT fk_sections_batches FOREIGN KEY (batch_id) REFERENCES utms.batches(id)
);
CREATE UNIQUE INDEX uq_sections_batch_identifier ON utms.sections(batch_id, section_identifier) WHERE deleted_at IS NULL;
CREATE INDEX idx_sections_batch_id ON utms.sections(batch_id) WHERE deleted_at IS NULL;
