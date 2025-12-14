-- Add missing columns to refresh_tokens table for token rotation
ALTER TABLE refresh_tokens
ADD COLUMN replaced_by_token_id UUID REFERENCES refresh_tokens(id),
ADD COLUMN reuse_window_expires_at TIMESTAMPTZ,
ADD COLUMN revoke_reason TEXT;

-- Fix ip_address column type for compatibility with Doobie
ALTER TABLE refresh_tokens ALTER COLUMN ip_address TYPE VARCHAR(45);

-- Create index for performance
CREATE INDEX idx_refresh_tokens_replaced_by ON refresh_tokens(replaced_by_token_id) WHERE replaced_by_token_id IS NOT NULL;