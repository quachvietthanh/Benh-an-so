-- =====================================================
-- V64__add_prescription_dispense_history_read_permission.sql
-- NCL-06-CN-008: Cấp phát một phần khi tồn kho không đủ (TC-05)
-- Dispensing history viewing is a distinct capability from the general
-- PRESCRIPTION_READ permission. Introduce a dedicated permission so MANAGER
-- (and other audit/oversight roles) can view dispensing history without being
-- granted full prescription read access.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PRESCRIPTION_DISPENSE_HISTORY_READ',
       'PRESCRIPTION DISPENSE HISTORY READ',
       'PRESCRIPTION',
       'View prescription dispensing history events (NCL-06-CN-008, TC-05).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PRESCRIPTION_DISPENSE_HISTORY_READ'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PRESCRIPTION_DISPENSE_HISTORY_READ'
WHERE r.name IN ('ADMIN', 'DOCTOR', 'PHARMACIST', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
