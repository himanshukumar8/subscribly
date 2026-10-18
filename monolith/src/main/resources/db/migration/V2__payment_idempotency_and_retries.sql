-- ============================================================================
-- V2__payment_idempotency_and_retries.sql
-- Production-safe idempotent schema evolution for Subscribly
-- Applies unique constraints, precise audit timestamps, retry indices, and optimistic locking
-- ============================================================================

-- 1. Safely add optimistic locking version columns
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
UPDATE subscriptions SET version = 0 WHERE version IS NULL;
ALTER TABLE subscriptions ALTER COLUMN version SET NOT NULL;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
UPDATE payments SET version = 0 WHERE version IS NULL;
ALTER TABLE payments ALTER COLUMN version SET NOT NULL;

-- 2. Safely add idempotency and retry columns to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS created_at_instant TIMESTAMP WITH TIME ZONE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS next_retry_instant TIMESTAMP WITH TIME ZONE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS processed_at_instant TIMESTAMP WITH TIME ZONE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(512);

-- 3. Backfill legacy records to avoid unique constraint violations
UPDATE payments
SET idempotency_key = 'migrated-legacy-' || id
WHERE idempotency_key IS NULL;

-- 4. Create unique constraint/index for idempotency key
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_idempotency_key
ON payments (idempotency_key);

-- 5. Create composite index to optimize scheduler micro-batch polling
CREATE INDEX IF NOT EXISTS idx_payments_retry_lookup
ON payments (status, next_retry_instant);

-- 6. Create index on subscription expiry for continuous auto-renew sweep
CREATE INDEX IF NOT EXISTS idx_subscriptions_due_lookup
ON subscriptions (status, expires_at);
