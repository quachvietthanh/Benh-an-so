-- =====================================================
-- V44__add_cancel_reason_to_prescriptions.sql
-- Add cancel_reason column to prescriptions table
-- for prescription cancellation (NCL-05-CN-005 / QTN-27).
-- =====================================================

ALTER TABLE prescriptions
    ADD COLUMN cancel_reason VARCHAR(500) NULL;

-- Backfill cancel_reason for existing cancelled prescriptions safely
UPDATE prescriptions
SET cancel_reason = SUBSTRING(COALESCE(note, 'Đã hủy theo ghi chú trước đây'), 1, 500)
WHERE status = 'CANCELLED' AND cancel_reason IS NULL;
