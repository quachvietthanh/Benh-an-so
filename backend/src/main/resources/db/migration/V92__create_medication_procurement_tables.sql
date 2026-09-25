-- =====================================================
-- V90__create_medication_procurement_tables.sql
-- NCL-06-CN-012: Dự trù mua thuốc và phiếu đặt hàng
-- Schema for medication procurement plans, items, code sequences and permissions
-- MySQL 8.x
-- =====================================================

-- ===========================
-- 1. Medication Procurement Plans
-- ===========================

CREATE TABLE medication_procurement_plans (
    id BINARY(16) NOT NULL,
    plan_code VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by BINARY(16) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    total_suggested_quantity INT NOT NULL DEFAULT 0,
    total_proposed_quantity INT NOT NULL DEFAULT 0,
    total_approved_quantity INT NOT NULL DEFAULT 0,
    note TEXT NULL,
    submitted_at TIMESTAMP NULL,
    approved_by BINARY(16) NULL,
    approved_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_medication_procurement_plans PRIMARY KEY (id),
    CONSTRAINT uk_medication_procurement_plans_code UNIQUE (plan_code),
    CONSTRAINT fk_medication_procurement_plans_creator
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_medication_procurement_plans_approver
        FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT chk_procurement_plans_status CHECK (
        status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED')
    ),
    CONSTRAINT chk_procurement_plans_dates CHECK (period_end_date >= period_start_date),
    CONSTRAINT chk_procurement_plans_proposed_qty CHECK (total_proposed_quantity >= 0),
    CONSTRAINT chk_procurement_plans_approved_qty CHECK (total_approved_quantity >= 0)
);

CREATE INDEX idx_procurement_plans_status ON medication_procurement_plans(status);
CREATE INDEX idx_procurement_plans_created_by ON medication_procurement_plans(created_by);
CREATE INDEX idx_procurement_plans_created_at ON medication_procurement_plans(created_at);

-- ===========================
-- 2. Medication Procurement Items
-- ===========================

CREATE TABLE medication_procurement_items (
    id BINARY(16) NOT NULL,
    plan_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    current_stock INT NOT NULL DEFAULT 0,
    min_stock_threshold INT NOT NULL DEFAULT 0,
    previous_period_consumption INT NOT NULL DEFAULT 0,
    suggested_quantity INT NOT NULL DEFAULT 0,
    proposed_quantity INT NOT NULL,
    approved_quantity INT NOT NULL DEFAULT 0,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medication_procurement_items PRIMARY KEY (id),
    CONSTRAINT fk_procurement_items_plan
        FOREIGN KEY (plan_id) REFERENCES medication_procurement_plans(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_procurement_items_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT uk_procurement_items_plan_medicine UNIQUE (plan_id, medicine_id),
    CONSTRAINT chk_procurement_items_proposed_qty CHECK (proposed_quantity > 0),
    CONSTRAINT chk_procurement_items_approved_qty CHECK (approved_quantity >= 0)
);

CREATE INDEX idx_procurement_items_plan ON medication_procurement_items(plan_id);
CREATE INDEX idx_procurement_items_medicine ON medication_procurement_items(medicine_id);

-- ===========================
-- 3. Code Sequences (DT - Dự trù)
-- ===========================

CREATE TABLE medication_procurement_code_sequences (
    code_prefix VARCHAR(10) NOT NULL,
    `last_value` BIGINT NOT NULL,
    CONSTRAINT pk_medication_procurement_code_sequences PRIMARY KEY (code_prefix),
    CONSTRAINT chk_procurement_code_sequences_last_value CHECK (`last_value` >= 0)
);

INSERT INTO medication_procurement_code_sequences (code_prefix, `last_value`)
VALUES ('DT', 0);

-- ===========================
-- 4. Permissions & Role Assignments
-- ===========================

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'MEDICATION_PROCUREMENT_READ',
       'Xem dự trù mua thuốc',
       'INVENTORY',
       'Xem gợi ý số lượng và danh sách phiếu dự trù mua thuốc (NCL-06-CN-012).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICATION_PROCUREMENT_READ'
);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'MEDICATION_PROCUREMENT_CREATE',
       'Lập dự trù mua thuốc',
       'INVENTORY',
       'Lập, điều chỉnh và gửi duyệt phiếu dự trù mua thuốc (NCL-06-CN-012).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICATION_PROCUREMENT_CREATE'
);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'MEDICATION_PROCUREMENT_APPROVE',
       'Phê duyệt dự trù mua thuốc',
       'INVENTORY',
       'Phê duyệt hoặc từ chối phiếu dự trù mua thuốc (NCL-06-CN-012).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICATION_PROCUREMENT_APPROVE'
);

-- Phân quyền cho Dược sĩ (PHARMACIST): Xem và Tạo/Gửi dự trù
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'PHARMACIST'
  AND p.code IN ('MEDICATION_PROCUREMENT_READ', 'MEDICATION_PROCUREMENT_CREATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Phân quyền cho Quản lý phòng khám (MANAGER): Xem và Duyệt/Từ chối dự trù
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN ('MEDICATION_PROCUREMENT_READ', 'MEDICATION_PROCUREMENT_APPROVE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Phân quyền cho Quản trị viên (ADMIN): Toàn quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('MEDICATION_PROCUREMENT_READ', 'MEDICATION_PROCUREMENT_CREATE', 'MEDICATION_PROCUREMENT_APPROVE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
