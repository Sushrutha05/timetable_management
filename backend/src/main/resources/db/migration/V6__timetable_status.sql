-- Create the timetable status table to track Publish state
CREATE TABLE timetable_metadata (
    id SERIAL PRIMARY KEY,
    key VARCHAR(255) UNIQUE NOT NULL,
    value VARCHAR(255) NOT NULL
);

-- Insert the default state as DRAFT
INSERT INTO timetable_metadata (key, value) VALUES ('STATUS', 'DRAFT');
