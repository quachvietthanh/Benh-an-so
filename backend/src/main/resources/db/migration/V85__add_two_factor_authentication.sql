-- =====================================================
-- V85__add_two_factor_authentication.sql
-- Two-Factor Authentication for High-Privilege Accounts (NCL-01-CN-006)
-- Adds role-level 2FA configuration and temporary challenge state.
-- =====================================================

-- 1. Role-level 2FA requirement flag (default off for backward compatibility)
ALTER TABLE roles
    ADD COLUMN two_factor_required BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Temporary 2FA challenge state (simulated verification code)
CREATE TABLE two_factor_challenges (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_two_factor_challenges PRIMARY KEY (id),
    CONSTRAINT fk_two_factor_challenges_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_two_factor_challenges_user_created
    ON two_factor_challenges(user_id, created_at);

-- 3. Permission to manage role-level 2FA requirement (admin only)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'TWO_FACTOR_AUTH_MANAGE', 'TWO FACTOR AUTH MANAGE', 'SECURITY',
       'Enable or disable two-factor authentication requirement by role (NCL-01-CN-006)',
       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'TWO_FACTOR_AUTH_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'TWO_FACTOR_AUTH_MANAGE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );
