-- ============================================================================
-- UTMS Demo Seed Data
-- Realistic Indian university data for development and testing
-- Run AFTER all Flyway migrations (V1..V9)
-- ============================================================================

-- Clean existing seed data (in reverse FK order)
DELETE FROM utms.institution_common_slots WHERE created_by = 'system';
DELETE FROM utms.soft_constraint_weights WHERE created_by = 'system';
DELETE FROM utms.session_derivation_rules WHERE created_by = 'system';
DELETE FROM utms.slot_definitions WHERE created_by = 'system';
DELETE FROM utms.time_slot_grids WHERE created_by = 'system';
DELETE FROM utms.working_day_patterns WHERE created_by = 'system';
DELETE FROM utms.calendar_orientation_periods WHERE created_by = 'system';
DELETE FROM utms.calendar_exam_windows WHERE created_by = 'system';
DELETE FROM utms.calendar_holidays WHERE created_by = 'system';
DELETE FROM utms.academic_calendars WHERE created_by = 'system';
DELETE FROM utms.asset_availability_windows WHERE created_by = 'system';
DELETE FROM utms.schedulable_assets WHERE created_by = 'system';
DELETE FROM utms.rooms WHERE created_by = 'system';
DELETE FROM utms.faculty_preferences WHERE created_by = 'system';
DELETE FROM utms.faculty_availability_windows WHERE created_by = 'system';
DELETE FROM utms.faculty_campus_associations WHERE created_by = 'system';
DELETE FROM utms.faculty_competencies WHERE created_by = 'system';
DELETE FROM utms.faculty WHERE created_by = 'system';
DELETE FROM utms.courses WHERE created_by = 'system';
DELETE FROM utms.sections WHERE created_by = 'system';
DELETE FROM utms.batches WHERE created_by = 'system';
DELETE FROM utms.programs WHERE created_by = 'system';
DELETE FROM utms.departments WHERE created_by = 'system';
DELETE FROM utms.campuses WHERE created_by = 'system';

