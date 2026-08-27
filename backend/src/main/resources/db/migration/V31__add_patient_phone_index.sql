-- =====================================================
-- V31__add_patient_phone_index.sql
-- NCL-14-CN-001 P1: normalize existing patient phone numbers to a canonical
-- "0..." format and add an index to make phone-based candidate lookup efficient
-- and deterministic.
-- =====================================================

-- Trim stray whitespace first.
UPDATE patients SET phone = TRIM(phone) WHERE phone IS NOT NULL;

-- Normalize international "+84" prefix to the domestic "0" prefix.
UPDATE patients SET phone = CONCAT('0', SUBSTRING(phone, 4)) WHERE phone LIKE '+84%';

CREATE INDEX idx_patients_phone ON patients(phone);
