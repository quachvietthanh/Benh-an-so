-- =====================================================
-- V91__create_prescription_templates.sql
-- NCL-05-CN-008: Bộ đơn thuốc mẫu theo chẩn đoán
-- Prescription templates keyed by diagnosis code, doctor-scoped.
-- Applying a template produces a draft only; it never persists a prescription.
-- =====================================================

CREATE TABLE prescription_templates (
    id BINARY(16) NOT NULL,
    diagnosis_catalog_id BINARY(16) NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_templates PRIMARY KEY (id),
    CONSTRAINT fk_prescription_templates_diagnosis
        FOREIGN KEY (diagnosis_catalog_id) REFERENCES diagnosis_catalog(id),
    CONSTRAINT fk_prescription_templates_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_prescription_templates_diagnosis
    ON prescription_templates(diagnosis_catalog_id);

CREATE INDEX idx_prescription_templates_created_by
    ON prescription_templates(created_by);

CREATE TABLE prescription_template_items (
    id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency INT NOT NULL,
    route VARCHAR(30) NOT NULL,
    duration_days INT NOT NULL,
    quantity INT NOT NULL,
    instructions TEXT NULL,
    sort_order INT NOT NULL,

    CONSTRAINT pk_prescription_template_items PRIMARY KEY (id),
    CONSTRAINT fk_prescription_template_items_template
        FOREIGN KEY (template_id) REFERENCES prescription_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_prescription_template_items_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id),
    CONSTRAINT uk_prescription_template_items_medicine
        UNIQUE (template_id, medicine_id),
    CONSTRAINT chk_prescription_template_items_frequency
        CHECK (frequency > 0),
    CONSTRAINT chk_prescription_template_items_duration
        CHECK (duration_days > 0),
    CONSTRAINT chk_prescription_template_items_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_prescription_template_items_sort_order
        CHECK (sort_order >= 0),
    CONSTRAINT chk_prescription_template_items_route CHECK (
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
    )
);

CREATE INDEX idx_prescription_template_items_template
    ON prescription_template_items(template_id, sort_order);
