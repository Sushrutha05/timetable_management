-- Add course_type to courses to distinguish theory vs lab classes
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS course_type VARCHAR(50) DEFAULT 'THEORY';

-- Add student_count to sections so scheduling can respect capacity needs
ALTER TABLE sections
    ADD COLUMN IF NOT EXISTS student_count INT DEFAULT 45;

-- Table to store admin-configurable time slots (including breaks)
CREATE TABLE IF NOT EXISTS time_slots (
    time_slot_id SERIAL PRIMARY KEY,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_break BOOLEAN NOT NULL DEFAULT FALSE
);

