-- NestHub Rental Marketplace - Initial Database Schema
-- File: V001__init.sql
-- Description: Role-Based Access Control (RBAC) and User Management
-- Platform: Web Application

-- ==========================================
-- ENUMS AND TYPES
-- ==========================================

CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING_VERIFICATION');

-- ==========================================
-- CORE TABLES - RBAC (Role-Based Access Control)
-- ==========================================

-- Privileges table
CREATE TABLE privileges (
    name VARCHAR(100) PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Role-Privilege mapping
CREATE TABLE role_privileges (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    privilege VARCHAR(100) NOT NULL REFERENCES privileges(name) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, privilege)
);

-- ==========================================
-- USERS TABLE
-- ==========================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Authentication
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hashed

    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,

    -- Role & Status
    role_id UUID NOT NULL REFERENCES roles(id),
    status user_status DEFAULT 'ACTIVE',

    -- Market
    market_id UUID,

    -- Soft delete
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Verification
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_phone ON users(phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_market ON users(market_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_deleted ON users(deleted_at);

-- ==========================================
-- REFRESH TOKENS (JWT)
-- ==========================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,  -- SHA-256 hash

    -- Device Information
    device_info JSONB,  -- {platform: "web", user_agent: "...", ip: "..."}
    ip_address INET,
    user_agent TEXT,

    -- Lifecycle
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for refresh tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash) WHERE NOT revoked;
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE NOT revoked;

-- ==========================================
-- SEED DATA - PRIVILEGES
-- ==========================================

INSERT INTO privileges (name, group_name, description) VALUES
-- User management
('create_user', 'users', 'Create new users'),
('update_user', 'users', 'Update user information'),
('update_any_user', 'users', 'Update any user information'),
('delete_user', 'users', 'Delete users'),
('view_users', 'users', 'View users'),
('create_super_user', 'users', 'Create super admin users'),

-- Role management
('create_role', 'roles', 'Create new roles'),
('update_role', 'roles', 'Update roles'),
('delete_role', 'roles', 'Delete roles'),
('view_roles', 'roles', 'View roles'),

-- Listings management
('admin_listings_view_all', 'listings', 'View all listings (admin)'),
('admin_listings_approve', 'listings', 'Approve listings'),
('admin_listings_reject', 'listings', 'Reject listings'),

-- Assets management
('create_asset', 'assets', 'Create assets (images, files)');

-- ==========================================
-- SEED DATA - ROLES
-- ==========================================

INSERT INTO roles (id, name, description, is_system) VALUES
('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrator with full privileges', TRUE),
('00000000-0000-0000-0000-000000000002', 'USER', 'Regular user role', TRUE);

-- ==========================================
-- SEED DATA - ROLE PRIVILEGES
-- ==========================================

-- ADMIN: All privileges
INSERT INTO role_privileges (role_id, privilege)
SELECT '00000000-0000-0000-0000-000000000001', name FROM privileges;

-- USER: Basic privileges
INSERT INTO role_privileges (role_id, privilege) VALUES
('00000000-0000-0000-0000-000000000002', 'view_users');

-- ==========================================
-- TRIGGERS
-- ==========================================

-- Update updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to tables with updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();