-- =====================================================
-- V37__create_security_alerts_table.sql
-- Security alerts for anomaly access detection (NCL-15 / QTN-25).
-- =====================================================

CREATE TABLE security_alerts (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    access_count INT NOT NULL,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT pk_security_alerts PRIMARY KEY (id)
);

CREATE INDEX idx_security_alerts_user_created
    ON security_alerts (user_id, created_at);

-- Seed SECURITY_ALERT_VIEW permission (idempotent).
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'SECURITY_ALERT_VIEW', REPLACE('SECURITY_ALERT_VIEW', '_', ' '),
       'SECURITY', 'View security alerts.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SECURITY_ALERT_VIEW');

-- Grant SECURITY_ALERT_VIEW to ADMIN only (idempotent).
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'SECURITY_ALERT_VIEW'
WHERE roles.id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
