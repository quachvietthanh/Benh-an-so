-- =====================================================
-- NCL-15-CN-003 - Persistent system configuration.
-- Generic key/value store so future system settings reuse
-- one source of truth (multi-instance safe via the DB).
-- =====================================================

CREATE TABLE system_configuration (
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_system_configuration PRIMARY KEY (config_key),
    CONSTRAINT fk_system_configuration_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Default anonymization mode is OFF.
INSERT INTO system_configuration (config_key, config_value, updated_by, updated_at)
VALUES (
    'anonymization.enabled',
    'false',
    UUID_TO_BIN('00000000-0000-0000-0000-000000000000'),
    CURRENT_TIMESTAMP
);

-- Permissions governing the anonymization mode (ADMIN only).
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'SYSTEM_CONFIG_READ',
       'SYSTEM CONFIG READ',
       'SYSTEM',
       'Read system configuration (e.g. anonymization mode)',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SYSTEM_CONFIG_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'SYSTEM_CONFIG_UPDATE',
       'SYSTEM CONFIG UPDATE',
       'SYSTEM',
       'Update system configuration (e.g. anonymization mode)',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SYSTEM_CONFIG_UPDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code IN ('SYSTEM_CONFIG_READ', 'SYSTEM_CONFIG_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );
