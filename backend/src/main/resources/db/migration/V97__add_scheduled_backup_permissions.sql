-- =====================================================
-- V97__add_scheduled_backup_permissions.sql
-- NCL-09-CN-009: schedule management + integrity verification permissions.
-- Grant to ADMIN only (idempotent).
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'BACKUP_SCHEDULE_READ',
       'BACKUP SCHEDULE READ',
       'BACKUP',
       'View the automatic backup schedule (NCL-09-CN-009).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BACKUP_SCHEDULE_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'BACKUP_SCHEDULE_UPDATE',
       'BACKUP SCHEDULE UPDATE',
       'BACKUP',
       'Configure and enable/disable the automatic backup schedule (NCL-09-CN-009).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BACKUP_SCHEDULE_UPDATE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'BACKUP_VERIFY',
       'BACKUP VERIFY',
       'BACKUP',
       'Verify the integrity of the latest backup (NCL-09-CN-009).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BACKUP_VERIFY');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'BACKUP_SCHEDULE_READ',
    'BACKUP_SCHEDULE_UPDATE',
    'BACKUP_VERIFY'
)
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
