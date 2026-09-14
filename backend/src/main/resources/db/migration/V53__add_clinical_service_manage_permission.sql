-- =====================================================
-- V53__add_clinical_service_manage_permission.sql
-- Add CLINICAL_SERVICE_MANAGE and grant it to ADMIN only
-- (NCL-04-CN-013 / TC-04).
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'CLINICAL_SERVICE_MANAGE', 'CLINICAL SERVICE MANAGE', 'CLINICAL_SERVICE', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = 'CLINICAL_SERVICE_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'CLINICAL_SERVICE_MANAGE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );
