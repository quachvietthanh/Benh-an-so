-- =====================================================
-- V25 - Grant medical record copy issuance (NCL-11-CN-004).
-- MANAGER / ADMIN issue PDF copies of signed medical records.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_COPY', 'MEDICAL RECORD COPY', 'MEDICAL_RECORD',
       'Issue a PDF copy of a signed medical record.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_COPY'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'MEDICAL_RECORD_COPY'
WHERE roles.name IN ('ADMIN', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
