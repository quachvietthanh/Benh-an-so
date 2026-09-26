-- =====================================================
-- V18__create_prescription_tables.sql
-- Medicine catalog, prescriptions and drug interaction rules
-- Includes the warning-log rule_id and severity alignment
-- previously introduced by a retired warning-log alignment migration
-- MySQL 8.x
-- =====================================================

-- ===========================
-- Medicines
-- ===========================

CREATE TABLE medicines (
    id BINARY(16) NOT NULL,
    medicine_code VARCHAR(30) NOT NULL,
    medicine_name VARCHAR(150) NOT NULL,
    active_ingredient VARCHAR(255) NOT NULL,
    strength VARCHAR(100) NOT NULL,
    dosage_form VARCHAR(30) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    default_route VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    stock_quantity INT NOT NULL DEFAULT 0,
    min_stock_threshold INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_medicines PRIMARY KEY (id),
    CONSTRAINT uk_medicines_code UNIQUE (medicine_code),
    CONSTRAINT chk_medicines_dosage_form CHECK (
        dosage_form IN (
            'TABLET',
            'CAPSULE',
            'SYRUP',
            'SUSPENSION',
            'SOLUTION',
            'INJECTION',
            'INFUSION',
            'CREAM',
            'OINTMENT',
            'GEL',
            'DROPS',
            'INHALER',
            'POWDER',
            'SUPPOSITORY',
            'OTHER'
        )
    ),
    CONSTRAINT chk_medicines_default_route CHECK (
        default_route IN (
            'ORAL',
            'SUBLINGUAL',
            'BUCCAL',
            'INTRAVENOUS',
            'INTRAMUSCULAR',
            'SUBCUTANEOUS',
            'TOPICAL',
            'OPHTHALMIC',
            'OTIC',
            'NASAL',
            'INHALATION',
            'RECTAL',
            'VAGINAL',
            'TRANSDERMAL',
            'OTHER'
        )
    ),
    CONSTRAINT chk_medicines_stock_quantity CHECK (stock_quantity >= 0),
    CONSTRAINT chk_medicines_min_stock_threshold CHECK (min_stock_threshold >= 0)
);

CREATE INDEX idx_medicines_name
    ON medicines(medicine_name);

CREATE INDEX idx_medicines_active_ingredient
    ON medicines(active_ingredient);

CREATE INDEX idx_medicines_active
    ON medicines(active);

-- ===========================
-- Prescription code sequence
-- ===========================

CREATE TABLE prescription_code_sequences (
    code_prefix VARCHAR(10) NOT NULL,
    `last_value` BIGINT NOT NULL,

    CONSTRAINT pk_prescription_code_sequences PRIMARY KEY (code_prefix),
    CONSTRAINT chk_prescription_code_sequences_last_value CHECK (`last_value` > 0)
);

-- ===========================
-- Prescriptions
-- ===========================

CREATE TABLE prescriptions (
    id BINARY(16) NOT NULL,
    prescription_code VARCHAR(30) NOT NULL,
    medical_record_id BINARY(16) NOT NULL,
    status VARCHAR(30) NOT NULL,
    note TEXT NULL,
    prescribed_by BINARY(16) NOT NULL,
    prescribed_at TIMESTAMP NOT NULL,
    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_prescriptions PRIMARY KEY (id),
    CONSTRAINT uk_prescriptions_code UNIQUE (prescription_code),
    CONSTRAINT fk_prescriptions_medical_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),
    CONSTRAINT fk_prescriptions_prescribed_by
        FOREIGN KEY (prescribed_by)
        REFERENCES users(id),
    CONSTRAINT fk_prescriptions_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id),
    CONSTRAINT chk_prescriptions_status CHECK (
        status IN ('PENDING_DISPENSE', 'DISPENSED', 'CANCELLED')
    ),
    CONSTRAINT chk_prescriptions_update_metadata CHECK (
        (updated_by IS NULL AND updated_at IS NULL)
        OR (updated_by IS NOT NULL AND updated_at IS NOT NULL)
    )
);

CREATE INDEX idx_prescriptions_medical_record
    ON prescriptions(medical_record_id);

CREATE INDEX idx_prescriptions_status
    ON prescriptions(status);

CREATE INDEX idx_prescriptions_prescribed_by
    ON prescriptions(prescribed_by);

CREATE INDEX idx_prescriptions_prescribed_at
    ON prescriptions(prescribed_at);

-- ===========================
-- Prescription items
-- ===========================

