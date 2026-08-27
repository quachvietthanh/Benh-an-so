-- =====================================================
-- V32__add_patient_consent_fields.sql
-- NCL-15-CN-001 / QTN-24: Personal data processing consent
-- Adds consent recording, timestamping, withdrawal status,
-- and non-medical processing restriction flags.
-- =====================================================

ALTER TABLE patients
    ADD COLUMN consent_agreed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN consent_agreed_at TIMESTAMP NULL,
    ADD COLUMN consent_version VARCHAR(30) NULL DEFAULT 'v1.0',
    ADD COLUMN consent_withdrawn BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN consent_withdrawn_at TIMESTAMP NULL,
    ADD COLUMN consent_withdrawn_reason VARCHAR(500) NULL,
    ADD COLUMN non_medical_use_restricted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE patients SET consent_agreed_at = created_at WHERE consent_agreed_at IS NULL;
