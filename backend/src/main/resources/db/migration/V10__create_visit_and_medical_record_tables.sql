-- =====================================================
-- V10 - Medical Visit and Medical Record
-- =====================================================

-- ===========================
-- Visits
-- ===========================

CREATE TABLE visits (
    id BINARY(16) NOT NULL,
    visit_code VARCHAR(30) NOT NULL,

    patient_id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    appointment_id BINARY(16) NULL,
    queue_id BINARY(16) NULL,

    visit_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,

    visit_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,

    reason TEXT NOT NULL,
    note TEXT NULL,

    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_visits
        PRIMARY KEY (id),

    CONSTRAINT uk_visits_code
        UNIQUE (visit_code),

    CONSTRAINT fk_visits_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_visits_doctor
        FOREIGN KEY (doctor_id)
        REFERENCES users(id),

    CONSTRAINT fk_visits_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id),

    CONSTRAINT fk_visits_queue
        FOREIGN KEY (queue_id)
        REFERENCES medical_queue(id),

    CONSTRAINT fk_visits_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE INDEX idx_visits_patient
    ON visits(patient_id);

CREATE INDEX idx_visits_doctor
    ON visits(doctor_id);

CREATE INDEX idx_visits_status
    ON visits(status);

CREATE INDEX idx_visits_visit_at
    ON visits(visit_at);

CREATE INDEX idx_visits_started_at
    ON visits(started_at);

CREATE INDEX idx_visits_appointment
    ON visits(appointment_id);

CREATE INDEX idx_visits_queue
    ON visits(queue_id);


-- ===========================
-- Medical Records
-- ===========================

CREATE TABLE medical_records (
    id BINARY(16) NOT NULL,
    visit_id BINARY(16) NOT NULL,

    chief_complaint TEXT NULL,
    symptoms TEXT NULL,
    medical_history TEXT NULL,
    physical_examination TEXT NULL,
    clinical_progress TEXT NULL,
    treatment_plan TEXT NULL,
    doctor_instructions TEXT NULL,
    conclusion TEXT NULL,

    status VARCHAR(30) NOT NULL,

    locked_at TIMESTAMP NULL,
    locked_by BINARY(16) NULL,

    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_medical_records
        PRIMARY KEY (id),

    CONSTRAINT uk_medical_records_visit
        UNIQUE (visit_id),

    CONSTRAINT fk_medical_records_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_medical_records_locked_by
        FOREIGN KEY (locked_by)
        REFERENCES users(id),

    CONSTRAINT fk_medical_records_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_medical_records_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

CREATE INDEX idx_medical_records_status
    ON medical_records(status);

CREATE INDEX idx_medical_records_created_at
    ON medical_records(created_at);


-- ===========================
-- Medical Record Amendments
-- ===========================

CREATE TABLE medical_record_amendments (
    id BINARY(16) NOT NULL,
    medical_record_id BINARY(16) NOT NULL,

    content TEXT NOT NULL,
    reason TEXT NOT NULL,

    amended_by BINARY(16) NOT NULL,
    amended_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medical_record_amendments
        PRIMARY KEY (id),

    CONSTRAINT fk_amendments_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),

    CONSTRAINT fk_amendments_user
        FOREIGN KEY (amended_by)
        REFERENCES users(id)
);

CREATE INDEX idx_amendments_record_time
    ON medical_record_amendments(
        medical_record_id,
        amended_at
    );


-- ===========================
-- Medical Record Access Logs
-- ===========================

CREATE TABLE medical_record_access_logs (
    id BINARY(16) NOT NULL,

    patient_id BINARY(16) NOT NULL,
    visit_id BINARY(16) NULL,
    medical_record_id BINARY(16) NULL,

    accessed_by BINARY(16) NOT NULL,
    action VARCHAR(30) NOT NULL,

    detail VARCHAR(500) NULL,
    ip_address VARCHAR(45) NULL,

    accessed_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medical_record_access_logs
        PRIMARY KEY (id),

    CONSTRAINT fk_access_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_access_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_access_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),

    CONSTRAINT fk_access_user
        FOREIGN KEY (accessed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_access_patient_time
    ON medical_record_access_logs(
        patient_id,
        accessed_at
    );

CREATE INDEX idx_access_visit
    ON medical_record_access_logs(visit_id);

CREATE INDEX idx_access_record
    ON medical_record_access_logs(medical_record_id);


-- ===========================
-- Diagnosis Catalog
-- ===========================

CREATE TABLE diagnosis_catalog (
    id BINARY(16) NOT NULL,

    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,

    active BOOLEAN NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_diagnosis_catalog
        PRIMARY KEY (id),

    CONSTRAINT uk_diagnosis_catalog_code
        UNIQUE (code)
);

CREATE INDEX idx_diagnosis_catalog_name
    ON diagnosis_catalog(name);

CREATE INDEX idx_diagnosis_catalog_active
    ON diagnosis_catalog(active);


-- ===========================
-- Medical Record Diagnoses
-- ===========================

CREATE TABLE medical_record_diagnoses (
    id BINARY(16) NOT NULL,

    medical_record_id BINARY(16) NOT NULL,
    diagnosis_catalog_id BINARY(16) NULL,

    diagnosis_code VARCHAR(30) NULL,
    diagnosis_name VARCHAR(150) NOT NULL,
    diagnosis_type VARCHAR(30) NOT NULL,

    note TEXT NULL,

    diagnosed_by BINARY(16) NOT NULL,
    diagnosed_at TIMESTAMP NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_medical_record_diagnoses
        PRIMARY KEY (id),

    CONSTRAINT fk_diagnoses_record
        FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id),

    CONSTRAINT fk_diagnoses_catalog
        FOREIGN KEY (diagnosis_catalog_id)
        REFERENCES diagnosis_catalog(id),

    CONSTRAINT fk_diagnoses_user
        FOREIGN KEY (diagnosed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_diagnoses_record
    ON medical_record_diagnoses(medical_record_id);

CREATE INDEX idx_diagnoses_catalog
    ON medical_record_diagnoses(diagnosis_catalog_id);

CREATE INDEX idx_diagnoses_type
    ON medical_record_diagnoses(diagnosis_type);