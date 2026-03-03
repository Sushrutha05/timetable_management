ALTER TABLE scheduled_classes
ADD COLUMN assigned_faculty_id BIGINT,
ADD CONSTRAINT fk_sc_assigned_faculty FOREIGN KEY (assigned_faculty_id) REFERENCES faculty(faculty_id);
