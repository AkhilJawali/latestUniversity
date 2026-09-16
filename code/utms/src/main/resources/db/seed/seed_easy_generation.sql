-- ============================================================================
-- UTMS — Minimal GUARANTEED-FEASIBLE seed for testing timetable generation.
-- Small footprint so the engine always produces a fully-placed (COMPLETE) draft:
--   1 campus, 1 department, 1 program, 1 batch (+1 section),
--   2 lecture-only courses (3 L-hours each), 2 faculty (one per course),
--   2 rooms, FIVE_DAY pattern, 6 teaching slots/day, L/T/P derivation rules.
-- Heuristic loader pairs 2 courses x 1 batch = 2 assignments => 6 lecture sessions.
-- 6 sessions vs 30 teaching slots x 2 rooms  =>  trivially feasible.
--
-- High IDs (>=100) so this never collides with seed_demo_data.sql.
-- Generate with: { "departmentId": 110, "semester": "ODD", "academicYear": "2025-26" }
--
-- Run AFTER Flyway migrations. Safe to re-run (cleans its own rows first).
-- ============================================================================

-- Clean previous runs of THIS fixture (reverse FK order), keyed by created_by tag.
DELETE FROM utms.institution_common_slots      WHERE created_by = 'easy-seed';
DELETE FROM utms.soft_constraint_weights       WHERE created_by = 'easy-seed';
DELETE FROM utms.session_derivation_rules      WHERE created_by = 'easy-seed';
DELETE FROM utms.slot_definitions              WHERE created_by = 'easy-seed';
DELETE FROM utms.time_slot_grids               WHERE created_by = 'easy-seed';
DELETE FROM utms.working_day_patterns          WHERE created_by = 'easy-seed';
DELETE FROM utms.academic_calendars            WHERE created_by = 'easy-seed';
DELETE FROM utms.rooms                         WHERE created_by = 'easy-seed';
DELETE FROM utms.faculty_competencies          WHERE created_by = 'easy-seed';
DELETE FROM utms.faculty                       WHERE created_by = 'easy-seed';
DELETE FROM utms.courses                        WHERE created_by = 'easy-seed';
DELETE FROM utms.sections                       WHERE created_by = 'easy-seed';
DELETE FROM utms.batches                        WHERE created_by = 'easy-seed';
DELETE FROM utms.programs                        WHERE created_by = 'easy-seed';
DELETE FROM utms.departments                     WHERE created_by = 'easy-seed';
DELETE FROM utms.campuses                        WHERE created_by = 'easy-seed';

