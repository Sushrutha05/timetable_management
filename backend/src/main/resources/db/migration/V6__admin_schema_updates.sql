
-- 1. Add department_id to users for Admin scoping (nullable initially, or we can update existing admins)
ALTER TABLE users ADD COLUMN department_id INT;

-- 2. Clean up Faculty table
ALTER TABLE faculty DROP COLUMN date_of_birth;
ALTER TABLE faculty DROP COLUMN date_of_joining;

-- 3. Update Designation Constraints
-- 3. Update Designation Constraints
-- Use ON CONFLICT to update existing records or insert new ones, avoiding FK violations
INSERT INTO designation_constraints (designation, max_lecture_hours, max_lab_hours) VALUES 
('HOD', 8, 4),
('Dean', 6, 2),
('Professor', 10, 5),
('Associate Professor', 12, 6),
('Assistant Professor', 14, 8),
('Senior Assistant Professor', 12, 6),
('Professor of Practice', 10, 5)
ON CONFLICT (designation) DO UPDATE 
SET max_lecture_hours = EXCLUDED.max_lecture_hours, 
    max_lab_hours = EXCLUDED.max_lab_hours;
