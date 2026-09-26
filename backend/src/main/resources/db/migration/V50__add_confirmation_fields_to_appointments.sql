-- =====================================================
-- V47__add_confirmation_fields_to_appointments.sql
-- NCL-03-CN-008: Xác nhận lịch hẹn
-- Business Rules: QTN-08, QTN-01, QTN-23
-- Acceptance Criteria: TC-01, TC-02, TC-03, TC-04
-- =====================================================

ALTER TABLE appointments ADD COLUMN confirmed_at TIMESTAMP NULL;
ALTER TABLE appointments ADD COLUMN confirmed_by BINARY(16) NULL;

ALTER TABLE appointments ADD CONSTRAINT fk_appointments_confirmed_by
    FOREIGN KEY (confirmed_by) REFERENCES users(id);

CREATE INDEX idx_appointments_confirmed_at
    ON appointments(confirmed_at);
