-- =====================================================
-- V47__add_diagnosis_catalog_abbreviation.sql
-- Optional Vietnamese/clinical abbreviation for a disease
-- code, searchable independently of the ICD code (NCL-13-CN-005).
-- Kept nullable: existing seeds have no separate abbreviation,
-- so no fabricated medical shorthand is backfilled.
-- =====================================================

ALTER TABLE diagnosis_catalog
    ADD COLUMN abbreviation VARCHAR(50) NULL;
