-- =====================================================
-- V31__add_booking_channel_and_doctor_schedules.sql
-- NCL-14-CN-003: Online appointment booking (QTN-04)
-- =====================================================

-- Track how an appointment was created. Legacy/in-person rows remain NULL.
ALTER TABLE appointments ADD COLUMN booking_channel VARCHAR(30) NULL;

CREATE INDEX idx_appointments_booking_channel
    ON appointments(booking_channel);

-- Per-doctor working schedule used to compute available slots for a date.
CREATE TABLE doctor_schedules (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_schedules PRIMARY KEY (id),
    CONSTRAINT uk_doctor_schedules_doctor_date UNIQUE (doctor_id, schedule_date),
    CONSTRAINT fk_doctor_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT ck_doctor_schedules_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_doctor_schedules_date ON doctor_schedules(schedule_date);
