-- Telegram Bot Tables and Integration
-- Migration: V003

-- Telegram users mapping to NestHub users
CREATE TABLE telegram_users (
  telegram_id BIGINT PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  username VARCHAR(255),
  first_name VARCHAR(255) NOT NULL,
  language_code VARCHAR(10) NOT NULL DEFAULT 'uz',
  is_registered BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_interaction_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create enum for bot session states
CREATE TYPE bot_state AS ENUM (
  'IDLE',
  'AWAITING_CITY',
  'AWAITING_PRICE_RANGE',
  'REGISTERING'
);

-- Telegram session state for conversational flow
CREATE TABLE telegram_sessions (
  telegram_id BIGINT PRIMARY KEY REFERENCES telegram_users(telegram_id) ON DELETE CASCADE,
  state bot_state NOT NULL DEFAULT 'IDLE',
  context JSONB,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Telegram subscriptions for new listing notifications
CREATE TABLE telegram_subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  telegram_id BIGINT NOT NULL REFERENCES telegram_users(telegram_id) ON DELETE CASCADE,
  city VARCHAR(100),
  min_price NUMERIC(12,2),
  max_price NUMERIC(12,2),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for telegram tables
CREATE INDEX idx_telegram_users_user_id ON telegram_users(user_id);
CREATE INDEX idx_telegram_users_username ON telegram_users(username) WHERE username IS NOT NULL;
CREATE INDEX idx_telegram_sessions_state ON telegram_sessions(state);
CREATE INDEX idx_telegram_subscriptions_telegram_id ON telegram_subscriptions(telegram_id);
CREATE INDEX idx_telegram_subscriptions_active ON telegram_subscriptions(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_telegram_subscriptions_city ON telegram_subscriptions(city) WHERE is_active = TRUE;

-- Function to auto-update updated_at timestamp for telegram_subscriptions
CREATE OR REPLACE FUNCTION update_telegram_subscriptions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for telegram_subscriptions
CREATE TRIGGER trigger_update_telegram_subscriptions_updated_at
  BEFORE UPDATE ON telegram_subscriptions
  FOR EACH ROW
  EXECUTE FUNCTION update_telegram_subscriptions_updated_at();

-- Function to auto-update last_interaction_at for telegram_users
CREATE OR REPLACE FUNCTION update_telegram_last_interaction()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE telegram_users
  SET last_interaction_at = CURRENT_TIMESTAMP
  WHERE telegram_id = NEW.telegram_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update last_interaction when session is updated
CREATE TRIGGER trigger_update_telegram_last_interaction
  AFTER UPDATE ON telegram_sessions
  FOR EACH ROW
  EXECUTE FUNCTION update_telegram_last_interaction();

-- Comments for documentation
COMMENT ON TABLE telegram_users IS 'Telegram users mapped to NestHub users for bot integration';
COMMENT ON TABLE telegram_sessions IS 'Conversation state management for Telegram bot';
COMMENT ON TABLE telegram_subscriptions IS 'User subscriptions for new listing notifications via Telegram';

COMMENT ON COLUMN telegram_users.telegram_id IS 'Telegram unique user ID';
COMMENT ON COLUMN telegram_users.user_id IS 'Linked NestHub user ID (nullable if not registered yet)';
COMMENT ON COLUMN telegram_users.language_code IS 'Preferred language: uz (default), ru, en';
COMMENT ON COLUMN telegram_users.is_registered IS 'Whether user has completed registration in NestHub';

COMMENT ON COLUMN telegram_sessions.state IS 'Current conversation state: IDLE, AWAITING_CITY, AWAITING_PRICE_RANGE, REGISTERING';
COMMENT ON COLUMN telegram_sessions.context IS 'JSON context for the current conversation (e.g., selected city, price range)';

COMMENT ON COLUMN telegram_subscriptions.city IS 'Filter by city (nullable for all cities)';
COMMENT ON COLUMN telegram_subscriptions.min_price IS 'Minimum price filter (nullable for no minimum)';
COMMENT ON COLUMN telegram_subscriptions.max_price IS 'Maximum price filter (nullable for no maximum)';
