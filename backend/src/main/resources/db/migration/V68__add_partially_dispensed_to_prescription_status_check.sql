-- =====================================================
-- V68__add_partially_dispensed_to_prescription_status_check.sql
-- NCL-06-CN-008: Cấp phát một phần khi tồn kho không đủ.
-- The application supports PrescriptionStatus.PARTIALLY_DISPENSED, but the
-- original chk_prescriptions_status CHECK constraint (V11) only allowed
-- PENDING_DISPENSE, DISPENSED and CANCELLED. Recreate the constraint so the
-- database accepts the partial-dispensing status.
-- =====================================================

ALTER TABLE prescriptions
    DROP CHECK chk_prescriptions_status;

ALTER TABLE prescriptions
    ADD CONSTRAINT chk_prescriptions_status CHECK (
        status IN ('PENDING_DISPENSE', 'PARTIALLY_DISPENSED', 'DISPENSED', 'CANCELLED')
    );