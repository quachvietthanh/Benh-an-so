-- =====================================================
-- V62__add_dispensed_quantity_to_prescription_items.sql
-- NCL-06-CN-008: Cấp phát một phần khi tồn kho không đủ
-- Tracks cumulative dispensed quantity per prescription item so the
-- remaining/shortage quantity can be computed atomically and cumulatively.
-- =====================================================

ALTER TABLE prescription_items
    ADD COLUMN dispensed_quantity INT NOT NULL DEFAULT 0;

ALTER TABLE prescription_items
    ADD CONSTRAINT chk_prescription_items_dispensed_quantity
        CHECK (dispensed_quantity >= 0 AND dispensed_quantity <= quantity);
