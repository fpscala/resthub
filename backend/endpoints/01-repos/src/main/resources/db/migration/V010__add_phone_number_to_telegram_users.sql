-- Add phone_number column to telegram_users table
ALTER TABLE telegram_users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

-- Add comment
COMMENT ON COLUMN telegram_users.phone_number IS 'Phone number from Telegram (optional, user can share via contact)';
