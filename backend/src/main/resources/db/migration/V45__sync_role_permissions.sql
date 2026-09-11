-- =====================================================
-- V44__sync_role_permissions.sql
-- Synchronize role permissions for business integrity (NCL-03, NCL-04, NCL-08).
-- Grants missing permissions to DOCTOR, RECEPTIONIST, and MANAGER roles.
-- =====================================================

-- 1. Grant Clinical Orders, Results and Room Read to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code IN (
    'CLINICAL_ORDER_CREATE',
    'CLINICAL_ORDER_READ',
    'CLINICAL_RESULT_CREATE',
    'CLINICAL_RESULT_UPDATE',
    'CLINICAL_RESULT_FINALIZE',
    'CLINICAL_RESULT_READ',
    'CLINICAL_RESULT_ATTACHMENT_CREATE',
    'CLINICAL_RESULT_ATTACHMENT_READ',
    'CLINICAL_SERVICE_READ',
    'ROOM_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
    AND rp.permission_id = p.id
);

-- 2. Grant Room, Queue Update Status, and User Read to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code IN (
    'ROOM_READ',
    'ROOM_ASSIGNMENT_READ',
    'QUEUE_UPDATE_STATUS',
    'USER_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
    AND rp.permission_id = p.id
);

-- 3. Grant Operational Dashboard and Appointment Read to MANAGER (66666666-6666-6666-6666-666666666666)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), p.id
FROM permissions p
WHERE p.code IN (
    'DASHBOARD_OPERATIONAL_READ',
    'APPOINTMENT_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('66666666-6666-6666-6666-666666666666')
    AND rp.permission_id = p.id
);
