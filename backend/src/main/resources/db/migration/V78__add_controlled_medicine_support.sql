-- =====================================================
-- V78__add_controlled_medicine_support.sql
-- NCL-06-CN-014: Quản lý thuốc kiểm soát đặc biệt
--
-- 1) `medicines.controlled` flag (QTN-39: đánh dấu thuốc kiểm soát đặc biệt)
-- 2) `controlled_medicine_registers` — append-only sổ theo dõi riêng (TC-02/TC-03/TC-04)
-- 3) `CONTROLLED_MEDICINE_REGISTER_READ` permission seeded to ADMIN, PHARMACIST, MANAGER
-- =====================================================

-- ===========================
-- Controlled flag on medicines
-- ===========================
ALTER TABLE medicines
    ADD COLUMN controlled BOOLEAN NOT NULL DEFAULT FALSE;

-- ===========================
-- Special controlled medicine register (append-only, immutable)
-- ===========================
CREATE TABLE controlled_medicine_registers (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    prescription_item_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    medicine_name VARCHAR(150) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    prescribed_by BINARY(16) NOT NULL,
    dispensed_by BINARY(16) NOT NULL,
    quantity INT NOT NULL,
    dispensed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_controlled_medicine_registers PRIMARY KEY (id),
    CONSTRAINT fk_cmr_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    CONSTRAINT fk_cmr_prescription_item
        FOREIGN KEY (prescription_item_id) REFERENCES prescription_items(id),
    CONSTRAINT fk_cmr_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT fk_cmr_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_cmr_prescribed_by
        FOREIGN KEY (prescribed_by) REFERENCES users(id),
    CONSTRAINT fk_cmr_dispensed_by
        FOREIGN KEY (dispensed_by) REFERENCES users(id),
    CONSTRAINT chk_cmr_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_cmr_prescription
    ON controlled_medicine_registers(prescription_id);

CREATE INDEX idx_cmr_patient
    ON controlled_medicine_registers(patient_id);

CREATE INDEX idx_cmr_dispensed_at
    ON controlled_medicine_registers(dispensed_at);

-- ===========================
-- Register read permission
-- ===========================
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'CONTROLLED_MEDICINE_REGISTER_READ',
       'CONTROLLED MEDICINE REGISTER READ',
       'PHARMACY',
       'View the special controlled medicine register (NCL-06-CN-014).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'CONTROLLED_MEDICINE_REGISTER_READ'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'CONTROLLED_MEDICINE_REGISTER_READ'
WHERE r.name IN ('ADMIN', 'PHARMACIST', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
