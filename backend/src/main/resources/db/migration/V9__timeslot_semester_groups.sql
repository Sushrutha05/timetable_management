-- V9: Add semester_group to time_slots and pre-seed canonical schedules

-- Step 1: Add the semester_group column
ALTER TABLE time_slots ADD COLUMN semester_group VARCHAR(20) NOT NULL DEFAULT 'SEM_3_4';

-- Step 2: Clear any existing manually-entered / old seed rows
DELETE FROM time_slots;

-- Step 3: Pre-seed using a cross-join of days × slot definitions
-- This generates 9 slots × 6 days × 2 active groups = 108 rows

INSERT INTO time_slots (day_of_week, start_time, end_time, is_break, semester_group)
WITH days(day) AS (
    VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY')
),
sem567(start_time, end_time, is_break) AS (
    VALUES
        ('09:00:00'::time, '09:55:00'::time, false),
        ('09:55:00'::time, '10:15:00'::time, true),   -- short break
        ('10:15:00'::time, '11:10:00'::time, false),
        ('11:10:00'::time, '12:05:00'::time, false),
        ('12:05:00'::time, '13:00:00'::time, true),   -- lunch break
        ('13:00:00'::time, '13:55:00'::time, false),
        ('13:55:00'::time, '14:50:00'::time, false),
        ('14:50:00'::time, '15:45:00'::time, false),
        ('15:45:00'::time, '16:30:00'::time, false)
),
sem34(start_time, end_time, is_break) AS (
    VALUES
        ('09:00:00'::time, '09:55:00'::time, false),
        ('09:55:00'::time, '10:50:00'::time, false),
        ('10:50:00'::time, '11:10:00'::time, true),   -- short break
        ('11:10:00'::time, '12:05:00'::time, false),
        ('12:05:00'::time, '13:00:00'::time, false),
        ('13:00:00'::time, '13:45:00'::time, true),   -- lunch break
        ('13:45:00'::time, '14:40:00'::time, false),
        ('14:40:00'::time, '15:35:00'::time, false),
        ('15:35:00'::time, '16:30:00'::time, false)
)
SELECT d.day, s.start_time, s.end_time, s.is_break, 'SEM_5_6_7'
FROM days d CROSS JOIN sem567 s
UNION ALL
SELECT d.day, s.start_time, s.end_time, s.is_break, 'SEM_3_4'
FROM days d CROSS JOIN sem34 s;

-- Note: SEM_1_2 slots are intentionally not seeded yet (system not in use for Sem 1/2).
-- When ready, insert rows with semester_group = 'SEM_1_2'.
