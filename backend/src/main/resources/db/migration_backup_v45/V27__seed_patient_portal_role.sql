-- =====================================================
-- V27__seed_patient_portal_role.sql
-- Seed the PATIENT role for patient portal authentication (NCL-14-CN-002 / QTN-23).
-- Patient data scope is enforced at runtime by PatientAccessGuard, so the role
-- intentionally carries no permission grants.
-- =====================================================

INSERT INTO roles (id, name, description, is_system, created_at, updated_at)
SELECT UUID_TO_BIN('77777777-7777-7777-7777-777777777777'), 'PATIENT', 'Patient portal account', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'PATIENT');