CREATE TABLE prescription_items (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    medicine_name VARCHAR(150) NOT NULL,
    active_ingredient VARCHAR(255) NOT NULL,
    strength VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency INT NOT NULL,
    route VARCHAR(30) NOT NULL,
    duration_days INT NOT NULL,
    quantity INT NOT NULL,
    instructions TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_prescription_items PRIMARY KEY (id),
    CONSTRAINT uk_prescription_items_prescription_medicine
        UNIQUE (prescription_id, medicine_id),
    CONSTRAINT fk_prescription_items_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES prescriptions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prescription_items_medicine
        FOREIGN KEY (medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT chk_prescription_items_route CHECK (
        route IN (
            'ORAL',
            'SUBLINGUAL',
            'BUCCAL',
            'INTRAVENOUS',
            'INTRAMUSCULAR',
            'SUBCUTANEOUS',
            'TOPICAL',
            'OPHTHALMIC',
            'OTIC',
            'NASAL',
            'INHALATION',
            'RECTAL',
            'VAGINAL',
            'TRANSDERMAL',
            'OTHER'
        )
    ),
    CONSTRAINT chk_prescription_items_duration_days CHECK (
        duration_days > 0
    ),
    CONSTRAINT chk_prescription_items_frequency CHECK (
        frequency > 0
    ),
    CONSTRAINT chk_prescription_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_prescription_items_medicine
    ON prescription_items(medicine_id);

-- ===========================
-- Drug interaction rules
-- ===========================

CREATE TABLE drug_interaction_rules (
    id BINARY(16) NOT NULL,
    active_ingredient_a VARCHAR(255) NOT NULL,
    active_ingredient_b VARCHAR(255) NOT NULL,
    severity_level VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    clinical_recommendation TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_drug_interaction_rules PRIMARY KEY (id),
    CONSTRAINT uk_drug_interaction_rules_ingredient_pair
        UNIQUE (active_ingredient_a, active_ingredient_b),
    CONSTRAINT chk_drug_interaction_rules_different_ingredients CHECK (
        active_ingredient_a <> active_ingredient_b
    ),
    CONSTRAINT chk_drug_interaction_rules_severity CHECK (
        severity_level IN ('MILD', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')
    )
);

CREATE INDEX idx_drug_interaction_rules_ingredient_b
    ON drug_interaction_rules(active_ingredient_b);

CREATE INDEX idx_drug_interaction_rules_active_severity
    ON drug_interaction_rules(is_active, severity_level);

-- ===========================
-- Prescription warning logs
-- Merged from the retired warning-log update:
-- 1) rule_id references drug_interaction_rules
-- 2) severity uses MILD, MODERATE, SEVERE, CONTRAINDICATED
-- ===========================

CREATE TABLE prescription_warning_logs (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    rule_id BINARY(16) NOT NULL,
    first_medicine_id BINARY(16) NOT NULL,
    second_medicine_id BINARY(16) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    warning_message TEXT NOT NULL,
    action VARCHAR(30) NOT NULL,
    override_reason TEXT NULL,
    handled_by BINARY(16) NOT NULL,
    handled_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_warning_logs PRIMARY KEY (id),
    CONSTRAINT fk_prescription_warning_logs_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES prescriptions(id),
    CONSTRAINT fk_prescription_warning_logs_rule
        FOREIGN KEY (rule_id)
        REFERENCES drug_interaction_rules(id),
    CONSTRAINT fk_prescription_warning_logs_first_medicine
        FOREIGN KEY (first_medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT fk_prescription_warning_logs_second_medicine
        FOREIGN KEY (second_medicine_id)
        REFERENCES medicines(id),
    CONSTRAINT fk_prescription_warning_logs_handled_by
        FOREIGN KEY (handled_by)
        REFERENCES users(id),
    CONSTRAINT chk_prescription_warning_logs_different_medicines CHECK (
        first_medicine_id <> second_medicine_id
    ),
    CONSTRAINT chk_prescription_warning_logs_severity CHECK (
        severity IN ('MILD', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')
    ),
    CONSTRAINT chk_prescription_warning_logs_action CHECK (
        action IN ('REMOVED_MEDICINE', 'REPLACED_MEDICINE', 'OVERRIDDEN')
    ),
    CONSTRAINT chk_prescription_warning_logs_override_reason CHECK (
        action <> 'OVERRIDDEN'
        OR (override_reason IS NOT NULL AND CHAR_LENGTH(TRIM(override_reason)) > 0)
    )
);

CREATE INDEX idx_prescription_warning_logs_prescription
    ON prescription_warning_logs(prescription_id);

CREATE INDEX idx_prescription_warning_logs_rule
    ON prescription_warning_logs(rule_id);

CREATE INDEX idx_prescription_warning_logs_handled_by
    ON prescription_warning_logs(handled_by);

CREATE INDEX idx_prescription_warning_logs_created_at
    ON prescription_warning_logs(created_at);

-- ===========================
-- Prescription amendments
-- ===========================

CREATE TABLE prescription_amendments (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    change_reason TEXT NOT NULL,
    before_data JSON NOT NULL,
    after_data JSON NOT NULL,
    amended_by BINARY(16) NOT NULL,
    amended_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_amendments PRIMARY KEY (id),
    CONSTRAINT fk_prescription_amendments_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES prescriptions(id),
    CONSTRAINT fk_prescription_amendments_amended_by
        FOREIGN KEY (amended_by)
        REFERENCES users(id),
    CONSTRAINT chk_prescription_amendments_change_reason CHECK (
        CHAR_LENGTH(TRIM(change_reason)) > 0
    )
);

CREATE INDEX idx_prescription_amendments_prescription
    ON prescription_amendments(prescription_id);

CREATE INDEX idx_prescription_amendments_amended_by
    ON prescription_amendments(amended_by);

CREATE INDEX idx_prescription_amendments_amended_at
    ON prescription_amendments(amended_at);


CREATE INDEX idx_prescriptions_status_prescribed_at
    ON prescriptions(status, prescribed_at);
