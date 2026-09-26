-- =====================================================
-- V46__create_appointment_reschedule_logs.sql
-- NCL-03-CN-007: Đổi lịch hẹn tại quầy
-- Business Rules: QTN-04, QTN-30, QTN-08
-- Acceptance Criteria: TC-01, TC-02, TC-03, TC-04
-- =====================================================

CREATE TABLE appointment_reschedule_logs (
    id BINARY(16) NOT NULL,
    appointment_id BINARY(16) NOT NULL,
    old_doctor_id BINARY(16) NOT NULL,
    new_doctor_id BINARY(16) NOT NULL,
    old_start_time TIMESTAMP NOT NULL,
    old_end_time TIMESTAMP NOT NULL,
    new_start_time TIMESTAMP NOT NULL,
    new_end_time TIMESTAMP NOT NULL,
    reason VARCHAR(500) NOT NULL,
    rescheduled_by BINARY(16) NOT NULL,
    rescheduled_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_appointment_reschedule_logs PRIMARY KEY (id),
    CONSTRAINT fk_arl_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_arl_old_doctor FOREIGN KEY (old_doctor_id) REFERENCES users(id),
    CONSTRAINT fk_arl_new_doctor FOREIGN KEY (new_doctor_id) REFERENCES users(id),
    CONSTRAINT fk_arl_rescheduled_by FOREIGN KEY (rescheduled_by) REFERENCES users(id),
    CONSTRAINT chk_arl_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_arl_new_time CHECK (new_end_time > new_start_time)
);

CREATE INDEX idx_arl_appointment ON appointment_reschedule_logs(appointment_id, rescheduled_at DESC);
CREATE INDEX idx_arl_rescheduled_by ON appointment_reschedule_logs(rescheduled_by, rescheduled_at);
