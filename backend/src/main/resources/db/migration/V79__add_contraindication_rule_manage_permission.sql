-- =====================================================
-- V79__add_contraindication_rule_manage_permission.sql
-- NCL-05-CN-006: Thêm quyền quản lý danh mục quy tắc chống chỉ định
-- Gán cho các vai trò: ADMIN, DOCTOR, PHARMACIST
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'CONTRAINDICATION_RULE_MANAGE', 'CONTRAINDICATION RULE MANAGE', 'PRESCRIPTION',
       'Manage contraindication rules catalog by age, pregnancy, breastfeeding, and chronic disease (NCL-05-CN-006)',
       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'CONTRAINDICATION_RULE_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'CONTRAINDICATION_RULE_MANAGE'
WHERE roles.name IN ('ADMIN', 'DOCTOR', 'PHARMACIST')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
