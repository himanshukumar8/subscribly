-- ============================================================================
-- V5__migrate_monetary_to_numeric.sql
-- Subscribly Financial Precision Hardening:
-- Migrate floating-point (DOUBLE PRECISION) monetary columns to exact NUMERIC(12,2)
-- Enforce database-level check constraints to guarantee non-negative financial values
-- ============================================================================

-- 1. Alter subscriptions.price to exact NUMERIC(12, 2)
ALTER TABLE subscriptions 
    ALTER COLUMN price TYPE NUMERIC(12, 2) USING ROUND(price::numeric, 2);

-- 2. Alter payments.amount to exact NUMERIC(12, 2)
ALTER TABLE payments 
    ALTER COLUMN amount TYPE NUMERIC(12, 2) USING ROUND(amount::numeric, 2);

-- 3. Normalize any anomalous legacy negative values to 0.00 before constraint enforcement
UPDATE subscriptions SET price = 0.00 WHERE price < 0;
UPDATE payments SET amount = 0.00 WHERE amount < 0;

-- 4. Add non-negative check constraints on monetary values
ALTER TABLE subscriptions 
    ADD CONSTRAINT chk_subscriptions_price_positive CHECK (price >= 0);

ALTER TABLE payments 
    ADD CONSTRAINT chk_payments_amount_positive CHECK (amount >= 0);

