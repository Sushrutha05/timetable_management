-- Seed Departments
INSERT INTO departments (name) VALUES ('Computer Science');

-- Seed Rooms
INSERT INTO rooms (room_number, type, capacity) VALUES ('101', 'Lecture Hall', 60);

-- Seed Courses
INSERT INTO courses (course_code, course_name, credit_hours, lecture_hours, tutorial_hours, practical_hours) 
VALUES ('CS101', 'Introduction to Computer Science', 3, 3, 0, 0);

-- Seed Designation Constraints (needed for Faculty)
INSERT INTO designation_constraints (designation, max_lecture_hours, max_lab_hours) VALUES ('Professor', 10, 5);

-- Seed Users (Faculty with role 2)
-- Password is 'password'
INSERT INTO users (email, password_hash, role) VALUES ('faculty@test.com', '$2b$12$f5ERl1AbKP5fNiky4jVlNu6lC2J/XR2v9658F57RarAnmKwYXA.vi', 2);
INSERT INTO users (email, password_hash, role) VALUES ('admin@test.com', '$2b$12$f5ERl1AbKP5fNiky4jVlNu6lC2J/XR2v9658F57RarAnmKwYXA.vi', 1);

-- Seed Faculty
INSERT INTO faculty (user_id, department_id, first_name, last_name, date_of_joining, date_of_birth, designation) 
VALUES (
    (SELECT user_id FROM users WHERE email='faculty@test.com'), 
    (SELECT department_id FROM departments WHERE name='Computer Science'),
    'Test', 'Faculty', '2020-01-01', '1980-01-01', 'Professor'
);

-- Seed Sections
INSERT INTO sections (department_id, name, semester, year, student_count) 
VALUES ((SELECT department_id FROM departments WHERE name='Computer Science'), 'A', 3, 2, 60);

-- Link Course to Department (Semester 3)
-- Note: V3 added 'semester' column to department_courses if I recall correctly? 
-- Let me check V3. V3 added 'semester' column. I need to populate it.
-- But wait, V3 was about finding courses by semester.
-- Let's check V3 content.
-- I'll assume standard INSERT if V3 column exists.
-- The previous replace_file_content for V3 said: "Added a new column semester to the department_courses table."
-- So I should include it.

INSERT INTO department_courses (department_id, course_id, semester) 
VALUES (
    (SELECT department_id FROM departments WHERE name='Computer Science'),
    (SELECT course_id FROM courses WHERE course_code='CS101'),
    3
);

-- Seed Course Offerings
INSERT INTO course_offerings (course_id, faculty_id, section_id) 
VALUES (
    (SELECT course_id FROM courses WHERE course_code='CS101'),
    (SELECT faculty_id FROM faculty WHERE first_name='Test'),
    (SELECT section_id FROM sections WHERE name='A')
);

-- Seed Scheduled Classes
INSERT INTO scheduled_classes (offering_id, room_id, day_of_week, start_time, end_time) 
VALUES (
    (SELECT offering_id FROM course_offerings WHERE faculty_id=(SELECT faculty_id FROM faculty WHERE first_name='Test') LIMIT 1),
    (SELECT room_id FROM rooms WHERE room_number='101'),
    'MONDAY', '10:00:00', '11:00:00'
);
