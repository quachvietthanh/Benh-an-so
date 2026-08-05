-- =====================================================
-- V18__update_warning_logs_severity_check.sql
-- Align prescription_warning_logs with the V17 active-ingredient
-- interaction rule engine and the InteractionSeverity enum.
--
-- 1) Add rule_id (FK -> drug_interaction_rules).
--    NOTE: V16 no longer creates the legacy drug_interaction_id
--    column / FK, so on a fresh schema there is nothing to drop.
--    (MySQL 8.0 does NOT support `DROP CONSTRAINT IF EXISTS`, so
--    no IF EXISTS clauses are used here.)
-- 2) Update the severity CHECK constraint to the enum values:
--    MILD, MODERATE, SEVERE, CONTRAINDICATED.
--
-- Compatible with MySQL 8.x and H2 (tests).
-- =====================================================

-- 1) Add rule_id referencing the drug_interaction_rules table
ALTER TABLE prescription_warning_logs
    ADD COLUMN rule_id BINARY(16) NOT NULL;

ALTER TABLE prescription_warning_logs
    ADD CONSTRAINT fk_prescription_warning_logs_rule
        FOREIGN KEY (rule_id)
        REFERENCES drug_interaction_rules(id);

CREATE INDEX idx_prescription_warning_logs_rule
    ON prescription_warning_logs(rule_id);

-- 2) Update severity CHECK to match InteractionSeverity
ALTER TABLE prescription_warning_logs
    DROP CONSTRAINT chk_prescription_warning_logs_severity;

ALTER TABLE prescription_warning_logs
    ADD CONSTRAINT chk_prescription_warning_logs_severity CHECK (
        severity IN ('MILD', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')
    );
