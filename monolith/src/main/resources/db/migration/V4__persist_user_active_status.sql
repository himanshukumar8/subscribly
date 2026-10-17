-- ============================================================================
-- V4__persist_user_active_status.sql
-- Subscribly User Management - Persist Account Active Status
-- Adds active column to users table, safely backfills existing records,
-- and enforces NOT NULL constraint.
-- ============================================================================

-- 1. Safely add active column with default true
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

-- 2. Backfill existing records that may have NULL active values
UPDATE users SET active = TRUE WHERE active IS NULL;

-- 3. Enforce NOT NULL constraint once backfill is complete
ALTER TABLE users ALTER COLUMN active SET NOT NULL;

-- 4. Ensure baseline system roles exist for authorization and concurrency locks
INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
