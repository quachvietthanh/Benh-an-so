-- =====================================================
-- V76__create_contraindication_schema.sql
-- NCL-05-CN-006: Cảnh báo chống chỉ định theo tuổi, thai kỳ và bệnh nền.
-- Configurable contraindication rules (age / pregnancy / disease) evaluated
-- against patient information. Pregnancy status is stored at patient level.
-- =====================================================

ALTER TABLE patients
    ADD COLUMN pregnancy_status VARCHAR(30) NULL;

ALTER TABLE patients
    ADD CONSTRAINT chk_patients_pregnancy_status
        CHECK (pregnancy_status IS NULL OR pregnancy_status IN ('PREGNANT', 'NOT_PREGNANT'));

CREATE TABLE contraindication_rules (
    id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NULL,
    active_ingredient VARCHAR(150) NULL,
    contraindication_type VARCHAR(30) NOT NULL,
    min_age_years INT NULL,
    max_age_years INT NULL,
    diagnosis_catalog_id BINARY(16) NULL,
    severity VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    recommendation VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_contraindication_rules PRIMARY KEY (id),
    CONSTRAINT fk_contraindication_rules_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT fk_contraindication_rules_diagnosis
        FOREIGN KEY (diagnosis_catalog_id) REFERENCES diagnosis_catalog(id),
    CONSTRAINT chk_contraindication_rules_target
        CHECK (medicine_id IS NOT NULL OR active_ingredient IS NOT NULL),
    CONSTRAINT chk_contraindication_rules_type
        CHECK (contraindication_type IN ('AGE', 'PREGNANCY', 'DISEASE')),
    CONSTRAINT chk_contraindication_rules_severity
        CHECK (severity IN ('LOW', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')),
    CONSTRAINT chk_contraindication_rules_message
        CHECK (CHAR_LENGTH(TRIM(message)) > 0)
);

CREATE INDEX idx_contraindication_rules_medicine
    ON contraindication_rules(medicine_id, active);

CREATE INDEX idx_contraindication_rules_ingredient
    ON contraindication_rules(active_ingredient, active);

CREATE TABLE prescription_contraindication_warning_logs (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    rule_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    contraindication_type VARCHAR(30) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    recommendation VARCHAR(500) NULL,
    override_reason VARCHAR(500) NULL,
    handled_by BINARY(16) NOT NULL,
    handled_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_contraindication_warning_logs PRIMARY KEY (id),
    CONSTRAINT fk_presc_contra_warning_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    CONSTRAINT fk_presc_contra_warning_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_presc_contra_warning_rule
        FOREIGN KEY (rule_id) REFERENCES contraindication_rules(id),
    CONSTRAINT fk_presc_contra_warning_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT fk_presc_contra_warning_handled_by
        FOREIGN KEY (handled_by) REFERENCES users(id),
    CONSTRAINT chk_presc_contra_warning_type
        CHECK (contraindication_type IN ('AGE', 'PREGNANCY', 'DISEASE')),
    CONSTRAINT chk_presc_contra_warning_severity
        CHECK (severity IN ('LOW', 'MODERATE', 'SEVERE', 'CONTRAINDICATED'))
);

CREATE INDEX idx_presc_contra_warning_prescription
    ON prescription_contraindication_warning_logs(prescription_id);

-- Seed illustrative, configurable contraindication rules.
INSERT INTO contraindication_rules (
    id, medicine_id, active_ingredient, contraindication_type, min_age_years, max_age_years,
    diagnosis_catalog_id, severity, message, recommendation, active, created_at, updated_at
) SELECT UUID_TO_BIN(UUID()), NULL, 'Aspirin', 'AGE', NULL, 15, NULL, 'CONTRAINDICATED',
     'Aspirin is contraindicated in children under 16 due to the risk of Reye syndrome.',
     'Use Paracetamol as a safer alternative for fever and pain.',
     TRUE, CURRENT_TIMESTAMP, NULL
  WHERE NOT EXISTS (SELECT 1 FROM contraindication_rules WHERE active_ingredient = 'Aspirin' AND contraindication_type = 'AGE');

INSERT INTO contraindication_rules (
    id, medicine_id, active_ingredient, contraindication_type, min_age_years, max_age_years,
    diagnosis_catalog_id, severity, message, recommendation, active, created_at, updated_at
) SELECT UUID_TO_BIN(UUID()), NULL, 'Ibuprofen', 'PREGNANCY', NULL, NULL, NULL, 'CONTRAINDICATED',
     'Ibuprofen is contraindicated in the third trimester of pregnancy.',
     'Use Paracetamol and consult the prescribing physician.',
     TRUE, CURRENT_TIMESTAMP, NULL
  WHERE NOT EXISTS (SELECT 1 FROM contraindication_rules WHERE active_ingredient = 'Ibuprofen' AND contraindication_type = 'PREGNANCY');

INSERT INTO contraindication_rules (
    id, medicine_id, active_ingredient, contraindication_type, min_age_years, max_age_years,
    diagnosis_catalog_id, severity, message, recommendation, active, created_at, updated_at
) SELECT UUID_TO_BIN(UUID()), NULL, 'Ibuprofen', 'DISEASE', NULL, NULL,
     UUID_TO_BIN('a1000000-0000-0000-0000-000000000014'), 'MODERATE',
     'NSAIDs may raise blood pressure and reduce the effect of antihypertensive therapy.',
     'Consider Paracetamol or a non-NSAID alternative and monitor blood pressure.',
     TRUE, CURRENT_TIMESTAMP, NULL
  WHERE NOT EXISTS (SELECT 1 FROM contraindication_rules WHERE active_ingredient = 'Ibuprofen' AND contraindication_type = 'DISEASE');
