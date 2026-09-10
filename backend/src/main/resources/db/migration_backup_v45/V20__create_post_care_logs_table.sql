-- =====================================================
-- V20__create_post_care_logs_table.sql
-- Post-visit care logs (NCL-10-CN-002)
-- =====================================================

CREATE TABLE post_care_logs (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    reminder_id BINARY(16) NULL,
    visit_id BINARY(16) NULL,
    contact_channel VARCHAR(30) NOT NULL,
    contacted_at TIMESTAMP NOT NULL,
    patient_condition VARCHAR(30) NOT NULL,
    care_notes TEXT NOT NULL,
    contact_outcome VARCHAR(30) NOT NULL,
    performed_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_post_care_logs PRIMARY KEY (id),

    CONSTRAINT fk_post_care_logs_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_post_care_logs_reminder
        FOREIGN KEY (reminder_id)
        REFERENCES follow_up_reminders(id),

    CONSTRAINT fk_post_care_logs_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_post_care_logs_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES users(id),

    CONSTRAINT chk_post_care_logs_channel
        CHECK (contact_channel IN ('PHONE', 'SMS', 'IN_PERSON', 'ZALO')),

    CONSTRAINT chk_post_care_logs_condition
        CHECK (patient_condition IN ('STABLE', 'RECOVERING', 'COMPLICATIONS', 'NEEDS_REVISIT')),

    CONSTRAINT chk_post_care_logs_outcome
        CHECK (contact_outcome IN ('REACHED', 'UNREACHABLE', 'DECLINED'))
);

CREATE INDEX idx_post_care_logs_patient
    ON post_care_logs(patient_id, contacted_at);

CREATE INDEX idx_post_care_logs_reminder
    ON post_care_logs(reminder_id);

CREATE INDEX idx_post_care_logs_contacted_at
    ON post_care_logs(contacted_at);

CREATE INDEX idx_post_care_logs_channel
    ON post_care_logs(contact_channel);