-- ============================================================================
-- 1. CAMPUSES (3 campuses)
-- ============================================================================
INSERT INTO utms.campuses (id, name, code, location, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 'Main Campus Bangalore', 'BLR', 'Banashankari, Bangalore 560070, Karnataka', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 'North Campus Hubli', 'HBL', 'Vidyanagar, Hubli 580031, Karnataka', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 'South Campus Mysore', 'MYS', 'Jayalakshmipuram, Mysore 570012, Karnataka', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.campuses_id_seq', 3);

-- ============================================================================
-- 2. DEPARTMENTS (10 departments across campuses)
-- ============================================================================
INSERT INTO utms.departments (id, name, code, campus_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'Computer Science and Engineering', 'CSE', 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'Electronics and Communication Engineering', 'ECE', 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'Mechanical Engineering', 'ME', 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'Civil Engineering', 'CE', 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'Mathematics', 'MATH', 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'Physics', 'PHY', 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'Chemistry', 'CHEM', 3, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'School of Management (MBA)', 'MBA', 3, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'Information Science and Engineering', 'ISE', 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'Electrical and Electronics Engineering', 'EEE', 3, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.departments_id_seq', 10);

-- ============================================================================
-- 3. PROGRAMS (12 programs across departments)
-- ============================================================================
INSERT INTO utms.programs (id, name, code, department_id, duration_semesters, degree_type, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'B.Tech Computer Science and Engineering', 'BTCS', 1, 8, 'B.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'B.Tech Electronics and Communication', 'BTEC', 2, 8, 'B.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'B.Tech Mechanical Engineering', 'BTME', 3, 8, 'B.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'B.Tech Civil Engineering', 'BTCE', 4, 8, 'B.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'M.Tech Computer Science', 'MTCS', 1, 4, 'M.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'M.Tech VLSI Design', 'MTVL', 2, 4, 'M.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'Master of Business Administration', 'MBAP', 8, 4, 'MBA', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'B.Sc Physics', 'BSPH', 6, 6, 'B.SC', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'B.Sc Chemistry', 'BSCH', 7, 6, 'B.SC', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'M.Sc Mathematics', 'MSMT', 5, 4, 'M.SC', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 'B.Tech Information Science', 'BTIS', 9, 8, 'B.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 'B.Tech Electrical Engineering', 'BTEE', 10, 8, 'B.TECH', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.programs_id_seq', 12);

-- ============================================================================
-- 4. BATCHES (15 batches across programs)
-- ============================================================================
INSERT INTO utms.batches (id, year_identifier, strength, elective_basket, program_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  '2024-25', 120, 'AI/ML,Cloud Computing,Cyber Security', 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  '2023-24', 120, 'AI/ML,Data Science', 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  '2024-25', 60, 'VLSI,Embedded Systems', 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  '2023-24', 60, 'VLSI,Signal Processing', 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  '2024-25', 60, 'Automotive,Manufacturing', 3, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  '2024-25', 60, 'Structural,Transportation', 4, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  '2024-25', 30, NULL, 5, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  '2024-25', 30, NULL, 6, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  '2024-25', 60, 'Finance,Marketing,HR', 7, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, '2024-25', 40, NULL, 8, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, '2024-25', 40, NULL, 9, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, '2024-25', 30, NULL, 10, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(13, '2024-25', 120, 'Full Stack,DevOps', 11, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(14, '2024-25', 60, 'Power Systems,Control Systems', 12, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(15, '2023-24', 60, 'Automotive,Thermal', 3, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.batches_id_seq', 15);

-- ============================================================================
-- 5. SECTIONS (20 sections across batches)
-- ============================================================================
INSERT INTO utms.sections (id, section_identifier, sub_strength, batch_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'A', 60, 1,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'B', 60, 1,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'A', 60, 2,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'B', 60, 2,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'A', 30, 3,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'B', 30, 3,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'A', 30, 4,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'B', 30, 4,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'A', 60, 5,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'A', 60, 6,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 'A', 30, 7,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 'A', 30, 8,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(13, 'A', 60, 9,  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(14, 'A', 40, 10, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(15, 'A', 40, 11, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(16, 'A', 30, 12, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(17, 'A', 60, 13, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(18, 'B', 60, 13, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(19, 'A', 60, 14, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(20, 'A', 60, 15, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.sections_id_seq', 20);

-- ============================================================================
-- 6. COURSES (15 courses across departments)
-- ============================================================================
INSERT INTO utms.courses (id, name, code, department_id, lecture_hours, tutorial_hours, practical_hours, credits, course_type, equipment_tags, is_cross_listed, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'Data Structures and Algorithms', 'CS301', 1, 3, 1, 2, 4.0, 'CORE', 'projector,computer_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'Digital Electronics', 'EC201', 2, 3, 0, 2, 4.0, 'CORE', 'projector,oscilloscope,logic_analyzer', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'Thermodynamics', 'ME301', 3, 3, 1, 0, 3.0, 'CORE', 'projector', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'Structural Analysis', 'CE401', 4, 3, 1, 2, 4.0, 'CORE', 'projector,structural_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'Linear Algebra', 'MA201', 5, 3, 1, 0, 4.0, 'CORE', 'projector,whiteboard', TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'Quantum Mechanics', 'PH301', 6, 3, 1, 2, 4.0, 'CORE', 'projector,physics_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'Database Management Systems', 'CS401', 1, 3, 0, 2, 4.0, 'CORE', 'projector,computer_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'Computer Networks', 'CS501', 1, 3, 0, 2, 4.0, 'CORE', 'projector,networking_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'Operating Systems', 'CS402', 1, 3, 0, 2, 4.0, 'CORE', 'projector,computer_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'Organic Chemistry', 'CH201', 7, 3, 0, 3, 4.0, 'CORE', 'projector,chemistry_lab,fume_hood', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 'Financial Management', 'MB301', 8, 3, 1, 0, 3.0, 'CORE', 'projector', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 'Machine Learning', 'CS601', 1, 3, 0, 2, 4.0, 'ELECTIVE', 'projector,gpu_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(13, 'Software Engineering', 'IS401', 9, 3, 1, 2, 4.0, 'CORE', 'projector,computer_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(14, 'Power Electronics', 'EE401', 10, 3, 0, 2, 4.0, 'CORE', 'projector,power_electronics_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(15, 'Signal Processing', 'EC301', 2, 3, 1, 2, 4.0, 'CORE', 'projector,dsp_lab', FALSE, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.courses_id_seq', 15);

-- ============================================================================
-- 7. FACULTY (12 faculty across departments)
-- ============================================================================
INSERT INTO utms.faculty (id, name, identifier, designation, qualification, home_department_id, min_weekly_load, max_weekly_load, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'Dr. Ramesh Kumar', 'FAC-BLR-001', 'PROFESSOR', 'Ph.D Computer Science, IISc Bangalore', 1, 12.0, 18.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'Prof. Lakshmi Devi', 'FAC-BLR-002', 'ASSOCIATE_PROFESSOR', 'M.Tech VLSI Design, IIT Madras', 2, 14.0, 20.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'Dr. Anil Sharma', 'FAC-BLR-003', 'PROFESSOR', 'Ph.D Mechanical Engineering, IIT Bombay', 3, 12.0, 16.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'Dr. Priya Nair', 'FAC-HBL-001', 'ASSOCIATE_PROFESSOR', 'Ph.D Structural Engineering, NIT Surathkal', 4, 14.0, 20.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'Prof. Suresh Patil', 'FAC-HBL-002', 'ASSISTANT_PROFESSOR', 'M.Sc Mathematics, University of Mysore', 5, 16.0, 22.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'Dr. Kavitha Rao', 'FAC-HBL-003', 'PROFESSOR', 'Ph.D Physics, IISc Bangalore', 6, 12.0, 16.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'Dr. Mohan Hegde', 'FAC-MYS-001', 'ASSOCIATE_PROFESSOR', 'Ph.D Organic Chemistry, JNCASR', 7, 14.0, 20.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'Prof. Deepa Kulkarni', 'FAC-MYS-002', 'PROFESSOR', 'Ph.D Finance, IIM Bangalore', 8, 12.0, 16.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'Dr. Venkatesh Murthy', 'FAC-BLR-004', 'ASSISTANT_PROFESSOR', 'Ph.D Data Science, IIT Delhi', 1, 16.0, 22.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'Prof. Anjali Deshmukh', 'FAC-BLR-005', 'ASSOCIATE_PROFESSOR', 'M.Tech Software Engineering, IIIT Hyderabad', 9, 14.0, 20.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 'Dr. Rajesh Gowda', 'FAC-MYS-003', 'ASSISTANT_PROFESSOR', 'Ph.D Power Systems, IIT Kharagpur', 10, 16.0, 22.0, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 'Prof. Shalini Iyer', 'FAC-BLR-006', 'ASSOCIATE_PROFESSOR', 'Ph.D Computer Networks, IIT Kanpur', 1, 14.0, 20.0, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.faculty_id_seq', 12);

-- ============================================================================
-- 8. FACULTY_COMPETENCIES (20 links — faculty to courses)
-- Each (faculty_id, course_id) pair must be unique per UNIQUE constraint
-- ============================================================================
INSERT INTO utms.faculty_competencies (id, faculty_id, course_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  1,  1,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Ramesh → Data Structures
(2,  1,  7,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Ramesh → DBMS
(3,  1,  12, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Ramesh → Machine Learning
(4,  2,  2,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Lakshmi → Digital Electronics
(5,  2,  15, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Lakshmi → Signal Processing
(6,  3,  3,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Anil → Thermodynamics
(7,  4,  4,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Priya → Structural Analysis
(8,  5,  5,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Suresh → Linear Algebra
(9,  6,  6,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Kavitha → Quantum Mechanics
(10, 7,  10, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Mohan → Organic Chemistry
(11, 8,  11, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Deepa → Financial Management
(12, 9,  12, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Venkatesh → Machine Learning
(13, 9,  7,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Venkatesh → DBMS
(14, 10, 13, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Anjali → Software Engineering
(15, 10, 9,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Anjali → Operating Systems
(16, 11, 14, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Rajesh → Power Electronics
(17, 12, 8,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Shalini → Computer Networks
(18, 12, 1,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Shalini → Data Structures
(19, 9,  9,  TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Venkatesh → Operating Systems
(20, 6,  5,  TRUE, NOW(), NOW(), 'system', 'system', NULL);  -- Dr. Kavitha → Linear Algebra (cross-dept)

SELECT setval('utms.faculty_competencies_id_seq', 20);

-- ============================================================================
-- 9. FACULTY_CAMPUS_ASSOCIATIONS (15 links)
-- Each (faculty_id, campus_id) pair must be unique per UNIQUE constraint
-- ============================================================================
INSERT INTO utms.faculty_campus_associations (id, faculty_id, campus_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  1,  1, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Ramesh → Bangalore
(2,  2,  1, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Lakshmi → Bangalore
(3,  3,  1, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Anil → Bangalore
(4,  4,  2, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Priya → Hubli
(5,  5,  2, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Suresh → Hubli
(6,  6,  2, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Kavitha → Hubli
(7,  7,  3, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Mohan → Mysore
(8,  8,  3, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Deepa → Mysore
(9,  9,  1, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Venkatesh → Bangalore
(10, 10, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Anjali → Bangalore
(11, 11, 3, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Rajesh → Mysore
(12, 12, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Prof. Shalini → Bangalore
(13, 1,  2, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Ramesh → Hubli (cross-campus)
(14, 6,  3, TRUE, NOW(), NOW(), 'system', 'system', NULL),  -- Dr. Kavitha → Mysore (cross-campus)
(15, 9,  3, TRUE, NOW(), NOW(), 'system', 'system', NULL);  -- Dr. Venkatesh → Mysore (cross-campus)

SELECT setval('utms.faculty_campus_associations_id_seq', 15);

-- ============================================================================
-- 10. FACULTY_AVAILABILITY_WINDOWS (15 windows — unavailability periods)
-- ============================================================================
INSERT INTO utms.faculty_availability_windows (id, faculty_id, day_of_week, start_time, end_time, reason_code, reason_note, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  1,  'MONDAY',    '08:00', '09:00', 'RESEARCH', 'Lab meeting with Ph.D scholars', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  1,  'WEDNESDAY', '14:00', '16:00', 'ADMINISTRATIVE', 'Department head duties', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  2,  'TUESDAY',   '16:00', '17:00', 'PERSONAL', 'Regular medical appointment', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  3,  'FRIDAY',    '08:00', '10:00', 'RESEARCH', 'Industry collaboration call', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  4,  'THURSDAY',  '14:00', '17:00', 'RESEARCH', 'Site visit supervision', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  5,  'MONDAY',    '14:00', '15:00', 'ADMINISTRATIVE', 'Faculty senate meeting', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  6,  'WEDNESDAY', '08:00', '10:00', 'RESEARCH', 'Experimental physics lab work', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  7,  'TUESDAY',   '08:00', '09:00', 'RESEARCH', 'Research group seminar', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  8,  'FRIDAY',    '14:00', '17:00', 'PERSONAL', 'Industry consulting hours', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 9,  'THURSDAY',  '08:00', '09:00', 'RESEARCH', 'AI Lab meeting', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 10, 'MONDAY',    '16:00', '17:00', 'ADMINISTRATIVE', 'Placement cell coordination', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 11, 'WEDNESDAY', '14:00', '16:00', 'RESEARCH', 'Power systems lab maintenance', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(13, 12, 'TUESDAY',   '14:00', '15:00', 'ADMINISTRATIVE', 'Exam committee meeting', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(14, 1,  'FRIDAY',    '14:00', '17:00', 'RESEARCH', 'Research paper writing block', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(15, 3,  'WEDNESDAY', '08:00', '09:00', 'PERSONAL', 'Personal commitment', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.faculty_availability_windows_id_seq', 15);

-- ============================================================================
-- 11. FACULTY_PREFERENCES (10 preferences)
-- Note: faculty_preferences has no deleted_at column per schema V4
-- ============================================================================
INSERT INTO utms.faculty_preferences (id, faculty_id, preferred_time_of_day, session_distribution, is_active, created_at, updated_at, created_by, updated_by) VALUES
(1,  1,  'MORNING',       'CONSECUTIVE',   TRUE, NOW(), NOW(), 'system', 'system'),
(2,  2,  'MORNING',       'SPREAD',        TRUE, NOW(), NOW(), 'system', 'system'),
(3,  3,  'AFTERNOON',     'CONSECUTIVE',   TRUE, NOW(), NOW(), 'system', 'system'),
(4,  4,  'MORNING',       'NO_PREFERENCE', TRUE, NOW(), NOW(), 'system', 'system'),
(5,  5,  'NO_PREFERENCE', 'SPREAD',        TRUE, NOW(), NOW(), 'system', 'system'),
(6,  6,  'MORNING',       'CONSECUTIVE',   TRUE, NOW(), NOW(), 'system', 'system'),
(7,  7,  'AFTERNOON',     'SPREAD',        TRUE, NOW(), NOW(), 'system', 'system'),
(8,  8,  'MORNING',       'NO_PREFERENCE', TRUE, NOW(), NOW(), 'system', 'system'),
(9,  9,  'MORNING',       'CONSECUTIVE',   TRUE, NOW(), NOW(), 'system', 'system'),
(10, 10, 'NO_PREFERENCE', 'SPREAD',        TRUE, NOW(), NOW(), 'system', 'system');

SELECT setval('utms.faculty_preferences_id_seq', 10);

-- ============================================================================
-- 12. ROOMS (12 rooms across campuses)
-- ============================================================================
INSERT INTO utms.rooms (id, name, code, campus_id, capacity, room_type, equipment_tags, building, floor, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'Classroom 101', 'CL-101', 1, 60, 'CLASSROOM', 'projector,whiteboard,air_conditioner', 'Academic Block A', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'Classroom 102', 'CL-102', 1, 60, 'CLASSROOM', 'projector,whiteboard,air_conditioner', 'Academic Block A', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'Classroom 201', 'CL-201', 1, 90, 'CLASSROOM', 'projector,whiteboard,air_conditioner,microphone', 'Academic Block A', '2', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'Computer Lab A1', 'LAB-A1', 1, 40, 'LAB', 'projector,computer_lab,air_conditioner,networking_lab', 'IT Block', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'Electronics Lab B2', 'LAB-B2', 1, 30, 'LAB', 'projector,oscilloscope,logic_analyzer,function_generator', 'ECE Block', '2', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'Seminar Hall 01', 'SH-01', 1, 150, 'SEMINAR_HALL', 'projector,microphone,air_conditioner,video_conferencing', 'Academic Block B', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'Auditorium', 'AUD-1', 1, 500, 'AUDITORIUM', 'projector,microphone,stage_lighting,video_conferencing', 'Central Block', 'G', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'Classroom 301', 'CL-301', 2, 60, 'CLASSROOM', 'projector,whiteboard', 'Main Block', '3', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'Physics Lab', 'LAB-PH1', 2, 30, 'LAB', 'projector,physics_lab,spectrometer,optical_bench', 'Science Block', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'Classroom 101', 'CL-101', 3, 60, 'CLASSROOM', 'projector,whiteboard,air_conditioner', 'Academic Block', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 'Chemistry Lab', 'LAB-CH1', 3, 25, 'LAB', 'projector,chemistry_lab,fume_hood,safety_shower', 'Science Block', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 'MBA Classroom', 'CL-MBA1', 3, 60, 'CLASSROOM', 'projector,whiteboard,air_conditioner,video_conferencing', 'Management Block', '1', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.rooms_id_seq', 12);

-- ============================================================================
-- 13. SCHEDULABLE_ASSETS (10 assets)
-- ============================================================================
INSERT INTO utms.schedulable_assets (id, name, identifier, asset_type, owning_department_id, campus_id, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  'Portable Projector Unit 1', 'PROJ-001', 'PROJECTOR', 1, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  'Portable Projector Unit 2', 'PROJ-002', 'PROJECTOR', 2, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  'GPU Compute Server', 'GPU-001', 'COMPUTING', 1, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  'CNC Machine', 'CNC-001', 'MACHINERY', 3, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  'Total Station Surveying Kit', 'SURV-001', 'EQUIPMENT', 4, 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  'Video Conferencing Kit', 'VC-001', 'AV_EQUIPMENT', 8, 3, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  'DSP Development Board Set', 'DSP-001', 'EQUIPMENT', 2, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  'Portable Whiteboard Set', 'WB-001', 'FURNITURE', 5, 2, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  'Robotics Kit', 'ROB-001', 'EQUIPMENT', 3, 1, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 'Power Electronics Trainer', 'PET-001', 'EQUIPMENT', 10, 3, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.schedulable_assets_id_seq', 10);

-- ============================================================================
-- 14. ASSET_AVAILABILITY_WINDOWS (10 windows)
-- ============================================================================
INSERT INTO utms.asset_availability_windows (id, asset_id, day_of_week, start_time, end_time, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  1, 'MONDAY',    '08:00', '17:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  1, 'WEDNESDAY', '08:00', '17:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  2, 'TUESDAY',   '08:00', '17:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  2, 'THURSDAY',  '08:00', '17:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  3, 'MONDAY',    '09:00', '18:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  3, 'FRIDAY',    '09:00', '18:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  4, 'TUESDAY',   '08:00', '13:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  5, 'WEDNESDAY', '08:00', '12:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  6, 'MONDAY',    '09:00', '17:00', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 7, 'THURSDAY',  '08:00', '17:00', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.asset_availability_windows_id_seq', 10);

-- ============================================================================
-- 15. ACADEMIC_CALENDARS (3 calendars — one per campus for odd semester 2024-25)
-- ============================================================================
INSERT INTO utms.academic_calendars (id, campus_id, academic_year, semester_identifier, semester_start_date, semester_end_date, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, '2024-25', 'ODD', '2024-08-01', '2024-12-15', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 2, '2024-25', 'ODD', '2024-08-05', '2024-12-18', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 3, '2024-25', 'ODD', '2024-08-01', '2024-12-20', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.academic_calendars_id_seq', 3);

-- ============================================================================
-- 16. CALENDAR_HOLIDAYS (10 holidays)
-- ============================================================================
INSERT INTO utms.calendar_holidays (id, calendar_id, start_date, end_date, description, scope, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1,  1, '2024-08-15', '2024-08-15', 'Independence Day', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  1, '2024-10-02', '2024-10-02', 'Gandhi Jayanti', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  1, '2024-10-12', '2024-10-14', 'Dasara Festival', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  1, '2024-11-01', '2024-11-01', 'Kannada Rajyotsava', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  1, '2024-11-14', '2024-11-14', 'Deepavali', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  2, '2024-08-15', '2024-08-15', 'Independence Day', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  2, '2024-10-02', '2024-10-02', 'Gandhi Jayanti', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  2, '2024-10-12', '2024-10-14', 'Dasara Festival', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9,  3, '2024-08-15', '2024-08-15', 'Independence Day', 'INSTITUTION_WIDE', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 3, '2024-10-03', '2024-10-03', 'Mysore Dasara Special Holiday', 'CAMPUS_SPECIFIC', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.calendar_holidays_id_seq', 10);

-- ============================================================================
-- 17. CALENDAR_EXAM_WINDOWS (6 exam windows — 2 per campus)
-- ============================================================================
INSERT INTO utms.calendar_exam_windows (id, calendar_id, start_date, end_date, exam_type, description, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, '2024-09-23', '2024-09-28', 'MID_SEMESTER', 'First Internal Assessment', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 1, '2024-12-02', '2024-12-14', 'END_SEMESTER', 'End Semester Examination', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 2, '2024-09-25', '2024-09-30', 'MID_SEMESTER', 'First Internal Assessment', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4, 2, '2024-12-05', '2024-12-17', 'END_SEMESTER', 'End Semester Examination', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5, 3, '2024-09-23', '2024-09-28', 'MID_SEMESTER', 'First Internal Assessment', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6, 3, '2024-12-09', '2024-12-20', 'END_SEMESTER', 'End Semester Examination', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.calendar_exam_windows_id_seq', 6);

-- ============================================================================
-- 18. CALENDAR_ORIENTATION_PERIODS (3 periods — one per campus)
-- ============================================================================
INSERT INTO utms.calendar_orientation_periods (id, calendar_id, start_date, end_date, description, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, '2024-08-01', '2024-08-03', 'Fresher Orientation and Induction Program', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 2, '2024-08-05', '2024-08-07', 'Fresher Orientation and Induction Program', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 3, '2024-08-01', '2024-08-04', 'Fresher Orientation and Campus Tour', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.calendar_orientation_periods_id_seq', 3);

-- ============================================================================
-- 19. WORKING_DAY_PATTERNS (3 patterns — one per campus)
-- ============================================================================
INSERT INTO utms.working_day_patterns (id, campus_id, pattern_type, working_saturdays, custom_definition, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, 'FIVE_DAY', NULL, NULL, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 2, 'SIX_DAY', NULL, NULL, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 3, 'ALTERNATE_SATURDAY', '1st,3rd', NULL, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.working_day_patterns_id_seq', 3);

-- ============================================================================
-- 20. TIME_SLOT_GRIDS (3 grids — one per campus)
-- ============================================================================
INSERT INTO utms.time_slot_grids (id, campus_id, grid_name, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, 'Bangalore Campus Standard Grid', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 2, 'Hubli Campus Standard Grid', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 3, 'Mysore Campus Standard Grid', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.time_slot_grids_id_seq', 3);

-- ============================================================================
-- 21. SLOT_DEFINITIONS (20 slots across grids)
-- Bangalore grid: 8 slots (6 teaching + 1 break + 1 lunch)
-- Hubli grid: 7 slots (5 teaching + 1 break + 1 lunch)
-- Mysore grid: 5 slots (3 teaching + 1 break + 1 lunch)
-- ============================================================================
INSERT INTO utms.slot_definitions (id, grid_id, start_time, end_time, slot_type, applicable_day, label, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
-- === Bangalore Campus Grid (id=1) — 8 slots ===
(1,  1, '08:00', '09:00', 'TEACHING', NULL, 'Period 1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2,  1, '09:00', '10:00', 'TEACHING', NULL, 'Period 2', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3,  1, '10:00', '10:15', 'BREAK',    NULL, 'Morning Break', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4,  1, '10:15', '11:15', 'TEACHING', NULL, 'Period 3', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5,  1, '11:15', '12:15', 'TEACHING', NULL, 'Period 4', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6,  1, '12:15', '13:00', 'LUNCH',    NULL, 'Lunch Break', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7,  1, '13:00', '14:00', 'TEACHING', NULL, 'Period 5', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8,  1, '14:00', '15:00', 'TEACHING', NULL, 'Period 6', TRUE, NOW(), NOW(), 'system', 'system', NULL),
-- === Hubli Campus Grid (id=2) — 7 slots ===
(9,  2, '08:30', '09:30', 'TEACHING', NULL, 'Period 1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(10, 2, '09:30', '10:30', 'TEACHING', NULL, 'Period 2', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(11, 2, '10:30', '10:45', 'BREAK',    NULL, 'Morning Break', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(12, 2, '10:45', '11:45', 'TEACHING', NULL, 'Period 3', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(13, 2, '11:45', '12:45', 'TEACHING', NULL, 'Period 4', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(14, 2, '12:45', '13:30', 'LUNCH',    NULL, 'Lunch Break', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(15, 2, '13:30', '14:30', 'TEACHING', NULL, 'Period 5', TRUE, NOW(), NOW(), 'system', 'system', NULL),
-- === Mysore Campus Grid (id=3) — 5 slots ===
(16, 3, '09:00', '10:00', 'TEACHING', NULL, 'Period 1', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(17, 3, '10:00', '11:00', 'TEACHING', NULL, 'Period 2', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(18, 3, '11:00', '11:15', 'BREAK',    NULL, 'Morning Break', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(19, 3, '11:15', '12:15', 'TEACHING', NULL, 'Period 3', TRUE, NOW(), NOW(), 'system', 'system', NULL),
(20, 3, '12:15', '13:00', 'LUNCH',    NULL, 'Lunch Break', TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.slot_definitions_id_seq', 20);

-- ============================================================================
-- SEED COMPLETE
-- Summary: 3 campuses, 10 departments, 12 programs, 15 batches, 20 sections,
-- 15 courses, 12 faculty, 20 competencies, 15 campus associations,
-- 15 availability windows, 10 preferences, 12 rooms, 10 assets,
-- 10 asset availability windows, 3 calendars, 10 holidays,
-- 6 exam windows, 3 orientation periods, 3 working day patterns,
-- 3 time slot grids, 20 slot definitions
-- ============================================================================

-- ============================================================================
-- 22. SESSION_DERIVATION_RULES (9 rules — L/T/P mapping per campus)
-- Maps each course component (Lecture/Tutorial/Practical) to a session
-- duration on a given campus. Unique per (campus_id, component_type).
-- ============================================================================
INSERT INTO utms.session_derivation_rules (id, campus_id, component_type, slot_duration_minutes, hours_per_session, description, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, 'L', 60,  1.0, 'Bangalore — 1 lecture hour maps to one 60-min teaching slot',   TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 1, 'T', 60,  1.0, 'Bangalore — 1 tutorial hour maps to one 60-min teaching slot',  TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 1, 'P', 120, 2.0, 'Bangalore — practicals scheduled as 2-hour lab blocks',         TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4, 2, 'L', 60,  1.0, 'Hubli — 1 lecture hour maps to one 60-min teaching slot',       TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5, 2, 'T', 60,  1.0, 'Hubli — 1 tutorial hour maps to one 60-min teaching slot',      TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6, 2, 'P', 120, 2.0, 'Hubli — practicals scheduled as 2-hour lab blocks',            TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7, 3, 'L', 60,  1.0, 'Mysore — 1 lecture hour maps to one 60-min teaching slot',      TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8, 3, 'T', 60,  1.0, 'Mysore — 1 tutorial hour maps to one 60-min teaching slot',     TRUE, NOW(), NOW(), 'system', 'system', NULL),
(9, 3, 'P', 180, 3.0, 'Mysore — practicals scheduled as 3-hour lab blocks',           TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.session_derivation_rules_id_seq', 9);

-- ============================================================================
-- 23. SOFT_CONSTRAINT_WEIGHTS (8 weights — quality-score tuning per campus)
-- Higher weight = greater influence on the generated timetable quality score.
-- Unique per (campus_id, constraint_type).
-- ============================================================================
INSERT INTO utms.soft_constraint_weights (id, campus_id, constraint_type, weight, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, 'FACULTY_TIME_PREFERENCE', 3.00, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 1, 'GAP_MINIMIZATION',        2.50, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 1, 'ROOM_PROXIMITY',          1.50, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4, 1, 'DAY_PATTERN_BALANCE',     2.00, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5, 1, 'FACULTY_DISTRIBUTION_PREFERENCE', 1.75, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6, 2, 'FACULTY_TIME_PREFERENCE', 2.00, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7, 2, 'GAP_MINIMIZATION',        3.00, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(8, 3, 'FACULTY_TIME_PREFERENCE', 2.50, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.soft_constraint_weights_id_seq', 8);

-- ============================================================================
-- 24. INSTITUTION_COMMON_SLOTS (7 slots — CCC/UWE pre-placed blockers)
-- Institution-wide reserved periods (e.g., common core courses, universal
-- wellness electives) that block regular scheduling in that day+slot.
-- slot_definition_id references TEACHING slots seeded in section 21.
-- ============================================================================
INSERT INTO utms.institution_common_slots (id, campus_id, name, day_of_week, slot_definition_id, applies_to_all_batches, is_active, created_at, updated_at, created_by, updated_by, deleted_at) VALUES
(1, 1, 'Common Core Course (CCC) — Constitution of India', 'MONDAY',    7,  TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(2, 1, 'Universal Wellness Elective (UWE) — Yoga',          'WEDNESDAY', 8,  TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(3, 1, 'Institution Assembly',                              'FRIDAY',    1,  TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(4, 2, 'Common Core Course (CCC) — Environmental Studies',  'TUESDAY',   15, TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(5, 2, 'Universal Wellness Elective (UWE) — Sports',        'THURSDAY',  13, TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(6, 3, 'Common Core Course (CCC) — Ethics',                 'MONDAY',    16, TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL),
(7, 3, 'Universal Wellness Elective (UWE) — Meditation',    'WEDNESDAY', 19, TRUE, TRUE, NOW(), NOW(), 'system', 'system', NULL);

SELECT setval('utms.institution_common_slots_id_seq', 7);
