-- =====================================================
-- V48__add_diagnosis_catalog_normalized_columns.sql
-- Normalized search columns for Vietnamese accent-insensitive
-- catalog lookup (NCL-13-CN-005). Populated in the application
-- layer (Java) because Vietnamese diacritic stripping is not
-- reliably expressible in MySQL/H2 SQL. Nullable so existing
-- seeded rows are backfilled lazily at startup.
-- =====================================================

ALTER TABLE diagnosis_catalog
    ADD COLUMN name_norm VARCHAR(150) NULL,
    ADD COLUMN abbreviation_norm VARCHAR(50) NULL;

CREATE INDEX idx_diagnosis_catalog_active_name_norm
    ON diagnosis_catalog (active, name_norm);
