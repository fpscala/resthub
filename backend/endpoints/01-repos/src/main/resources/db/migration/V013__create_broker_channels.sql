-- Create chat_type enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'chat_type') THEN
        CREATE TYPE chat_type AS ENUM ('CHANNEL', 'GROUP', 'SUPERGROUP');
    END IF;
END $$;

-- Create broker_channels table for auto-discovered channels
-- This stores channels/groups where bot AND broker have posting permissions
CREATE TABLE IF NOT EXISTS broker_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telegram_user_id BIGINT NOT NULL,           -- broker's telegram ID
    telegram_chat_id BIGINT NOT NULL,           -- channel/group telegram ID
    chat_title VARCHAR(255) NOT NULL,           -- channel/group name
    chat_type chat_type NOT NULL,               -- CHANNEL, GROUP, or SUPERGROUP
    chat_username VARCHAR(255),                 -- @username if public
    bot_is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    user_can_post BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,    -- for soft disable
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_broker_channel UNIQUE (telegram_user_id, telegram_chat_id)
);

-- Index for fast lookup by broker
CREATE INDEX IF NOT EXISTS idx_broker_channels_user_id ON broker_channels(telegram_user_id);

-- Index for finding all brokers in a channel
CREATE INDEX IF NOT EXISTS idx_broker_channels_chat_id ON broker_channels(telegram_chat_id);

-- Index for active channels with posting permission
CREATE INDEX IF NOT EXISTS idx_broker_channels_active_postable ON broker_channels(telegram_user_id, is_active, user_can_post)
WHERE is_active = TRUE AND user_can_post = TRUE;

-- Create listing_channels junction table for multi-channel posting
-- Links one listing to multiple channels where it was posted
CREATE TABLE IF NOT EXISTS listing_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    telegram_chat_id BIGINT NOT NULL,
    telegram_message_id BIGINT,                 -- message ID in that channel
    posted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_listing_channel UNIQUE (listing_id, telegram_chat_id)
);

-- Index for finding all channels for a listing
CREATE INDEX IF NOT EXISTS idx_listing_channels_listing_id ON listing_channels(listing_id);
