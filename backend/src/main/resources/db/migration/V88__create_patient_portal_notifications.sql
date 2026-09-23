-- =====================================================
-- V88__create_patient_portal_notifications.sql
-- NCL-14-CN-008: Thông báo và nhắc lịch trên cổng bệnh nhân
-- Business Rule: QTN-23 (patient data isolation)
-- Acceptance Criteria: TC-01 (reminder), TC-02 (changed), TC-03 (no cross-patient access)
-- =====================================================

CREATE TABLE patient_portal_notifications (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    appointment_id BINARY(16) NULL,
    reschedule_log_id BINARY(16) NULL,
    clinical_result_id BINARY(16) NULL,

    CONSTRAINT pk_patient_portal_notifications PRIMARY KEY (id),
    CONSTRAINT fk_ppn_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_ppn_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    CONSTRAINT fk_ppn_reschedule_log FOREIGN KEY (reschedule_log_id) REFERENCES appointment_reschedule_logs(id) ON DELETE SET NULL,
    CONSTRAINT fk_ppn_clinical_result FOREIGN KEY (clinical_result_id) REFERENCES clinical_results(id) ON DELETE SET NULL,
    CONSTRAINT chk_ppn_type CHECK (type IN ('APPOINTMENT_REMINDER', 'APPOINTMENT_CHANGED', 'LAB_RESULT_AVAILABLE'))
);

CREATE INDEX idx_ppn_patient_created ON patient_portal_notifications(patient_id, created_at DESC);

-- Idempotency guard rails (defense-in-depth). NULL values are treated as
-- distinct by MySQL and H2, so each unique key only applies to its own type.
CREATE UNIQUE INDEX uk_ppn_reminder ON patient_portal_notifications(patient_id, type, appointment_id);
CREATE UNIQUE INDEX uk_ppn_changed ON patient_portal_notifications(patient_id, type, reschedule_log_id);
CREATE UNIQUE INDEX uk_ppn_lab_result ON patient_portal_notifications(patient_id, type, clinical_result_id);
