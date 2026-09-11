-- =====================================================
-- V49__production_security_guard.sql
-- Production Security Hardening Guard (OWASP A07:2021)
-- When running in Production environment ('${env}' = 'prod'):
-- 1. Deactivates all demo staff accounts and scrambles password hash
--    so they cannot be accessed with default credentials.
-- 2. Deactivates default admin account and enforces must_change_password.
-- In non-prod environments (local, dev, test), demo accounts remain active.
-- =====================================================

UPDATE users
SET active = FALSE,
    password_hash = '$2a$10$LOCKED.ACCOUNT.CANNOT.LOGIN.PROD.GUARD.000000000000000000'
WHERE username IN ('doctor1', 'doctor2', 'doctor_new', 'receptionist1', 'pharmacist1', 'manager1')
  AND '${env}' = 'prod';

UPDATE users
SET active = FALSE,
    must_change_password = TRUE
WHERE username = 'admin'
  AND '${env}' = 'prod';
