-- =====================================================
-- V90 - Grant medical record export permission (NCL-11-CN-007).
-- ADMIN / MANAGER export medical records according to data exchange structure.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_EXPORT', 'MEDICAL RECORD EXPORT', 'MEDICAL_RECORD',
       'Export signed medical records according to standard data exchange structure (NCL-11-CN-007).', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_EXPORT'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'MEDICAL_RECORD_EXPORT'
WHERE roles.name IN ('ADMIN', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
