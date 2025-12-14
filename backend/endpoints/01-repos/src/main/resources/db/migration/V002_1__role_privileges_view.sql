-- NestHub Role Privileges View
-- Migration: V002_1__role_privileges_view.sql

-- Create view for roles with their privileges
CREATE OR REPLACE VIEW role_privileges_view AS
SELECT
    r.id,
    r.name,
    COALESCE(array_agg(rp.privilege) FILTER (WHERE rp.privilege IS NOT NULL), ARRAY[]::VARCHAR[]) AS privileges,
    r.description,
    r.is_system,
    r.created_at,
    r.updated_at
FROM roles r
LEFT JOIN role_privileges rp ON r.id = rp.role_id
GROUP BY r.id, r.name, r.description, r.is_system, r.created_at, r.updated_at;

-- Note: Indexes cannot be created on views in PostgreSQL
-- Performance will be handled by indexes on the base tables

-- Comment
COMMENT ON VIEW role_privileges_view IS 'View that includes roles with their associated privileges as JSON array';