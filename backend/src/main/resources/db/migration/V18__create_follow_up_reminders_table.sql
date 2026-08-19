-- =====================================================
-- V18__create_follow_up_reminders_table.sql
-- Follow-up reminders (NCL-10-CN-001)
-- =====================================================

CREATE TABLE follow_up_reminders (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    visit_id BINARY(16) NULL,
    appointment_id BINARY(16) NULL,
    follow_up_date DATE NOT NULL,
    remind_at TIMESTAMP NOT NULL,
    reminder_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_follow_up_reminders PRIMARY KEY (id),

    CONSTRAINT fk_follow_up_reminders_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_follow_up_reminders_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_follow_up_reminders_appointment
        FOREIGN KEY (appointment_id)
        REFERENCES appointments(id),

    CONSTRAINT fk_follow_up_reminders_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_follow_up_reminders_type
        CHECK (reminder_type IN ('REVISIT', 'MEDICATION_CHECK', 'GENERAL')),

    CONSTRAINT chk_follow_up_reminders_status
        CHECK (status IN ('PENDING', 'SENT', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_follow_up_reminders_patient
    ON follow_up_reminders(patient_id);

CREATE INDEX idx_follow_up_reminders_status
    ON follow_up_reminders(status);

CREATE INDEX idx_follow_up_reminders_remind_at
    ON follow_up_reminders(remind_at);
