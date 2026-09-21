-- =====================================================
-- V77__add_inventory_report_view_permission.sql
-- NCL-06-CN-013 (TC-04): Báo cáo xuất nhập tồn theo kỳ.
-- Introduce a dedicated permission so the inventory stock report is not
-- reachable by DOCTOR (who holds PHARMACY_READ for medicine catalog lookups).
-- Actors: ADMIN, PHARMACIST, MANAGER. Doctor and Receptionist are denied.
-- =====================================================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'INVENTORY_REPORT_VIEW',
       'INVENTORY REPORT VIEW',
       'INVENTORY',
       'View the periodic stock in/out inventory report (NCL-06-CN-013).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'INVENTORY_REPORT_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'INVENTORY_REPORT_VIEW'
WHERE r.name IN ('ADMIN', 'PHARMACIST', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
