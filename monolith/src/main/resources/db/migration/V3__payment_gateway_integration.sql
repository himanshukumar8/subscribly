-- ============================================================================
-- V3__payment_gateway_integration.sql
-- Subscribly Payment Gateway Integration & Webhook Idempotency
-- Adds gateway reference columns to payments and creates webhook_events table
-- ============================================================================

-- 1. Safely add gateway columns to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(128);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(128);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_signature VARCHAR(256);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'INR';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider VARCHAR(32) DEFAULT 'SIMULATED';

-- 2. Create unique index on gateway_order_id to prevent duplicate gateway orders
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_gateway_order_id
ON payments (gateway_order_id)
WHERE gateway_order_id IS NOT NULL;

-- 3. Create table for database-backed webhook idempotency and audit trail
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128),
    provider VARCHAR(32) DEFAULT 'RAZORPAY',
    status VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(64),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_webhook_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_status ON webhook_events (status);
CREATE INDEX IF NOT EXISTS idx_webhook_events_received_at ON webhook_events (received_at);
