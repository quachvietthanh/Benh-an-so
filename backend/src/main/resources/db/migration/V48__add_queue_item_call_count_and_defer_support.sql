-- =====================================================
-- V48__add_queue_item_call_count_and_defer_support.sql
-- NCL-03-CN-009: Gọi lại và tạm hoãn bệnh nhân vắng trong hàng đợi
-- NCL-03-CN-008: Xác nhận lịch hẹn (Phân quyền bổ sung)
-- NCL-03-CN-007: Đổi lịch hẹn tại quầy (Phân quyền bổ sung)
-- Acceptance Criteria: TC-01, TC-02, TC-03, TC-04
-- =====================================================

-- 1. Schema alteration: add call_count column to queue_items
ALTER TABLE queue_items ADD COLUMN call_count INT NOT NULL DEFAULT 0;

-- Backfill call_count for existing called items
UPDATE queue_items SET call_count = 1 WHERE called_at IS NOT NULL;

-- 2. Ensure all relevant permissions exist in the permissions catalog
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), req.code, REPLACE(req.code, '_', ' '), req.module, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'QUEUE_CREATE' AS code, 'QUEUE' AS module UNION ALL
    SELECT 'QUEUE_CALL_NEXT', 'QUEUE' UNION ALL
    SELECT 'QUEUE_UPDATE_STATUS', 'QUEUE' UNION ALL
    SELECT 'QUEUE_VIEW', 'QUEUE' UNION ALL
    SELECT 'QUEUE_COUNT', 'QUEUE' UNION ALL
    SELECT 'APPOINTMENT_CREATE', 'APPOINTMENT' UNION ALL
    SELECT 'APPOINTMENT_READ', 'APPOINTMENT' UNION ALL
    SELECT 'APPOINTMENT_UPDATE', 'APPOINTMENT' UNION ALL
    SELECT 'APPOINTMENT_DELETE', 'APPOINTMENT' UNION ALL
    SELECT 'USER_READ', 'USER' UNION ALL
    SELECT 'ROOM_READ', 'ROOM' UNION ALL
    SELECT 'ROOM_ASSIGNMENT_READ', 'ROOM'
) AS req
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = req.code
);

-- 3. Grant permissions to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
-- NCL-03-CN-007: APPOINTMENT_READ, APPOINTMENT_UPDATE, USER_READ, ROOM_READ, ROOM_ASSIGNMENT_READ
-- NCL-03-CN-008: APPOINTMENT_READ, APPOINTMENT_UPDATE
-- NCL-03-CN-009: QUEUE_CREATE, QUEUE_CALL_NEXT, QUEUE_UPDATE_STATUS, QUEUE_VIEW, QUEUE_COUNT
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code IN (
    'APPOINTMENT_READ',
    'APPOINTMENT_UPDATE',
    'USER_READ',
    'ROOM_READ',
    'ROOM_ASSIGNMENT_READ',
    'QUEUE_CREATE',
    'QUEUE_CALL_NEXT',
    'QUEUE_UPDATE_STATUS',
    'QUEUE_VIEW',
    'QUEUE_COUNT'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
    AND rp.permission_id = p.id
);

-- 4. Grant permissions to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code IN (
    'APPOINTMENT_CREATE',
    'APPOINTMENT_READ',
    'APPOINTMENT_UPDATE',
    'APPOINTMENT_DELETE',
    'QUEUE_CREATE',
    'QUEUE_CALL_NEXT',
    'QUEUE_UPDATE_STATUS',
    'QUEUE_VIEW',
    'QUEUE_COUNT',
    'USER_READ',
    'ROOM_READ',
    'ROOM_ASSIGNMENT_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);

-- 5. Grant permissions to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code IN (
    'APPOINTMENT_READ',
    'QUEUE_CALL_NEXT',
    'QUEUE_UPDATE_STATUS',
    'QUEUE_VIEW',
    'QUEUE_COUNT',
    'ROOM_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
    AND rp.permission_id = p.id
);

-- 6. Grant permissions to MANAGER (66666666-6666-6666-6666-666666666666)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), p.id
FROM permissions p
WHERE p.code IN (
    'APPOINTMENT_READ',
    'QUEUE_VIEW',
    'QUEUE_COUNT'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('66666666-6666-6666-6666-666666666666')
    AND rp.permission_id = p.id
);
