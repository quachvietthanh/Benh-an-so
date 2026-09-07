-- =====================================================
-- V37__create_patient_allergy_tables.sql
-- NCL-02-CN-005: Quản lý tiền sử dị ứng thuốc của bệnh nhân
-- Business Rules: QTN-26, QTN-02, QTN-01, QTN-11
-- =====================================================

CREATE TABLE patient_allergies (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    allergen_type VARCHAR(50) NOT NULL DEFAULT 'MEDICATION',
    allergen_name VARCHAR(255) NOT NULL,
    normalized_allergen_name VARCHAR(255) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    reaction VARCHAR(255) NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    active_normalized_name VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN active = TRUE THEN normalized_allergen_name ELSE NULL END),
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patient_allergies PRIMARY KEY (id),
    CONSTRAINT fk_patient_allergies_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_allergies_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_patient_allergies_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_patient_allergies_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE', 'ANAPHYLAXIS')),
    CONSTRAINT uk_patient_active_allergen UNIQUE (patient_id, active_normalized_name)
);

CREATE INDEX idx_patient_allergies_patient ON patient_allergies(patient_id, active);
CREATE INDEX idx_patient_allergies_normalized ON patient_allergies(patient_id, normalized_allergen_name);

-- Bảng nhật ký thay đổi dị ứng (TC-04: Lưu vết người sửa/xóa, thời điểm và nội dung trước khi sửa)
CREATE TABLE patient_allergy_change_logs (
    id BINARY(16) NOT NULL,
    allergy_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    action VARCHAR(20) NOT NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    change_reason TEXT NULL,
    changed_by BINARY(16) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patient_allergy_change_logs PRIMARY KEY (id),
    CONSTRAINT fk_patient_allergy_change_logs_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_allergy_change_logs_changed_by FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_allergy_change_logs_allergy ON patient_allergy_change_logs(allergy_id);
CREATE INDEX idx_allergy_change_logs_patient ON patient_allergy_change_logs(patient_id);
CREATE INDEX idx_allergy_change_logs_changed_at ON patient_allergy_change_logs(changed_at);

-- Seed permissions
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PATIENT_ALLERGY_WRITE',
       'PATIENT ALLERGY WRITE',
       'PATIENT',
       'Record, update, or remove patient medication allergy history (QTN-11)',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PATIENT_ALLERGY_WRITE'
);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PATIENT_ALLERGY_READ',
       'PATIENT ALLERGY READ',
       'PATIENT',
       'View patient medication allergy history',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PATIENT_ALLERGY_READ'
);

-- Grant PATIENT_ALLERGY_WRITE to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_ALLERGY_WRITE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);

-- Grant PATIENT_ALLERGY_WRITE to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_ALLERGY_WRITE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
    AND rp.permission_id = p.id
);

-- Grant PATIENT_ALLERGY_READ to ADMIN, DOCTOR, NURSE, PHARMACIST, MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'PATIENT_ALLERGY_READ'
AND r.id IN (
    UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), -- ADMIN
    UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), -- DOCTOR
    UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), -- NURSE
    UUID_TO_BIN('55555555-5555-5555-5555-555555555555'), -- PHARMACIST
    UUID_TO_BIN('66666666-6666-6666-6666-666666666666')  -- MANAGER
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id
    AND rp.permission_id = p.id
);
