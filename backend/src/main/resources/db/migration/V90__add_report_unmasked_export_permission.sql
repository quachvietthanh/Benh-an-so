-- =====================================================
-- V90__add_report_unmasked_export_permission.sql
-- High-privilege permission for unmasked report export
-- (NCL-15-CN-007 / QTN-43).
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'REPORT_UNMASKED_EXPORT', REPLACE('REPORT_UNMASKED_EXPORT', '_', ' '),
       'REPORT', 'Export report with unmasked patient identifying information.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'REPORT_UNMASKED_EXPORT');

-- Grant to ADMIN by default (idempotent).
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'REPORT_UNMASKED_EXPORT'
WHERE roles.id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
