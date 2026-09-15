-- =====================================================
-- V56__add_guardian_fields_to_patients.sql
-- NCL-02-CN-008 / QTN-44: Pediatric patient guardian link
-- Adds guardian details, guardian user link, and consent signer name.
-- All columns are nullable to preserve historical data.
-- =====================================================

ALTER TABLE patients
    ADD COLUMN guardian_name VARCHAR(100) NULL,
    ADD COLUMN guardian_relationship VARCHAR(50) NULL,
    ADD COLUMN guardian_phone VARCHAR(20) NULL,
    ADD COLUMN guardian_identity_number VARCHAR(20) NULL,
    ADD COLUMN guardian_user_id BINARY(16) NULL,
    ADD COLUMN consent_signer_name VARCHAR(100) NULL;

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_guardian_user
    FOREIGN KEY (guardian_user_id) REFERENCES users(id);

CREATE INDEX idx_patients_guardian_phone ON patients(guardian_phone);
CREATE INDEX idx_patients_guardian_user_id ON patients(guardian_user_id);
