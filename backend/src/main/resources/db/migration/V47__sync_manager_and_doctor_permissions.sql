-- =====================================================
-- V47__sync_manager_and_doctor_permissions.sql
-- Grant missing permissions for MANAGER and DOCTOR roles
-- (US-74: Clinic Manager and Doctor Room Assignment)
-- =====================================================

-- 1. Grant ROOM_ASSIGNMENT_READ to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code = 'ROOM_ASSIGNMENT_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
        AND rp.permission_id = p.id
  );

-- 2. Grant full management & operational permissions to MANAGER (66666666-6666-6666-6666-666666666666)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), p.id
FROM permissions p
WHERE p.code IN (
    'USER_READ',
    'DOCTOR_SCHEDULE_READ',
    'DOCTOR_SCHEDULE_UPDATE',
    'DOCTOR_TIMEOFF_READ',
    'DOCTOR_TIMEOFF_CREATE',
    'DOCTOR_TIMEOFF_CANCEL',
    'ROOM_READ',
    'ROOM_ASSIGNMENT_READ',
    'CLINIC_CONFIGURATION_READ',
    'CLINIC_CONFIGURATION_UPDATE',
    'MEDICAL_RECORD_READ',
    'MEDICAL_RECORD_TEMPLATE_MANAGE'
)
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('66666666-6666-6666-6666-666666666666')
        AND rp.permission_id = p.id
  );

-- 3. Grant QUEUE_CALL_NEXT to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code = 'QUEUE_CALL_NEXT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
        AND rp.permission_id = p.id
  );

