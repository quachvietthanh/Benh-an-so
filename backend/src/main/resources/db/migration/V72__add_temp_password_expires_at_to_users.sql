-- =====================================================
-- V69__add_temp_password_expires_at_to_users.sql
-- NCL-01-CN-005 / QTN-28:
-- Add temp_password_expires_at column to users table
-- for temporary password expiration tracking.
-- =====================================================

ALTER TABLE users
    ADD COLUMN temp_password_expires_at TIMESTAMP NULL;
