-- =====================================================
-- V21__grant_follow_up_and_care_log_permissions.sql
-- Grant Follow-Up Reminder (NCL-10-CN-001) and Post-Care Log (NCL-10-CN-002)
-- permissions to RECEPTIONIST, and care-log permissions to DOCTOR.
-- Idempotent: safe on fresh installs where V2 already seeds these rows.
-- =====================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'FOLLOW_UP_REMINDER_CREATE',
    'FOLLOW_UP_REMINDER_READ',
    'FOLLOW_UP_REMINDER_UPDATE',
    'CARE_LOG_CREATE',
    'CARE_LOG_READ'
)
WHERE r.id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'CARE_LOG_CREATE',
    'CARE_LOG_READ'
)
WHERE r.id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
