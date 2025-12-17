-- Create cities table
CREATE TABLE IF NOT EXISTS cities (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for active cities
CREATE INDEX IF NOT EXISTS idx_cities_active ON cities (is_active);

-- Insert initial cities data with Uzbek names
INSERT INTO cities (id, name, is_active) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Toshkent', true),
    ('550e8400-e29b-41d4-a716-446655440002', 'Samarqand', true),
    ('550e8400-e29b-41d4-a716-446655440003', 'Buxoro', true),
    ('550e8400-e29b-41d4-a716-446655440004', 'Namangan', true),
    ('550e8400-e29b-41d4-a716-446655440005', 'Andijon', true),
    ('550e8400-e29b-41d4-a716-446655440006', 'Farg''ona', true),
    ('550e8400-e29b-41d4-a716-446655440007', 'Nukus', true),
    ('550e8400-e29b-41d4-a716-446655440008', 'Qarshi', true),
    ('550e8400-e29b-41d4-a716-446655440009', 'Termiz', true),
    ('550e8400-e29b-41d4-a716-446655440010', 'Jizzax', true),
    ('550e8400-e29b-41d4-a716-446655440011', 'Guliston', true),
    ('550e8400-e29b-41d4-a716-446655440012', 'Navoiy', true),
    ('550e8400-e29b-41d4-a716-446655440013', 'Urganch', true),
    ('550e8400-e29b-41d4-a716-446655440014', 'Marg''ilon', true),
    ('550e8400-e29b-41d4-a716-446655440015', 'Angren', true),
    ('550e8400-e29b-41d4-a716-446655440016', 'Bekobod', true),
    ('550e8400-e29b-41d4-a716-446655440017', 'Chirchiq', true),
    ('550e8400-e29b-41d4-a716-446655440018', 'Denov', true),
    ('550e8400-e29b-41d4-a716-446655440019', 'Kitob', true)
ON CONFLICT (name) DO NOTHING;

-- Update updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_cities_updated_at
    BEFORE UPDATE ON cities
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();