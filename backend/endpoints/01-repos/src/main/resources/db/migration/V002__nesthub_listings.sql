-- NestHub Rental Marketplace Listings and Contracts Tables
-- Migration: V002

-- Create enum for listing status
CREATE TYPE listing_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- Listings table
CREATE TABLE listings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  price NUMERIC(12,2) NOT NULL CHECK (price > 0),
  city VARCHAR(100) NOT NULL,
  images TEXT[] NOT NULL,
  status listing_status NOT NULL DEFAULT 'PENDING',
  rejection_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approved_at TIMESTAMPTZ,
  approved_by UUID REFERENCES users(id)
);

-- Indexes for listings table (optimized for common queries)
CREATE INDEX idx_listings_owner ON listings(owner_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_city ON listings(city);
CREATE INDEX idx_listings_price ON listings(price);
CREATE INDEX idx_listings_created ON listings(created_at DESC);
CREATE INDEX idx_listings_approved ON listings(approved_at DESC) WHERE status = 'APPROVED';

-- Contracts table
CREATE TABLE contracts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  pdf_url VARCHAR(500) NOT NULL,
  generated_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for contracts table
CREATE INDEX idx_contracts_listing ON contracts(listing_id);

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_listings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update updated_at on listings table
CREATE TRIGGER trigger_update_listings_updated_at
  BEFORE UPDATE ON listings
  FOR EACH ROW
  EXECUTE FUNCTION update_listings_updated_at();

-- Comments for documentation
COMMENT ON TABLE listings IS 'Rental property listings for NestHub marketplace';
COMMENT ON TABLE contracts IS 'Generated PDF contracts for rental agreements';
COMMENT ON COLUMN listings.status IS 'Listing moderation status: PENDING (awaiting approval), APPROVED (visible to public), REJECTED (denied by admin)';
COMMENT ON COLUMN listings.images IS 'Array of S3/MinIO URLs for listing images';
COMMENT ON COLUMN listings.rejection_reason IS 'Admin explanation if listing was rejected';
