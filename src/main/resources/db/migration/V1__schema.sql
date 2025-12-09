-- ============================================================================
-- NestHub MVP Database Schema
-- PostgreSQL 14+
-- ============================================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE user_role AS ENUM ('user', 'landlord', 'admin');

CREATE TYPE listing_status AS ENUM ('draft', 'pending', 'active', 'archived');

CREATE TYPE document_type AS ENUM ('passport', 'id_card', 'other');

CREATE TYPE verification_status AS ENUM ('pending', 'approved', 'rejected');

CREATE TYPE contract_status AS ENUM ('generated', 'signed', 'cancelled');

-- ============================================================================
-- TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- users
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    email text NOT NULL,
    phone text,
    hashed_password text NOT NULL,
    role user_role NOT NULL DEFAULT 'user',
    verified boolean NOT NULL DEFAULT false,
    verification_requested_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- ----------------------------------------------------------------------------
-- listings
-- ----------------------------------------------------------------------------
CREATE TABLE listings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id uuid NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    price numeric(12,2) NOT NULL,
    currency char(3) NOT NULL DEFAULT 'UZS',
    city text NOT NULL,
    address_text text NOT NULL,
    lat double precision,
    lng double precision,
    area_m2 numeric(8,2) NOT NULL,
    rooms int NOT NULL,
    status listing_status NOT NULL DEFAULT 'draft',
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT listings_owner_id_fk FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT listings_price_check CHECK (price >= 0),
    CONSTRAINT listings_area_check CHECK (area_m2 > 0),
    CONSTRAINT listings_rooms_check CHECK (rooms > 0),
    CONSTRAINT listings_lat_check CHECK (lat IS NULL OR (lat >= -90 AND lat <= 90)),
    CONSTRAINT listings_lng_check CHECK (lng IS NULL OR (lng >= -180 AND lng <= 180))
);

-- ----------------------------------------------------------------------------
-- listing_images
-- ----------------------------------------------------------------------------
CREATE TABLE listing_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id uuid NOT NULL,
    url text NOT NULL,
    sort_order int NOT NULL DEFAULT 0,
    is_primary boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT listing_images_listing_id_fk FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    CONSTRAINT listing_images_sort_order_check CHECK (sort_order >= 0)
);

-- ----------------------------------------------------------------------------
-- verification_requests
-- ----------------------------------------------------------------------------
CREATE TABLE verification_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    document_type document_type NOT NULL,
    document_url text NOT NULL,
    status verification_status NOT NULL DEFAULT 'pending',
    admin_id uuid,
    reviewed_at timestamptz,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT verification_requests_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT verification_requests_admin_id_fk FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ----------------------------------------------------------------------------
-- contracts
-- ----------------------------------------------------------------------------
CREATE TABLE contracts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id uuid NOT NULL,
    renter_id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    contract_url text NOT NULL,
    status contract_status NOT NULL DEFAULT 'generated',
    amount numeric(12,2) NOT NULL,
    currency char(3) NOT NULL DEFAULT 'UZS',
    period_start date NOT NULL,
    period_end date NOT NULL,
    signed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT contracts_listing_id_fk FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE RESTRICT,
    CONSTRAINT contracts_renter_id_fk FOREIGN KEY (renter_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT contracts_landlord_id_fk FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT contracts_amount_check CHECK (amount >= 0),
    CONSTRAINT contracts_period_check CHECK (period_end > period_start),
    CONSTRAINT contracts_renter_landlord_check CHECK (renter_id != landlord_id)
);

-- ----------------------------------------------------------------------------
-- sessions
-- ----------------------------------------------------------------------------
CREATE TABLE sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    refresh_token_hash text NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT sessions_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT sessions_expires_at_check CHECK (expires_at > created_at)
);

-- ----------------------------------------------------------------------------
-- s3_uploads
-- ----------------------------------------------------------------------------
CREATE TABLE s3_uploads (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    key text NOT NULL,
    public_url text,
    uploader_id uuid,
    uploaded boolean NOT NULL DEFAULT false,
    uploaded_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT s3_uploads_uploader_id_fk FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT s3_uploads_key_unique UNIQUE (key)
);

-- ----------------------------------------------------------------------------
-- admin_audit_logs (FUTURE)
-- ----------------------------------------------------------------------------
CREATE TABLE admin_audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id uuid,
    entity_type text NOT NULL,
    entity_id uuid NOT NULL,
    action text NOT NULL,
    details jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT admin_audit_logs_admin_id_fk FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
);

COMMENT ON TABLE admin_audit_logs IS 'FUTURE: Admin audit logging for compliance and tracking';

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Users
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role) WHERE deleted_at IS NULL;

-- Listings
CREATE INDEX idx_listings_owner_id ON listings(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_listings_status ON listings(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_listings_city ON listings(city) WHERE deleted_at IS NULL;
CREATE INDEX idx_listings_published_at ON listings(published_at) WHERE deleted_at IS NULL;

-- Listing Images
CREATE INDEX idx_listing_images_listing_id ON listing_images(listing_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_listing_images_is_primary ON listing_images(listing_id, is_primary) WHERE deleted_at IS NULL AND is_primary = true;

-- Verification Requests
CREATE INDEX idx_verification_requests_user_id ON verification_requests(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_verification_requests_status ON verification_requests(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_verification_requests_admin_id ON verification_requests(admin_id) WHERE deleted_at IS NULL;

-- Contracts
CREATE INDEX idx_contracts_listing_id ON contracts(listing_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_renter_id ON contracts(renter_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_landlord_id ON contracts(landlord_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contracts_status ON contracts(status) WHERE deleted_at IS NULL;

-- Sessions
CREATE INDEX idx_sessions_user_id ON sessions(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at) WHERE deleted_at IS NULL;

-- S3 Uploads
CREATE INDEX idx_s3_uploads_uploader_id ON s3_uploads(uploader_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_s3_uploads_uploaded ON s3_uploads(uploaded) WHERE deleted_at IS NULL;

-- Admin Audit Logs
CREATE INDEX idx_admin_audit_logs_admin_id ON admin_audit_logs(admin_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_admin_audit_logs_entity ON admin_audit_logs(entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs(created_at) WHERE deleted_at IS NULL;
