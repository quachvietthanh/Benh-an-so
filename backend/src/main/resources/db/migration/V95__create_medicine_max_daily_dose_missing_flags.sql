-- =====================================================
-- V95__create_medicine_max_daily_dose_missing_flags.sql
-- NCL-05-CN-007: persistent flag for missing max-daily-dose
-- catalog configuration (TC-04).
--
-- One active flag per (medicine_id, missing_reason). Repeated
-- prescriptions of the same still-missing medicine update
-- last_detected_at instead of creating duplicate rows.
-- =====================================================

CREATE TABLE medicine_max_daily_dose_missing_flags (
    id BINARY(16) NOT NULL,
    medicine_id BINARY(16) NOT NULL,
    active_ingredient VARCHAR(255) NOT NULL,
    missing_reason VARCHAR(50) NOT NULL,
    first_detected_at TIMESTAMP NOT NULL,
    last_detected_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medicine_mdd_missing_flags PRIMARY KEY (id),
    CONSTRAINT uq_medicine_mdd_missing_flag UNIQUE (medicine_id, missing_reason),
    CONSTRAINT fk_medicine_mdd_missing_flag_medicine
        FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

CREATE INDEX idx_medicine_mdd_missing_flag_medicine
    ON medicine_max_daily_dose_missing_flags(medicine_id);
