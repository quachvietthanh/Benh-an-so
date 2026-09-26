-- =====================================================
-- V28__add_unique_phone_and_lockout_persistence.sql
-- NCL-14-CN-002 remediation:
--   1) Enforce unique phone on users (multiple NULLs remain allowed in MySQL/H2).
--   2) Persist login attempts / lockout so TC-02 survives restarts/instances,
--      with an atomic blocked_until timestamp for accurate expiry.
-- =====================================================

CREATE UNIQUE INDEX uk_users_phone ON users(phone);

CREATE TABLE login_attempts (
    identifier VARCHAR(100) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    blocked_until TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_login_attempts PRIMARY KEY (identifier)
);
