-- ============================================================
-- Migrate old BotState enum values to new naming convention
-- BUYER vs BROKER flow separation
-- ============================================================
-- NOTE: PostgreSQL requires each ALTER TYPE ADD VALUE to be
-- committed before use. Since Flyway runs in a transaction,
-- we need to add all enum values first, then update data in
-- a way that works around the transaction limitation.

-- Step 1: Temporarily set all broker states to IDLE
-- (we'll update them after enum is modified)
UPDATE telegram_sessions
SET state = 'IDLE'
WHERE state IN (
    'AWAITING_LISTING_TYPE',
    'AWAITING_PRICE',
    'AWAITING_CITY_FOR_POSTING',
    'AWAITING_ROOMS',
    'AWAITING_PHONE',
    'AWAITING_DISTRICT',
    'AWAITING_FLOOR',
    'AWAITING_TOTAL_FLOORS',
    'AWAITING_BUILDING_TYPE',
    'AWAITING_CONDITION',
    'AWAITING_CONFIRMATION',
    'AWAITING_CHANNEL_SELECTION',
    'REGISTERING'
);

-- Step 2: Add new broker enum values
-- Each ALTER TYPE must be in its own statement
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_LISTING_TYPE' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_LISTING_TYPE';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_PRICE' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_PRICE';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_CITY' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_CITY';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_ROOMS' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_ROOMS';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_PHONE' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_PHONE';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_DISTRICT' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_DISTRICT';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_FLOOR' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_FLOOR';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_TOTAL_FLOORS' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_TOTAL_FLOORS';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_BUILDING_TYPE' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_BUILDING_TYPE';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_CONDITION' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_CONDITION';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_CONFIRMATION' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_CONFIRMATION';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_enum WHERE enumlabel = 'BROKER_AWAITING_CHANNEL_SELECTION' AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_CHANNEL_SELECTION';
    END IF;
END $$;
