-- =====================================================
-- V35__add_patient_consent_permission.sql
-- NCL-15-CN-001 / QTN-24: Dedicated consent update permission
-- Restricts consent recording/withdrawal to RECEPTIONIST and ADMIN roles.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PATIENT_CONSENT_UPDATE',
       'PATIENT CONSENT UPDATE',
       'PATIENT',
       'Record or withdraw patient personal-data-processing consent',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PATIENT_CONSENT_UPDATE'
);

-- Grant to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_CONSENT_UPDATE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);

-- Grant to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_CONSENT_UPDATE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
    AND rp.permission_id = p.id
);
