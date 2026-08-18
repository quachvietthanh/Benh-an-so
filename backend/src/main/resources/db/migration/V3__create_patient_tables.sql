CREATE TABLE patients (
    id BINARY(16) NOT NULL,
    patient_code VARCHAR(30) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    identity_number VARCHAR(20),
    insurance_number VARCHAR(30),
    blood_type VARCHAR(20),
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BINARY(16) NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patients PRIMARY KEY (id),
    CONSTRAINT uk_patients_code UNIQUE (patient_code),
    CONSTRAINT uk_patients_identity UNIQUE (identity_number),
    CONSTRAINT uk_patients_user_id UNIQUE (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_patients_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE patient_change_logs (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    changed_by BINARY(16) NOT NULL,
    action VARCHAR(20) NOT NULL,
    change_detail JSON NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patient_change_logs PRIMARY KEY (id),
    CONSTRAINT fk_patient_change_logs_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_change_logs_user FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_patients_created_by ON patients(created_by);
CREATE INDEX idx_patients_full_name ON patients(full_name);
CREATE INDEX idx_patients_active ON patients(active);
CREATE INDEX idx_patient_change_logs_patient ON patient_change_logs(patient_id);
CREATE INDEX idx_patient_change_logs_created_at ON patient_change_logs(created_at);
