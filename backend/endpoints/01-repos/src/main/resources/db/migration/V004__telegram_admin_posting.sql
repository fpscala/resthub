-- NestHub Admin-Assisted Telegram Posting Feature
-- Migration: V003__telegram_admin_posting.sql

-- Create enum for listing type
CREATE TYPE listing_type AS ENUM ('FOR_SALE', 'FOR_RENT');

-- Add structured columns to listings table
ALTER TABLE listings
ADD COLUMN listing_type listing_type NOT NULL DEFAULT 'FOR_RENT',
ADD COLUMN rooms int4 NULL,
ADD COLUMN district varchar(100) NULL,
ADD COLUMN floor int4 NULL,
ADD COLUMN total_floors int4 NULL,
ADD COLUMN building_type varchar(50) NULL,
ADD COLUMN condition varchar(50) NULL;

-- Add indexes for new structured columns
CREATE INDEX idx_listings_type ON listings(listing_type);
CREATE INDEX idx_listings_rooms ON listings(rooms);
CREATE INDEX idx_listings_district ON listings(district);

-- Add comments for documentation
COMMENT ON COLUMN listings.listing_type IS 'Listing type: SOTILADI (for sale) or IJARAGA (for rent)';
COMMENT ON COLUMN listings.rooms IS 'Number of rooms in the property';
COMMENT ON COLUMN listings.district IS 'District or area within the city';
COMMENT ON COLUMN listings.floor IS 'Floor number where the property is located';
COMMENT ON COLUMN listings.total_floors IS 'Total number of floors in the building';
COMMENT ON COLUMN listings.building_type IS 'Building type: Kvartira (flat), Hovli (house), Ofis (office)';
COMMENT ON COLUMN listings.condition IS 'Property condition: Yaxshi (good), Zo‘r (excellent), Ta’mirlangan (renovated)';