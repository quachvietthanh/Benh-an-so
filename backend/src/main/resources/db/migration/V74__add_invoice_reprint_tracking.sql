-- =====================================================
-- V74__add_invoice_reprint_tracking.sql
-- NCL-07-CN-005: Tra cứu và in lại hóa đơn.
-- Records every reprint of an invoice so the frontend can mark
-- the document as a reprint ("bản in lại") and show the print count.
-- =====================================================

ALTER TABLE invoices
    ADD COLUMN reprint_count INT NOT NULL DEFAULT 0;

ALTER TABLE invoices
    ADD COLUMN last_reprinted_at TIMESTAMP NULL;

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_reprint_count CHECK (reprint_count >= 0);
