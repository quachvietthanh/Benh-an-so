-- =====================================================
-- V62__add_revisit_date_to_medical_records.sql
-- Add revisit_date to medical_records (NCL-04-CN-010)
-- =====================================================

ALTER TABLE medical_records
    ADD COLUMN revisit_date DATE NULL;

CREATE INDEX idx_medical_records_revisit_date
    ON medical_records(revisit_date);
