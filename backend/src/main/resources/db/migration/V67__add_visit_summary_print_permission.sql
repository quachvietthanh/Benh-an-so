-- =====================================================
-- V67 - Add visit summary print permission (NCL-04-CN-011).
-- Allows DOCTOR, RECEPTIONIST, ADMIN, and MANAGER to print/export visit summary.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'VISIT_SUMMARY_PRINT', 'VISIT SUMMARY PRINT', 'CLINICAL',
       'View and print clinical visit summary for patient (NCL-04-CN-011)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'VISIT_SUMMARY_PRINT'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'VISIT_SUMMARY_PRINT'
WHERE roles.name IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
