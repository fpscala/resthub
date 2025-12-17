-- Create Language enum type for Telegram user languages
-- Migration: V008

-- Create enum for language codes matching Scala Language enum
CREATE TYPE language AS ENUM (
  'uz',  -- Uzbek
  'ru',  -- Russian
  'en'   -- English
);

-- Update telegram_users table to use language enum instead of varchar
-- First drop the default constraint
ALTER TABLE telegram_users ALTER COLUMN language_code DROP DEFAULT;

-- Change the column type to use the new enum
ALTER TABLE telegram_users ALTER COLUMN language_code TYPE language
  USING language_code::language;

-- Set back the default using enum value
ALTER TABLE telegram_users ALTER COLUMN language_code SET DEFAULT 'uz';

-- Add constraint to ensure only valid enum values
ALTER TABLE telegram_users ADD CONSTRAINT chk_language_code
  CHECK (language_code IN ('uz', 'ru', 'en'));

-- Comments for documentation
COMMENT ON TYPE language IS 'Enum for supported languages: uz (Uzbek), ru (Russian), en (English)';
COMMENT ON COLUMN telegram_users.language_code IS 'Preferred language using language enum: uz (default), ru, en';