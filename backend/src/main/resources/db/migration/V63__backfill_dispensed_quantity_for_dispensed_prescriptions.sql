-- =====================================================
-- V63__backfill_dispensed_quantity_for_dispensed_prescriptions.sql
-- NCL-06-CN-008: Cấp phát một phần khi tồn kho không đủ
-- Existing prescriptions already in DISPENSED state were created before the
-- dispensed_quantity column existed (V62 defaulted it to 0). Backfill the
-- cumulative dispensed quantity so a fully dispensed item reports
-- dispensed_quantity == quantity and remaining == 0.
-- =====================================================

UPDATE prescription_items
SET dispensed_quantity = quantity
WHERE prescription_id IN (
    SELECT id FROM prescriptions WHERE status = 'DISPENSED'
);
