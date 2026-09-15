-- =====================================================
-- V57__add_patient_merge_support.sql
-- NCL-02-CN-006 / QTN-33: Support duplicate patient record merging
-- Adds status, merged_into_patient_id, merged_at, merged_by, merge_reason.
-- Inserts PATIENT_MERGE permission and assigns to ADMIN, RECEPTIONIST, MANAGER.
-- =====================================================

ALTER TABLE patients
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN merged_into_patient_id BINARY(16) NULL,
    ADD COLUMN merged_at TIMESTAMP NULL,
    ADD COLUMN merged_by BINARY(16) NULL,
    ADD COLUMN merge_reason VARCHAR(500) NULL;

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_merged_into
    FOREIGN KEY (merged_into_patient_id) REFERENCES patients(id);

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_merged_by
    FOREIGN KEY (merged_by) REFERENCES users(id);

CREATE INDEX idx_patients_status ON patients(status);
CREATE INDEX idx_patients_merged_into ON patients(merged_into_patient_id);

-- Add PATIENT_MERGE permission
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PATIENT_MERGE',
       'PATIENT MERGE',
       'PATIENT',
       'Merge duplicate patient profiles and transfer clinical and billing records',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PATIENT_MERGE'
);

-- Grant to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_MERGE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);

-- Grant to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_MERGE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
    AND rp.permission_id = p.id
);

-- Grant to MANAGER (66666666-6666-6666-6666-666666666666)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), p.id
FROM permissions p
WHERE p.code = 'PATIENT_MERGE'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('66666666-6666-6666-6666-666666666666')
    AND rp.permission_id = p.id
);
