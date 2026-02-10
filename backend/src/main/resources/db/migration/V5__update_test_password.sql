-- Update the password hash for the test faculty user
UPDATE users 
SET password_hash = '$2b$12$XXzJ8A8ijytXhCY9CzIvTOUqEmMWCXwlR9r7pmvFbL29Sh7/LgUi6'
WHERE email = 'faculty@example.com';
