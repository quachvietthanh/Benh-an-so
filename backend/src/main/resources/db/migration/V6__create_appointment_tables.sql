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
