-- =====================================================
-- V97__session_management_and_idle_timeout.sql
-- NCL-01-CN-007: Quản lý phiên làm việc và tự động đăng xuất (QTN-45, QTN-01)
-- 1. Add session_idle_timeout_minutes to clinic_configuration
-- 2. Add ip_address and user_agent to user_sessions
-- 3. Create index for active session lookup
-- 4. Seed SESSION_READ and SESSION_TERMINATE permissions
-- =====================================================

-- 1. Clinic configuration session idle timeout (default 30 mins, 5 to 1440 mins)
ALTER TABLE clinic_configuration
    ADD COLUMN session_idle_timeout_minutes INT NOT NULL DEFAULT 30;

ALTER TABLE clinic_configuration
    ADD CONSTRAINT chk_clinic_configuration_session_idle_timeout
        CHECK (session_idle_timeout_minutes >= 5 AND session_idle_timeout_minutes <= 1440);

-- 2. Add workstation and client context to user_sessions
ALTER TABLE user_sessions
    ADD COLUMN ip_address VARCHAR(45) NULL;

ALTER TABLE user_sessions
    ADD COLUMN user_agent VARCHAR(500) NULL;

-- 3. Index for querying active sessions efficiently
CREATE INDEX idx_user_sessions_active
    ON user_sessions(revoked_at, refresh_expires_at, last_used_at);

-- 4. Seed permissions
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'SESSION_READ', 'SESSION READ', 'SECURITY',
       'View active user sessions list (NCL-01-CN-007)', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SESSION_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'SESSION_TERMINATE', 'SESSION TERMINATE', 'SECURITY',
       'Remotely terminate active user sessions (NCL-01-CN-007)', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SESSION_TERMINATE');

-- Grant SESSION_READ and SESSION_TERMINATE to ADMIN only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code IN ('SESSION_READ', 'SESSION_TERMINATE')
  AND r.id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111') -- ADMIN
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
