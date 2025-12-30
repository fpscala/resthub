-- Add BROKER_AWAITING_DESCRIPTION enum value to bot_state
-- This is the new optional description step in the broker posting flow
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum
        WHERE enumlabel = 'BROKER_AWAITING_DESCRIPTION'
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'bot_state')
    ) THEN
        ALTER TYPE bot_state ADD VALUE 'BROKER_AWAITING_DESCRIPTION';
    END IF;
END $$;
