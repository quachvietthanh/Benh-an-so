-- =====================================================
-- QTN-19 - Minimum medical-record retention period.
-- Defaults to 10 years; never below 10 years.
-- =====================================================

ALTER TABLE clinic_configuration
    ADD COLUMN retention_years INT NOT NULL DEFAULT 10;

ALTER TABLE clinic_configuration
    ADD CONSTRAINT chk_clinic_configuration_retention_years CHECK (retention_years >= 10);
