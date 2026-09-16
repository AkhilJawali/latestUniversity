-- V2__create_courses_tables.sql
-- Course management tables: courses, course_prerequisites, course_department_links

CREATE TABLE utms.courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(20) NOT NULL,
    department_id BIGINT NOT NULL,
    lecture_hours INTEGER NOT NULL DEFAULT 0 CHECK (lecture_hours >= 0),
    tutorial_hours INTEGER NOT NULL DEFAULT 0 CHECK (tutorial_hours >= 0),
    practical_hours INTEGER NOT NULL DEFAULT 0 CHECK (practical_hours >= 0),
    credits DECIMAL(3,1) NOT NULL CHECK (credits > 0),
    course_type VARCHAR(20) NOT NULL CHECK (course_type IN ('CORE', 'ELECTIVE', 'AUDIT')),
    equipment_tags VARCHAR(2000) DEFAULT '',
    is_cross_listed BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_courses_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT chk_courses_ltp_positive CHECK (lecture_hours + tutorial_hours + practical_hours > 0)
);

CREATE UNIQUE INDEX uq_courses_department_code ON utms.courses(department_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_department_id ON utms.courses(department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_type ON utms.courses(course_type) WHERE deleted_at IS NULL;
-- equipment_tags stored as comma-separated VARCHAR, searched at application level

CREATE TABLE utms.course_prerequisites (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    prerequisite_course_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT fk_cp_prerequisite FOREIGN KEY (prerequisite_course_id) REFERENCES utms.courses(id),
    CONSTRAINT uq_course_prerequisite UNIQUE (course_id, prerequisite_course_id),
    CONSTRAINT chk_no_self_prerequisite CHECK (course_id != prerequisite_course_id)
);

CREATE TABLE utms.course_department_links (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_cdl_course FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT fk_cdl_department FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_course_department_link UNIQUE (course_id, department_id)
);
