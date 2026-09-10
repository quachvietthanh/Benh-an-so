-- =====================================================
-- V43__add_access_log_report_export_permission.sql
-- Admin-only permission for the medical record access log
-- report export (NCL-15-CN-004 / QTN-25).
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'ACCESS_LOG_REPORT_EXPORT', REPLACE('ACCESS_LOG_REPORT_EXPORT', '_', ' '),
       'SECURITY', 'Export medical record access log report.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'ACCESS_LOG_REPORT_EXPORT');

-- Grant to ADMIN only (idempotent).
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'ACCESS_LOG_REPORT_EXPORT'
WHERE roles.id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
