-- =====================================================
-- V59__create_vital_signs_table.sql
-- NCL-04-CN-007 / QTN-07, QTN-02: Structured Vital Signs for Visits
-- Stores structured clinical measurements: pulse, blood pressure,
-- temperature, respiratory rate, weight, height, BMI, SpO2, and abnormal flags.
-- =====================================================

CREATE TABLE vital_signs (
    id BINARY(16) NOT NULL,
    visit_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    medical_record_id BINARY(16) NULL,

    pulse INT NULL,
    blood_pressure_systolic INT NULL,
    blood_pressure_diastolic INT NULL,
    temperature DECIMAL(4, 1) NULL,
    respiratory_rate INT NULL,
    weight DECIMAL(5, 2) NULL,
    height DECIMAL(5, 2) NULL,
    bmi DECIMAL(5, 1) NULL,
    spo2 INT NULL,

    is_abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    abnormal_flags VARCHAR(500) NULL,
    note VARCHAR(500) NULL,

    recorded_by BINARY(16) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_vital_signs
        PRIMARY KEY (id),

    CONSTRAINT fk_vital_signs_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_vital_signs_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_vital_signs_medical_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),

    CONSTRAINT fk_vital_signs_recorded_by
        FOREIGN KEY (recorded_by)
        REFERENCES users(id),

    CONSTRAINT fk_vital_signs_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

CREATE INDEX idx_vital_signs_visit
    ON vital_signs(visit_id);

CREATE INDEX idx_vital_signs_patient_recorded_at
    ON vital_signs(patient_id, recorded_at);

CREATE INDEX idx_vital_signs_medical_record
    ON vital_signs(medical_record_id);

CREATE INDEX idx_vital_signs_recorded_at
    ON vital_signs(recorded_at);
