-- =====================================================
-- V50__relax_payment_constraints_and_add_indexes.sql
-- 1. Relax payment check constraint to allow partial payments,
--    deposits, installments, and health insurance (BHYT) co-pay.
-- 2. Add composite indexes on visits and appointments to optimize
--    patient history queries without filesort.
-- =====================================================

-- 1. Relax payment constraint
ALTER TABLE payments DROP CONSTRAINT chk_payments_amount_match;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_amount_paid_valid
    CHECK (amount_paid >= 0 AND amount_paid <= total_amount);

-- 2. Composite indexes for high-frequency patient history lookups
CREATE INDEX idx_visits_patient_visit_at_desc
    ON visits(patient_id, visit_at DESC);

CREATE INDEX idx_appointments_patient_start_time_desc
    ON appointments(patient_id, start_time DESC);
