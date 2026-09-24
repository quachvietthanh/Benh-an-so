-- =====================================================
-- V89__create_appointment_waitlist_table.sql
-- NCL-03-CN-012: Danh sách chờ khi hết khung giờ
-- =====================================================

CREATE TABLE appointment_waitlist (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    desired_date DATE NOT NULL,
    time_preference VARCHAR(30) NOT NULL DEFAULT 'ANYTIME',
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    note VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    booked_appointment_id BINARY(16) NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_appointment_waitlist PRIMARY KEY (id),
    CONSTRAINT fk_waitlist_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_waitlist_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_waitlist_booked_appointment FOREIGN KEY (booked_appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    CONSTRAINT fk_waitlist_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_waitlist_status CHECK (status IN ('WAITING', 'SCHEDULED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_waitlist_time_preference CHECK (time_preference IN ('ANYTIME', 'MORNING', 'AFTERNOON'))
);

CREATE INDEX idx_waitlist_doctor_date_status ON appointment_waitlist(doctor_id, desired_date, status, created_at);
CREATE INDEX idx_waitlist_patient_date ON appointment_waitlist(patient_id, desired_date);
CREATE UNIQUE INDEX uq_waitlist_patient_doctor_date_status ON appointment_waitlist(patient_id, doctor_id, desired_date, status);

