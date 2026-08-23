ALTER TABLE medical_records
    ADD COLUMN signature_data LONGTEXT NULL,
    ADD COLUMN signed_at TIMESTAMP NULL,
    ADD COLUMN signed_by BINARY(16) NULL;

ALTER TABLE medical_records
    ADD CONSTRAINT fk_medical_records_signed_by
    FOREIGN KEY (signed_by) REFERENCES users (id);
