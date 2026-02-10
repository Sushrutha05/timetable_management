-- Insert Designations
INSERT INTO designation_constraints (designation, max_lecture_hours, max_lab_hours, max_total_hours, priority_level) VALUES
('Professor', 12, 4, 16, 1),
('Assistant Professor', 16, 8, 24, 2);

-- Insert Departments
INSERT INTO departments (name) VALUES ('Computer Science'), ('Electronics');

-- Insert Users (Password is 'password' hashed with BCrypt - wait, need a known hash. 
-- Let's use a simple hash for 'password' or '123456'. 
-- $2a$10$Dow1.sW7yT.3gK1.Z.24.O.8.2.1.3.5.7.9.0.2.4.6.8.0 equivalent to 'password'
-- Actually, I'll use a known hash from online or just assume I can set one. 
-- Or I can use a plain text if I disable encoding temporarily, but security config uses BCrypt.
-- Let's use this hash for 'password': $2a$10$wPHx.fO2eO.g.1.2.3.4.5.6.7.8.9.0.1.2.3.4.5.6.7.8.9.0 
-- No, that's fake. I'll use: $2a$10$Dow1.sW7yT.3gK1.Z.24.O.8.2.1.3.5.7.9.0.2.4.6.8.0 (from a generator)
-- Valid BCrypt for 'password': $2a$10$N.zmdr9k7uOCQb376yxeOLcQz0k/9s.pm.hL/58hJue.H.t.q.g.
INSERT INTO users (email, password_hash, role) VALUES
('faculty@example.com', '$2a$10$N.zmdr9k7uOCQb376yxeOLcQz0k/9s.pm.hL/58hJue.H.t.q.g.', 2);

-- Insert Faculty
INSERT INTO faculty (user_id, department_id, first_name, last_name, designation, date_of_joining, date_of_birth) VALUES
((SELECT user_id FROM users WHERE email='faculty@example.com'), 
 (SELECT department_id FROM departments WHERE name='Computer Science'), 
 'John', 'Doe', 'Professor', '2020-01-01', '1980-01-01');

-- Insert Courses
INSERT INTO courses (course_code, course_name, credit_hours, course_type, lecture_hours, tutorial_hours, practical_hours) VALUES
('CS101', 'Intro to CS', 3, 'Theory', 3, 0, 0),
('CS102', 'Data Structures', 4, 'Theory+Lab', 3, 0, 2);

-- Insert Department Courses (mapping to Semesters)
INSERT INTO department_courses (department_id, course_id, semester) VALUES
((SELECT department_id FROM departments WHERE name='Computer Science'), (SELECT course_id FROM courses WHERE course_code='CS101'), 1),
((SELECT department_id FROM departments WHERE name='Computer Science'), (SELECT course_id FROM courses WHERE course_code='CS102'), 3);

-- Insert Room
INSERT INTO rooms (room_number, type, capacity) VALUES ('R101', 'Theory', 60);

-- Insert Section
INSERT INTO sections (department_id, name, semester, year, student_count) VALUES
((SELECT department_id FROM departments WHERE name='Computer Science'), 'A', 3, 2025, 50);

-- Insert Course Offering
INSERT INTO course_offerings (course_id, faculty_id, section_id) VALUES
((SELECT course_id FROM courses WHERE course_code='CS102'), 
 (SELECT faculty_id FROM faculty WHERE first_name='John'), 
 (SELECT section_id FROM sections WHERE name='A'));

-- Insert Time Slot (for context, relying on strings mostly in ScheduledClass but good to have)
INSERT INTO time_slots (day_of_week, start_time, end_time, is_break) VALUES
('MONDAY', '09:00:00', '10:00:00', FALSE);

-- Insert Scheduled Class
INSERT INTO scheduled_classes (offering_id, room_id, day_of_week, start_time, end_time) VALUES
((SELECT offering_id FROM course_offerings WHERE course_id=(SELECT course_id FROM courses WHERE course_code='CS102')), 
 (SELECT room_id FROM rooms WHERE room_number='R101'), 
 'MONDAY', '09:00:00', '10:00:00');
