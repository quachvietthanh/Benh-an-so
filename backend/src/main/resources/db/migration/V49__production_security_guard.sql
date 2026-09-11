-- =====================================================
-- V49__production_security_guard.sql
-- Production Security Hardening Guard (OWASP A07:2021)
-- When running in Fail-Secure mode (LOWER(TRIM('${seed_demo}')) != 'true'):
-- 1. Deactivates all demo staff accounts and scrambles password hash
--    so they cannot be accessed with default credentials.
-- 2. Wipes default admin password hash (Password123@) ONLY IF it is still
--    the known default hash from V2 ($2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK),
--    setting active = FALSE and must_change_password = TRUE,
--    requiring ProductionAdminBootstrap to provision safe credentials.
--    Pre-existing custom admin passwords on upgraded production DBs are preserved.
-- In non-prod environments ('${seed_demo}' = 'true'), demo accounts remain active.
-- =====================================================

UPDATE users
SET active = FALSE,
    password_hash = '$2a$10$LOCKED.ACCOUNT.CANNOT.LOGIN.PROD.GUARD.000000000000000000'
WHERE username IN ('doctor1', 'doctor2', 'doctor_new', 'receptionist1', 'pharmacist1', 'manager1')
  AND LOWER(TRIM('${seed_demo}')) != 'true';

UPDATE users
SET active = FALSE,
    must_change_password = TRUE,
    password_hash = '$2a$10$BOOTSTRAP.ADMIN.PASSWORD.NOT.SET.000000000000000000000'
WHERE username = 'admin'
  AND password_hash = '$2a$10$OY5a1YZ/5Iaz2PcEKjfOveEyy3FVXm7ei9OxTW6jPMyap/Hlk.5sK'
  AND LOWER(TRIM('${seed_demo}')) != 'true';

-- =====================================================
-- 3. F-P2-01: Soft Neutralization Guard for Demo Transactions
-- When running in Fail-Secure mode (LOWER(TRIM('${seed_demo}')) != 'true'):
-- - Cancels sample payments to eliminate fake revenue from financial reports.
-- - Cancels sample appointments from V6 and V14.
-- - Deactivates sample doctor schedules.
-- - Deactivates sample patients (BN000001 - BN000010).
-- Safe across both fresh and existing production databases (0 risk of FK errors).
-- =====================================================

UPDATE payments
SET status = 'CANCELLED',
    refund_reason = 'PROD_GUARD_SAMPLE_CANCELLED'
WHERE id = UUID_TO_BIN('23000000-0000-0000-0000-000000000001')
  AND LOWER(TRIM('${seed_demo}')) != 'true';

UPDATE appointments
SET status = 'CANCELLED',
    cancel_reason = 'PROD_GUARD_SAMPLE_CANCELLED'
WHERE (HEX(id) LIKE 'CCCCCCCCCCCCCCCCCCCCCCCCC0%' OR appointment_code IN ('LH000001', 'LH000002', 'LH000003', 'LH000004', 'LH000005', 'LH000006', 'LH000007', 'LH000008'))
  AND LOWER(TRIM('${seed_demo}')) != 'true';

UPDATE doctor_schedules
SET active = FALSE
WHERE (HEX(id) LIKE '8B00000000000000000000000000000%' OR doctor_id IN (SELECT id FROM users WHERE username IN ('doctor1', 'doctor2')))
  AND LOWER(TRIM('${seed_demo}')) != 'true';

UPDATE patients
SET active = FALSE
WHERE (HEX(id) LIKE 'BBBBBBBBBBBBBBBBBBBBBBBBB0%' OR patient_code IN ('BN000001', 'BN000002', 'BN000003', 'BN000004', 'BN000005', 'BN000006', 'BN000007', 'BN000008', 'BN000009', 'BN000010'))
  AND LOWER(TRIM('${seed_demo}')) != 'true';


