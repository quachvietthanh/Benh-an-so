-- =====================================================
-- V56__add_visit_close_fields.sql
-- Add close_reason and closed_at columns to visits table
-- for NCL-04-CN-009 (early end / cancel a visit with a reason).
-- =====================================================

ALTER TABLE visits
    ADD COLUMN close_reason VARCHAR(500) NULL,
    ADD COLUMN closed_at TIMESTAMP NULL;
