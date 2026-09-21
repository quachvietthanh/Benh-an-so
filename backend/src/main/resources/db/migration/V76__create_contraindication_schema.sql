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

-- =====================================================
-- Contraindication rule catalog
-- =====================================================
-- The `contraindication_rules` table is intentionally seeded with NO rows.
-- Clinical rules (age / pregnancy / disease thresholds, severity, message and
-- recommendation) MUST come from an authoritative clinical source and be
-- configured by an authorized business/clinical process. No clinical
-- thresholds or medication/disease pairings are invented by this migration.
--
-- Configuration mechanism: the catalog is managed as controlled schema/config
-- changes (Flyway migrations reviewed by a clinician), NOT through a runtime
-- write API. A runtime management endpoint is deliberately out of scope until
-- the product defines an authoritative rule catalog and a permission model for
-- it; ordinary clinical users must not be able to modify these rules.
-- =====================================================
