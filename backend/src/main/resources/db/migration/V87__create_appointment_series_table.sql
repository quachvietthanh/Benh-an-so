CREATE TABLE appointment_series (
    id BINARY(16) NOT NULL,
    series_code VARCHAR(30) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    medical_record_id BINARY(16) NULL,
    total_sessions INT NOT NULL,
    interval_days INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    notes TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT pk_appointment_series PRIMARY KEY (id),
    CONSTRAINT uk_appointment_series_code UNIQUE (series_code),
    CONSTRAINT fk_series_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_series_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_series_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_records(id),
    CONSTRAINT fk_series_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_series_total_sessions CHECK (total_sessions >= 2),
    CONSTRAINT ck_series_interval_days CHECK (interval_days >= 1)
);

CREATE INDEX idx_series_patient_id ON appointment_series(patient_id);
CREATE INDEX idx_series_doctor_id ON appointment_series(doctor_id);

ALTER TABLE appointments ADD COLUMN series_id BINARY(16) NULL;
ALTER TABLE appointments ADD COLUMN sequence_number INT NULL;

ALTER TABLE appointments ADD CONSTRAINT fk_appointments_series
    FOREIGN KEY (series_id) REFERENCES appointment_series(id) ON DELETE SET NULL;

CREATE INDEX idx_appointments_series_id ON appointments(series_id);

-- Sequence table for atomic, concurrency-safe code generation (APT and SER)
CREATE TABLE appointment_code_sequences (
    code_prefix VARCHAR(10) NOT NULL,
    `last_value` BIGINT NOT NULL,
    CONSTRAINT pk_appointment_code_sequences PRIMARY KEY (code_prefix),
    CONSTRAINT chk_appointment_code_sequences_last_value CHECK (`last_value` >= 0)
);

INSERT INTO appointment_code_sequences (code_prefix, `last_value`)
VALUES (
    'APT',
    COALESCE((SELECT MAX(CAST(RIGHT(appointment_code, 6) AS UNSIGNED)) FROM appointments WHERE appointment_code REGEXP '[0-9]{6}$'), 0)
);

INSERT INTO appointment_code_sequences (code_prefix, `last_value`)
VALUES ('SER', 0);
