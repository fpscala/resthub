-- Add telegram_channel_id and telegram_message_id columns to listings table
-- These fields are used for backwards compatibility with single-channel posting
-- For multi-channel posting, use the listing_channels junction table

ALTER TABLE listings
ADD COLUMN IF NOT EXISTS telegram_channel_id BIGINT;

ALTER TABLE listings
ADD COLUMN IF NOT EXISTS telegram_message_id VARCHAR(255);

-- Index for finding listings by telegram channel
CREATE INDEX IF NOT EXISTS idx_listings_telegram_channel ON listings(telegram_channel_id)
WHERE telegram_channel_id IS NOT NULL;
