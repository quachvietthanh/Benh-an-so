-- =====================================================
-- V18__update_warning_logs_severity_check.sql
-- Align prescription_warning_logs with the V17 active-ingredient
-- interaction rule engine and the InteractionSeverity enum.
--
-- 1) Replace the legacy drug_interaction_id (FK -> drug_interactions)
--    with rule_id (FK -> drug_interaction_rules).
-- 2) Update the severity CHECK constraint to the enum values:
--    MILD, MODERATE, SEVERE, CONTRAINDICATED.
--
-- Compatible with MySQL 8.x (8.0.19+ for DROP CONSTRAINT) and H2 (tests).
-- =====================================================

-- 1) Drop the legacy foreign key referencing drug_interactions
--    IF EXISTS keeps this compatible both with old databases that still
--    have the legacy column/FK and with fresh databases (V16 no longer
--    creates the drug_interactions table or the legacy warning-log column).
ALTER TABLE prescription_warning_logs
    DROP CONSTRAINT IF EXISTS fk_prescription_warning_logs_interaction;

-- 2) Drop the legacy column; its indexes are dropped automatically
ALTER TABLE prescription_warning_logs
    DROP COLUMN IF EXISTS drug_interaction_id;

-- 3) Add rule_id referencing the new drug_interaction_rules table
ALTER TABLE prescription_warning_logs
    ADD COLUMN rule_id BINARY(16) NOT NULL;

ALTER TABLE prescription_warning_logs
    ADD CONSTRAINT fk_prescription_warning_logs_rule
        FOREIGN KEY (rule_id)
        REFERENCES drug_interaction_rules(id);

CREATE INDEX idx_prescription_warning_logs_rule
    ON prescription_warning_logs(rule_id);

-- 4) Update severity CHECK to match InteractionSeverity
ALTER TABLE prescription_warning_logs
    DROP CONSTRAINT chk_prescription_warning_logs_severity;

ALTER TABLE prescription_warning_logs
    ADD CONSTRAINT chk_prescription_warning_logs_severity CHECK (
        severity IN ('MILD', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')
    );
