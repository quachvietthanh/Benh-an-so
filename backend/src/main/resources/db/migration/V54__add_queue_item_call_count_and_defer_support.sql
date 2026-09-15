ALTER TABLE queue_items ADD COLUMN call_count INT NOT NULL DEFAULT 0;

UPDATE queue_items SET call_count = 1 WHERE called_at IS NOT NULL;

INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code IN (
    'QUEUE_CALL_NEXT',
    'QUEUE_UPDATE_STATUS',
    'USER_READ'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
    AND rp.permission_id = p.id
);
