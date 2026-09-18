-- =====================================================
-- V67__add_admin_operation_log_read_permission.sql
-- NCL-09-CN-006 (QTN-31): Nhật ký thao tác quản trị hệ thống.
-- Actors: ADMIN and MANAGER. Doctor is denied (TC-03).
-- The administrative operation log is a read-only view over the existing
-- audit_logs table (resource types USER/ROLE/PERMISSION/MEDICINE/
-- SERVICE_CATALOG/SERVICE_PRICE), protected by this dedicated permission.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'ADMIN_OPERATION_LOG_READ',
       'ADMIN OPERATION LOG READ',
       'AUDIT',
       'View administrative operation logs (NCL-09-CN-006 / QTN-31).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'ADMIN_OPERATION_LOG_READ'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'ADMIN_OPERATION_LOG_READ'
WHERE r.name IN ('ADMIN', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
