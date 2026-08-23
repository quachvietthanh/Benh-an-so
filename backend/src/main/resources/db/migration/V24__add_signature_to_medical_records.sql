ALTER TABLE medical_records
    ADD COLUMN IF NOT EXISTS signature_data TEXT NULL,
    ADD COLUMN IF NOT EXISTS signed_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN IF NOT EXISTS signed_by UUID NULL;

ALTER TABLE medical_records
    ADD CONSTRAINT fk_medical_records_signed_by
    FOREIGN KEY (signed_by) REFERENCES users (id);
