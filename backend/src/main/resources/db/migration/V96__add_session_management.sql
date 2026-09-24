-- =====================================================
-- NCL-01-CN-007 - Session management & automatic logout.
-- The configurable inactivity timeout and warning threshold
-- live in the existing clinic_configuration singleton (QTN-45).
-- =====================================================

ALTER TABLE clinic_configuration
    ADD COLUMN session_timeout_minutes INT NOT NULL DEFAULT 30;

ALTER TABLE clinic_configuration
    ADD CONSTRAINT chk_clinic_configuration_session_timeout
    CHECK (session_timeout_minutes >= 1 AND session_timeout_minutes <= 1440);

ALTER TABLE clinic_configuration
    ADD COLUMN session_warning_minutes INT NOT NULL DEFAULT 5;

ALTER TABLE clinic_configuration
    ADD CONSTRAINT chk_clinic_configuration_session_warning
    CHECK (session_warning_minutes >= 0);

-- Session-management permissions (seeded to ADMIN only).
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'SESSION_READ',
       'SESSION READ',
       'SESSION',
       'View open user sessions',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SESSION_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'SESSION_TERMINATE',
       'SESSION TERMINATE',
       'SESSION',
       'Remotely terminate an open user session',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SESSION_TERMINATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code IN ('SESSION_READ', 'SESSION_TERMINATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );
