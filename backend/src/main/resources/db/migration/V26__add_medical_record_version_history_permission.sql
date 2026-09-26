-- =====================================================
-- V26 - Grant medical record version history read (NCL-11-CN-003).
-- MANAGER / ADMIN view medical record version/amendment history.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_VERSION_HISTORY_READ', 'MEDICAL RECORD VERSION HISTORY READ', 'MEDICAL_RECORD',
       'View medical record version and amendment history.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_VERSION_HISTORY_READ'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'MEDICAL_RECORD_VERSION_HISTORY_READ'
WHERE roles.name IN ('ADMIN', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
