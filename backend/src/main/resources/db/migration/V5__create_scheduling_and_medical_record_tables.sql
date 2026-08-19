CREATE TABLE appointments (
    id BINARY(16) NOT NULL,
    appointment_code VARCHAR(30) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    cancel_reason VARCHAR(500),
    checked_in_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT uk_appointments_code UNIQUE (appointment_code),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_appointments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_appointments_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_created_by ON appointments(created_by);
CREATE INDEX idx_appointments_end_time ON appointments(end_time);
CREATE INDEX idx_appointments_status_start_time ON appointments(status, start_time);
CREATE INDEX idx_appointments_doctor_status_start_time ON appointments(doctor_id, status, start_time);


CREATE TABLE rooms (
    id BINARY(16) NOT NULL,
    room_code VARCHAR(30) NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT uk_rooms_code UNIQUE (room_code),
    CONSTRAINT ck_rooms_code_not_blank CHECK (CHAR_LENGTH(TRIM(room_code)) BETWEEN 1 AND 30),
    CONSTRAINT ck_rooms_name_not_blank CHECK (CHAR_LENGTH(TRIM(room_name)) BETWEEN 1 AND 100)
);

CREATE TABLE doctor_room_assignments (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    assigned_by BINARY(16) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_doctor_room_assignments PRIMARY KEY (id),
    CONSTRAINT uk_doctor_room_assignments_doctor UNIQUE (doctor_id),
    CONSTRAINT uk_doctor_room_assignments_room UNIQUE (room_id),
    CONSTRAINT fk_doctor_room_assignments_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_doctor_room_assignments_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_doctor_room_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id)
);

CREATE TABLE medical_queues (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    queue_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_medical_queues PRIMARY KEY (id),
    CONSTRAINT uk_medical_queues_doctor_date UNIQUE (doctor_id, queue_date),
    CONSTRAINT fk_medical_queues_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_medical_queues_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT ck_medical_queues_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE queue_items (
    id BINARY(16) NOT NULL,
    medical_queue_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    appointment_id BINARY(16) NULL,
    visit_id BINARY(16) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    queue_number INT NOT NULL,
    queue_date DATE NOT NULL,
    checked_in_at TIMESTAMP NOT NULL,
    called_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(500) NULL,
    skipped_at TIMESTAMP NULL,
    skip_reason VARCHAR(500) NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_queue_items PRIMARY KEY (id),
    CONSTRAINT uk_queue_items_appointment UNIQUE (appointment_id),
    CONSTRAINT uk_queue_items_visit UNIQUE (visit_id),
    CONSTRAINT uk_queue_items_queue_number UNIQUE (medical_queue_id, queue_number),
    CONSTRAINT fk_queue_items_queue FOREIGN KEY (medical_queue_id) REFERENCES medical_queues(id),
    CONSTRAINT fk_queue_items_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_queue_items_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_queue_items_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_queue_items_source_type CHECK (source_type IN ('APPOINTMENT', 'WALK_IN')),
    CONSTRAINT ck_queue_items_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'WAITING_FOR_RESULT', 'COMPLETED', 'CANCELLED', 'SKIPPED'))
);

CREATE INDEX idx_rooms_active ON rooms(active);
CREATE INDEX idx_medical_queues_room_date ON medical_queues(room_id, queue_date);
CREATE INDEX idx_queue_items_queue_status_number ON queue_items(medical_queue_id, status, queue_number);
CREATE INDEX idx_queue_items_patient_date_status ON queue_items(patient_id, queue_date, status);
CREATE INDEX idx_queue_items_visit ON queue_items(visit_id);



CREATE TABLE appointment_notification_logs (
    id BINARY(16) NOT NULL,
    appointment_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempted_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP NULL,
    failure_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_appointment_notification_logs PRIMARY KEY (id),
    CONSTRAINT fk_appointment_notification_logs_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_appointment_notification_logs_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id)
);

CREATE INDEX idx_appointment_notification_logs_lookup
    ON appointment_notification_logs(appointment_id, notification_type, status);
CREATE INDEX idx_appointment_notification_logs_attempted_at
    ON appointment_notification_logs(attempted_at);


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
    queue_item_id BINARY(16) NULL,

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

    CONSTRAINT fk_visits_queue_item
        FOREIGN KEY (queue_item_id)
        REFERENCES queue_items(id),

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

CREATE UNIQUE INDEX uk_visits_queue_item
    ON visits(queue_item_id);

ALTER TABLE queue_items
    ADD CONSTRAINT fk_queue_items_visit
    FOREIGN KEY (visit_id)
    REFERENCES visits(id);


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


CREATE INDEX idx_access_user
    ON medical_record_access_logs(accessed_by);

CREATE INDEX idx_access_time
    ON medical_record_access_logs(accessed_at);

CREATE INDEX idx_access_user_time
    ON medical_record_access_logs(accessed_by, accessed_at);
