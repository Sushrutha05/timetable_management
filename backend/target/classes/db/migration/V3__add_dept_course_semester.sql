-- Add semester column to department_courses to categorize courses by semester
ALTER TABLE department_courses
ADD COLUMN semester INTEGER NOT NULL DEFAULT 1;
