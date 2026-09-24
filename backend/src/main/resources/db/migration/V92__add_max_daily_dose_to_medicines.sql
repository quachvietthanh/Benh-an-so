-- =====================================================
-- V92__add_max_daily_dose_to_medicines.sql
-- NCL-05-CN-007: Kiểm tra liều dùng tối đa theo ngày (mg/day)
--
-- 1) medicine catalog gains:
--    - strength_value_mg : active-ingredient mg per unit (e.g. per tablet)
--    - max_daily_dose_mg : max active-ingredient mg per day (active-ingredient level)
-- 2) prescription_items gains single_dose_quantity : units taken per single dose
--
-- All new numeric columns are nullable; NULL means "not configured" and is
-- treated as missing data (never zero, never a block) per TC-04.
-- =====================================================

ALTER TABLE medicines
    ADD COLUMN strength_value_mg DECIMAL(12,3) NULL;

ALTER TABLE medicines
    ADD CONSTRAINT chk_medicines_strength_value_mg
        CHECK (strength_value_mg IS NULL OR strength_value_mg > 0);

ALTER TABLE medicines
    ADD COLUMN max_daily_dose_mg DECIMAL(12,3) NULL;

ALTER TABLE medicines
    ADD CONSTRAINT chk_medicines_max_daily_dose_mg
        CHECK (max_daily_dose_mg IS NULL OR max_daily_dose_mg > 0);

ALTER TABLE prescription_items
    ADD COLUMN single_dose_quantity DECIMAL(12,3) NULL;

ALTER TABLE prescription_items
    ADD CONSTRAINT chk_prescription_items_single_dose_quantity
        CHECK (single_dose_quantity IS NULL OR single_dose_quantity > 0);

-- ===========================
-- Business warning/override log (NOT a replacement for the central audit)
-- ===========================
CREATE TABLE prescription_max_daily_dose_warning_logs (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    active_ingredient VARCHAR(255) NOT NULL,
    total_daily_dose_mg DECIMAL(12,3) NOT NULL,
    max_daily_dose_mg DECIMAL(12,3) NOT NULL,
    override_reason VARCHAR(500) NULL,
    handled_by BINARY(16) NOT NULL,
    handled_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_max_daily_dose_warning_logs PRIMARY KEY (id),
    CONSTRAINT fk_presc_max_dose_warning_prescription
        FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
    CONSTRAINT fk_presc_max_dose_warning_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_presc_max_dose_warning_handled_by
        FOREIGN KEY (handled_by) REFERENCES users(id)
);

CREATE INDEX idx_presc_max_dose_warning_prescription
    ON prescription_max_daily_dose_warning_logs(prescription_id);
