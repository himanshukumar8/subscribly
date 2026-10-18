-- ============================================================================
-- V1__initial_schema.sql
-- Subscribly Core Baseline Schema Definition
-- Creates base entity tables: users, roles, user_roles, subscriptions, payments
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roles_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, roles_id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    plan VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    billing_cycle VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATE,
    end_date DATE,
    started_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    auto_renew BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id),
    user_id BIGINT REFERENCES users(id),
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempts INTEGER DEFAULT 0,
    created_at DATE,
    next_retry_at DATE,
    auto_renew BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_payments_sub_id ON payments (subscription_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments (user_id);
