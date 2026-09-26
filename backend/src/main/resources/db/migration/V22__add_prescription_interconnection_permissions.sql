INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), permission_code, REPLACE(permission_code, '_', ' '), 'PRESCRIPTION', description,
       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'PRESCRIPTION_INTERCONNECTION_SEND' AS permission_code,
           'Send a prescription to the interconnection gateway.' AS description
    UNION ALL
    SELECT 'PRESCRIPTION_INTERCONNECTION_READ',
           'Search prescription interconnection submissions.'
    UNION ALL
    SELECT 'PRESCRIPTION_INTERCONNECTION_RETRY',
           'Retry failed prescription interconnection submissions.'
) AS permission_catalog
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permissions.code = permission_catalog.permission_code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'PRESCRIPTION_INTERCONNECTION_SEND'
WHERE roles.name = 'DOCTOR'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'PRESCRIPTION_INTERCONNECTION_READ',
    'PRESCRIPTION_INTERCONNECTION_RETRY'
)
WHERE roles.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
