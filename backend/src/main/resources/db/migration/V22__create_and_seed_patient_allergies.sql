-- =====================================================
-- V22__create_and_seed_patient_allergies.sql
-- Patient medication allergy management & prescription allergy warning logs
-- (NCL-02-CN-005, NCL-05-CN-004 - Demo US-73, US-75)
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

-- Seed Sample Patient Allergies (Demo US-73: Quản lý tiền sử dị ứng thuốc)
INSERT INTO patient_allergies (
    id, patient_id, allergen_type, allergen_name, normalized_allergen_name, severity, reaction, notes, active, created_by, created_at, updated_by, updated_at
) VALUES
-- BN000001 (Nguyen Van An) dị ứng Amoxicillin (Penicillin) - SEVERE
(UUID_TO_BIN('ea100000-0000-0000-0000-000000000001'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), 'MEDICATION', 'Amoxicillin', 'amoxicillin', 'SEVERE', 'Phù mạch, nổi mày đay toàn thân, khó thở thanh quản.', 'Tiền sử sốc phản vệ nhẹ năm 2022 tại BV Bạch Mai.', TRUE, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
-- BN000002 (Tran Thi Binh) dị ứng Paracetamol - MODERATE
(UUID_TO_BIN('ea100000-0000-0000-0000-000000000002'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), 'MEDICATION', 'Paracetamol', 'paracetamol', 'MODERATE', 'Nổi ban sần đỏ rải rác vùng ngực và cổ, ngứa nhiều.', 'Xuất hiện sau khi uống 1 viên Hapacol 500mg.', TRUE, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
-- BN000003 (Le Minh Chau) dị ứng Aspirin - SEVERE
(UUID_TO_BIN('ea100000-0000-0000-0000-000000000003'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'), 'MEDICATION', 'Aspirin', 'aspirin', 'SEVERE', 'Khởi phát cơn hen suyễn phế quản kịch phát, tím tái.', 'Chống chỉ định tuyệt đối nhóm NSAID.', TRUE, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'), CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP);

-- Seed Prescription Allergy Warning Override Log (Demo US-75: Cảnh báo dị ứng khi kê đơn có lý do lâm sàng)
INSERT INTO prescription_allergy_warning_logs (
    id, prescription_id, patient_id, allergy_id, medicine_id, active_ingredient, allergen_name, severity, reaction, override_reason, handled_by, handled_at, created_at
) VALUES
(UUID_TO_BIN('ea200000-0000-0000-0000-000000000001'),
 UUID_TO_BIN('16200000-0000-0000-0000-000000000004'),
 UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
 UUID_TO_BIN('ea100000-0000-0000-0000-000000000001'),
 UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
 'Amoxicillin', 'Amoxicillin', 'SEVERE', 'Phù mạch, nổi mày đay toàn thân, khó thở thanh quản.',
 'Bác sĩ ghi đè tạm thời để kiểm thử phản ứng thuốc trong phòng cấp cứu có chuẩn bị sẵn Adrenaline.',
 UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
