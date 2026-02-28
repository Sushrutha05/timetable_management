-- ========================
-- CORE MASTER TABLES
-- ========================

CREATE TABLE designation_constraints (
    designation VARCHAR PRIMARY KEY,
    max_lecture_hours INT NOT NULL,
    max_lab_hours INT NOT NULL,
    max_total_hours INT DEFAULT 20,
    priority_level INT DEFAULT 0
);

CREATE TABLE departments (
    department_id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL
);

CREATE TABLE courses (
    course_id BIGSERIAL PRIMARY KEY,
    course_code VARCHAR NOT NULL,
    course_name VARCHAR NOT NULL,
    credit_hours INT NOT NULL,
    course_type VARCHAR,
    lecture_hours INT DEFAULT 0,
    tutorial_hours INT DEFAULT 0,
    practical_hours INT DEFAULT 0
);

CREATE TABLE rooms (
    room_id SERIAL PRIMARY KEY,
    room_number VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    capacity INT NOT NULL
);

CREATE TABLE time_slots (
    time_slot_id BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_break BOOLEAN NOT NULL DEFAULT FALSE
);

-- ========================
-- USER + FACULTY
-- ========================

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR NOT NULL,
    password_hash VARCHAR NOT NULL,
    role INT NOT NULL
);

CREATE TABLE faculty (
    faculty_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    department_id INT NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR,
    middle_initial VARCHAR,
    date_of_joining DATE NOT NULL,
    date_of_birth DATE NOT NULL,
    designation VARCHAR NOT NULL
);

-- ========================
-- ACADEMIC STRUCTURE
-- ========================

CREATE TABLE sections (
    section_id BIGSERIAL PRIMARY KEY,
    department_id INT NOT NULL,
    name VARCHAR NOT NULL,
    semester INT NOT NULL,
    year INT NOT NULL,
    student_count INT
);

CREATE TABLE department_courses (
    department_id INT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (department_id, course_id)
);

CREATE TABLE course_offerings (
    offering_id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL
);

CREATE TABLE faculty_preferences (
    preference_id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    priority INT NOT NULL
);

CREATE TABLE scheduled_classes (
    class_id BIGSERIAL PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    room_id INT NOT NULL,
    day_of_week VARCHAR NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);
