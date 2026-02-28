-- Add requires_password_reset column to users table
ALTER TABLE users ADD COLUMN requires_password_reset BOOLEAN DEFAULT TRUE;

-- Update the seed admin account email to use an organisation domain
UPDATE users SET email = 'admin@organisation.edu' WHERE email = 'admin@test.com';
