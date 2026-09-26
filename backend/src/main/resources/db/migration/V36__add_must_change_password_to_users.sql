-- =====================================================
-- V36__add_must_change_password_to_users.sql
-- NCL-01-CN-005 / QTN-28 / QTN-01:
-- Add must_change_password flag to users table and
-- seed USER_RESET_PASSWORD permission for ADMIN role.
-- =====================================================

ALTER TABLE users
ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Permission to reset staff passwords (restricted to administrator)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'USER_RESET_PASSWORD',
       'USER RESET PASSWORD',
       'USER',
       'Reset staff password and issue temporary password (QTN-28)',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'USER_RESET_PASSWORD'
);

-- Grant USER_RESET_PASSWORD to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'USER_RESET_PASSWORD'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);
