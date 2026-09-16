-- V3: Faculty tables (faculty, faculty_competencies, faculty_campus_associations)

CREATE TABLE utms.faculty (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    identifier VARCHAR(50) NOT NULL UNIQUE,
    designation VARCHAR(50) NOT NULL,
    qualification VARCHAR(500) NOT NULL,
    home_department_id BIGINT NOT NULL,
    min_weekly_load DECIMAL(4,1) CHECK (min_weekly_load IS NULL OR min_weekly_load >= 0),
    max_weekly_load DECIMAL(4,1) CHECK (max_weekly_load IS NULL OR max_weekly_load > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_faculty_departments FOREIGN KEY (home_department_id) REFERENCES utms.departments(id),
    CONSTRAINT chk_faculty_load_order CHECK (min_weekly_load IS NULL OR max_weekly_load IS NULL OR min_weekly_load <= max_weekly_load)
);

CREATE INDEX idx_faculty_department_id ON utms.faculty(home_department_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_faculty_designation ON utms.faculty(designation) WHERE deleted_at IS NULL;

CREATE TABLE utms.faculty_competencies (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_fc_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT fk_fc_course FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT uq_faculty_competency UNIQUE (faculty_id, course_id)
);

CREATE INDEX idx_fc_faculty_id ON utms.faculty_competencies(faculty_id);
CREATE INDEX idx_fc_course_id ON utms.faculty_competencies(course_id);

CREATE TABLE utms.faculty_campus_associations (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    campus_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_fca_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT fk_fca_campus FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_faculty_campus UNIQUE (faculty_id, campus_id)
);

CREATE INDEX idx_fca_faculty_id ON utms.faculty_campus_associations(faculty_id);
CREATE INDEX idx_fca_campus_id ON utms.faculty_campus_associations(campus_id);
