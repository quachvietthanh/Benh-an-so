-- =====================================================
-- V46__add_diagnosis_recent_index.sql
-- Composite index to back the doctor-scoped recent
-- disease-code suggestion lookup (NCL-13-CN-005).
-- =====================================================

CREATE INDEX idx_diagnoses_doctor_time
    ON medical_record_diagnoses (diagnosed_by, diagnosed_at);
