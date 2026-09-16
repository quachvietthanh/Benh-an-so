-- =====================================================
-- V60__add_clinical_order_cancellation_and_permission.sql
-- Add cancellation columns to clinical orders and order items
-- and seed CLINICAL_ORDER_CANCEL permission (NCL-04-CN-008 / QTN-13, QTN-17).
-- =====================================================

-- 1. Add cancellation columns to clinical_orders
ALTER TABLE clinical_orders
    ADD COLUMN cancel_reason VARCHAR(500) NULL,
    ADD COLUMN cancelled_by BINARY(16) NULL,
    ADD COLUMN cancelled_at TIMESTAMP NULL;

ALTER TABLE clinical_orders
    ADD CONSTRAINT fk_orders_cancelled_by
    FOREIGN KEY (cancelled_by)
    REFERENCES users(id);

CREATE INDEX idx_clinical_orders_status_ordered_at
    ON clinical_orders(status, ordered_at);

-- 2. Add cancellation columns to clinical_order_items
ALTER TABLE clinical_order_items
    ADD COLUMN cancel_reason VARCHAR(500) NULL,
    ADD COLUMN cancelled_by BINARY(16) NULL,
    ADD COLUMN cancelled_at TIMESTAMP NULL;

ALTER TABLE clinical_order_items
    ADD CONSTRAINT fk_items_cancelled_by
    FOREIGN KEY (cancelled_by)
    REFERENCES users(id);

CREATE INDEX idx_clinical_order_items_status_created
    ON clinical_order_items(status, created_at);

-- 3. Seed CLINICAL_ORDER_CANCEL permission
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'CLINICAL_ORDER_CANCEL', 'CLINICAL ORDER CANCEL', 'CLINICAL_ORDER', 'Cancel clinical orders or clinical order items with reason', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = 'CLINICAL_ORDER_CANCEL'
);

-- 4. Grant CLINICAL_ORDER_CANCEL to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'CLINICAL_ORDER_CANCEL'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );

-- 5. Grant CLINICAL_ORDER_CANCEL to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code = 'CLINICAL_ORDER_CANCEL'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
        AND rp.permission_id = p.id
  );
