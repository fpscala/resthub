-- Add BROKER_AWAITING_IMAGES enum value to bot_state
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumlabel = 'BROKER_AWAITING_IMAGES'
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')
    ) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_IMAGES';
    END IF;
END $$;