-- 1. Campus
INSERT INTO utms.campuses (id, name, code, location, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(100, 'Test Campus', 'TST', 'Test City', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 2. Department (belongs to campus 100)
INSERT INTO utms.departments (id, name, code, campus_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(110, 'Test Department', 'TSTD', 100, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 3. Program (belongs to department 110)
INSERT INTO utms.programs (id, name, code, department_id, duration_semesters, degree_type, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(120, 'B.Tech Test Program', 'BTST', 110, 8, 'B.TECH', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 4. Batch (belongs to program 120) — small strength so any room fits
INSERT INTO utms.batches (id, year_identifier, strength, elective_basket, program_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(130, '2025-26', 30, NULL, 120, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 5. Section (belongs to batch 130)
INSERT INTO utms.sections (id, section_identifier, sub_strength, batch_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(140, 'A', 30, 130, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 6. Courses — LECTURE ONLY (no tutorial/practical) to avoid lab/equipment/block constraints.
INSERT INTO utms.courses (id, name, code, department_id, lecture_hours, tutorial_hours, practical_hours, credits, course_type, equipment_tags, is_cross_listed, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(150, 'Test Course One', 'TST101', 110, 3, 0, 0, 3.0, 'CORE', '', FALSE, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(151, 'Test Course Two', 'TST102', 110, 3, 0, 0, 3.0, 'CORE', '', FALSE, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 7. Faculty — one per course, generous weekly load, home dept 110.
INSERT INTO utms.faculty (id, name, identifier, designation, qualification, home_department_id, min_weekly_load, max_weekly_load, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(160, 'Dr. Test Alpha', 'FAC-TST-001', 'PROFESSOR', 'Ph.D Test', 110, 0.0, 40.0, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(161, 'Dr. Test Beta',  'FAC-TST-002', 'PROFESSOR', 'Ph.D Test', 110, 0.0, 40.0, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 8. Faculty competencies — faculty 160 teaches 150, faculty 161 teaches 151.
--    (No faculty availability windows => faculty are free all week => no blocking.)
INSERT INTO utms.faculty_competencies (id, faculty_id, course_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(170, 160, 150, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(171, 161, 151, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 9. Rooms — capacity 60 >= batch strength 30.
INSERT INTO utms.rooms (id, name, code, campus_id, capacity, room_type, equipment_tags, building, floor, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(180, 'Test Room 1', 'TST-R1', 100, 60, 'CLASSROOM', '', 'Test Block', '1', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(181, 'Test Room 2', 'TST-R2', 100, 60, 'CLASSROOM', '', 'Test Block', '1', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 10. Working-day pattern — FIVE_DAY => Mon-Fri.
INSERT INTO utms.working_day_patterns (id, campus_id, pattern_type, working_saturdays, custom_definition, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(190, 100, 'FIVE_DAY', NULL, NULL, TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 11. Time-slot grid for the campus.
INSERT INTO utms.time_slot_grids (id, campus_id, grid_name, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(200, 100, 'Test Campus Grid', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 12. Slot definitions — 6 all-day 60-min TEACHING slots (matches L/T rule duration 60).
INSERT INTO utms.slot_definitions (id, grid_id, start_time, end_time, slot_type, applicable_day, label, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(210, 200, '08:00', '09:00', 'TEACHING', NULL, 'P1', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(211, 200, '09:00', '10:00', 'TEACHING', NULL, 'P2', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(212, 200, '10:00', '11:00', 'TEACHING', NULL, 'P3', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(213, 200, '11:00', '12:00', 'TEACHING', NULL, 'P4', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(214, 200, '13:00', '14:00', 'TEACHING', NULL, 'P5', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(215, 200, '14:00', '15:00', 'TEACHING', NULL, 'P6', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 13. Session-derivation rules for campus 100 (short codes L/T/P; engine matches both forms).
INSERT INTO utms.session_derivation_rules (id, campus_id, component_type, slot_duration_minutes, hours_per_session, description, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(220, 100, 'L', 60,  1.0, 'Test — 1 lecture hour => one 60-min slot',  TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(221, 100, 'T', 60,  1.0, 'Test — 1 tutorial hour => one 60-min slot', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL),
(222, 100, 'P', 120, 2.0, 'Test — practicals => 2-hour blocks',        TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- 14. Academic calendar for (campus 100, 2025-26, ODD).
INSERT INTO utms.academic_calendars (id, campus_id, academic_year, semester_identifier, semester_start_date, semester_end_date, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(230, 100, '2025-26', 'ODD', '2025-08-01', '2025-12-15', TRUE, NOW(), NOW(), 'easy-seed', 'easy-seed', NULL);

-- Keep sequences ahead of these fixed high IDs so future inserts do not collide.
SELECT setval('utms.campuses_id_seq',                (SELECT GREATEST(MAX(id), 100) FROM utms.campuses));
SELECT setval('utms.departments_id_seq',             (SELECT GREATEST(MAX(id), 110) FROM utms.departments));
SELECT setval('utms.programs_id_seq',                (SELECT GREATEST(MAX(id), 120) FROM utms.programs));
SELECT setval('utms.batches_id_seq',                 (SELECT GREATEST(MAX(id), 130) FROM utms.batches));
SELECT setval('utms.sections_id_seq',                (SELECT GREATEST(MAX(id), 140) FROM utms.sections));
SELECT setval('utms.courses_id_seq',                 (SELECT GREATEST(MAX(id), 151) FROM utms.courses));
SELECT setval('utms.faculty_id_seq',                 (SELECT GREATEST(MAX(id), 161) FROM utms.faculty));
SELECT setval('utms.faculty_competencies_id_seq',    (SELECT GREATEST(MAX(id), 171) FROM utms.faculty_competencies));
SELECT setval('utms.rooms_id_seq',                   (SELECT GREATEST(MAX(id), 181) FROM utms.rooms));
SELECT setval('utms.working_day_patterns_id_seq',    (SELECT GREATEST(MAX(id), 190) FROM utms.working_day_patterns));
SELECT setval('utms.time_slot_grids_id_seq',         (SELECT GREATEST(MAX(id), 200) FROM utms.time_slot_grids));
SELECT setval('utms.slot_definitions_id_seq',        (SELECT GREATEST(MAX(id), 215) FROM utms.slot_definitions));
SELECT setval('utms.session_derivation_rules_id_seq',(SELECT GREATEST(MAX(id), 222) FROM utms.session_derivation_rules));
SELECT setval('utms.academic_calendars_id_seq',      (SELECT GREATEST(MAX(id), 230) FROM utms.academic_calendars));

-- ============================================================================
-- DONE. Generate the timetable with this payload from the frontend:
--   { "departmentId": 110, "semester": "ODD", "academicYear": "2025-26" }
-- Expected: 6 lecture sessions placed, status COMPLETE (no infeasibility).
-- ============================================================================
