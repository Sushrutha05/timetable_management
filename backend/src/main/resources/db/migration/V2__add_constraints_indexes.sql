-- ========================
-- UNIQUE CONSTRAINTS
-- ========================

ALTER TABLE departments
ADD CONSTRAINT uq_department_name UNIQUE (name);

ALTER TABLE courses
ADD CONSTRAINT uq_course_code UNIQUE (course_code);

ALTER TABLE courses
ADD CONSTRAINT uq_course_name UNIQUE (course_name);

ALTER TABLE rooms
ADD CONSTRAINT uq_room_number UNIQUE (room_number);

ALTER TABLE users
ADD CONSTRAINT uq_user_email UNIQUE (email);

ALTER TABLE faculty
ADD CONSTRAINT uq_faculty_user UNIQUE (user_id);

-- ========================
-- FOREIGN KEYS
-- ========================

ALTER TABLE faculty
ADD CONSTRAINT fk_faculty_user
FOREIGN KEY (user_id) REFERENCES users(user_id);

ALTER TABLE faculty
ADD CONSTRAINT fk_faculty_department
FOREIGN KEY (department_id) REFERENCES departments(department_id);

ALTER TABLE faculty
ADD CONSTRAINT fk_faculty_designation
FOREIGN KEY (designation)
REFERENCES designation_constraints(designation);

ALTER TABLE sections
ADD CONSTRAINT fk_sections_department
FOREIGN KEY (department_id)
REFERENCES departments(department_id);

ALTER TABLE department_courses
ADD CONSTRAINT fk_dep_courses_department
FOREIGN KEY (department_id)
REFERENCES departments(department_id);

ALTER TABLE department_courses
ADD CONSTRAINT fk_dep_courses_course
FOREIGN KEY (course_id)
REFERENCES courses(course_id);

ALTER TABLE course_offerings
ADD CONSTRAINT fk_offering_course
FOREIGN KEY (course_id)
REFERENCES courses(course_id);

ALTER TABLE course_offerings
ADD CONSTRAINT fk_offering_faculty
FOREIGN KEY (faculty_id)
REFERENCES faculty(faculty_id);

ALTER TABLE course_offerings
ADD CONSTRAINT fk_offering_section
FOREIGN KEY (section_id)
REFERENCES sections(section_id);

ALTER TABLE faculty_preferences
ADD CONSTRAINT fk_pref_faculty
FOREIGN KEY (faculty_id)
REFERENCES faculty(faculty_id);

ALTER TABLE faculty_preferences
ADD CONSTRAINT fk_pref_course
FOREIGN KEY (course_id)
REFERENCES courses(course_id);

ALTER TABLE scheduled_classes
ADD CONSTRAINT fk_sched_offering
FOREIGN KEY (offering_id)
REFERENCES course_offerings(offering_id);

ALTER TABLE scheduled_classes
ADD CONSTRAINT fk_sched_room
FOREIGN KEY (room_id)
REFERENCES rooms(room_id);

-- ========================
-- PERFORMANCE INDEXES
-- ========================

CREATE INDEX idx_faculty_department
ON faculty(department_id);

CREATE INDEX idx_sections_department
ON sections(department_id);

CREATE INDEX idx_offering_course
ON course_offerings(course_id);

CREATE INDEX idx_offering_faculty
ON course_offerings(faculty_id);

CREATE INDEX idx_sched_room
ON scheduled_classes(room_id);

CREATE INDEX idx_sched_offering
ON scheduled_classes(offering_id);

CREATE INDEX idx_faculty_pref_faculty
ON faculty_preferences(faculty_id);

CREATE INDEX idx_faculty_pref_course
ON faculty_preferences(course_id);
