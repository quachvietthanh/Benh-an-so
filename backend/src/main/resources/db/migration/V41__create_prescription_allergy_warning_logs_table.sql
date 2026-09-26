-- =====================================================
-- V41__create_prescription_allergy_warning_logs_table.sql
-- NCL-05-CN-004: Cảnh báo dị ứng thuốc khi kê đơn
-- Business Rules: QTN-26, QTN-05, QTN-02, QTN-11
-- Acceptance Criteria: TC-01, TC-02, TC-03, TC-04, TC-05
-- =====================================================

CREATE TABLE prescription_allergy_warning_logs (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    allergy_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    active_ingredient VARCHAR(255) NOT NULL,
    allergen_name VARCHAR(255) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    reaction VARCHAR(255) NULL,
    override_reason TEXT NOT NULL,
    handled_by BINARY(16) NOT NULL,
    handled_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_allergy_warning_logs PRIMARY KEY (id),
    CONSTRAINT fk_pawl_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_pawl_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_pawl_allergy FOREIGN KEY (allergy_id) REFERENCES patient_allergies(id),
    CONSTRAINT fk_pawl_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT fk_pawl_handled_by FOREIGN KEY (handled_by) REFERENCES users(id),
    CONSTRAINT chk_pawl_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE', 'ANAPHYLAXIS')),
    CONSTRAINT chk_pawl_override_reason CHECK (CHAR_LENGTH(TRIM(override_reason)) > 0)
);

CREATE INDEX idx_pawl_prescription ON prescription_allergy_warning_logs(prescription_id);
CREATE INDEX idx_pawl_patient ON prescription_allergy_warning_logs(patient_id, handled_at);
CREATE INDEX idx_pawl_handled_by ON prescription_allergy_warning_logs(handled_by, handled_at);
CREATE INDEX idx_pawl_handled_at ON prescription_allergy_warning_logs(handled_at);

-- Seed permission for TC-05: Quản trị viên tra cứu nhật ký cảnh báo dị ứng
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PRESCRIPTION_ALLERGY_WARNING_VIEW',
       'PRESCRIPTION ALLERGY WARNING VIEW',
       'PRESCRIPTION',
       'View prescription medication allergy override warning logs (QTN-26, TC-05)',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PRESCRIPTION_ALLERGY_WARNING_VIEW'
);

-- Grant PRESCRIPTION_ALLERGY_WARNING_VIEW to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'PRESCRIPTION_ALLERGY_WARNING_VIEW'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);
