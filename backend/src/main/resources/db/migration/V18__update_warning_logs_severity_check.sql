-- =====================================================
-- V18__update_warning_logs_severity_check.sql
-- Align prescription_warning_logs with the V17 active-ingredient
-- interaction rule engine and the InteractionSeverity enum.
-- Safe / Idempotent for MySQL 8.x
-- =====================================================

DROP PROCEDURE IF EXISTS update_prescription_warning_logs_v18;

DELIMITER //

CREATE PROCEDURE update_prescription_warning_logs_v18()
BEGIN
    -- 1) Drop legacy foreign key & column if they exist
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND CONSTRAINT_NAME = 'fk_prescription_warning_logs_interaction'
    ) THEN
        ALTER TABLE prescription_warning_logs DROP FOREIGN KEY fk_prescription_warning_logs_interaction;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND COLUMN_NAME = 'drug_interaction_id'
    ) THEN
        ALTER TABLE prescription_warning_logs DROP COLUMN drug_interaction_id;
    END IF;

    -- 2) Add rule_id column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND COLUMN_NAME = 'rule_id'
    ) THEN
        ALTER TABLE prescription_warning_logs ADD COLUMN rule_id BINARY(16) NOT NULL;
    END IF;

    -- 3) Add foreign key constraint if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND CONSTRAINT_NAME = 'fk_prescription_warning_logs_rule'
    ) THEN
        ALTER TABLE prescription_warning_logs
            ADD CONSTRAINT fk_prescription_warning_logs_rule
                FOREIGN KEY (rule_id) REFERENCES drug_interaction_rules(id);
    END IF;

    -- 4) Create index if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND INDEX_NAME = 'idx_prescription_warning_logs_rule'
    ) THEN
        CREATE INDEX idx_prescription_warning_logs_rule ON prescription_warning_logs(rule_id);
    END IF;

    -- 5) Drop and recreate severity constraint if needed
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND CONSTRAINT_NAME = 'chk_prescription_warning_logs_severity'
    ) THEN
        ALTER TABLE prescription_warning_logs DROP CHECK chk_prescription_warning_logs_severity;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'prescription_warning_logs' 
          AND CONSTRAINT_NAME = 'chk_prescription_warning_logs_severity'
    ) THEN
        ALTER TABLE prescription_warning_logs
            ADD CONSTRAINT chk_prescription_warning_logs_severity CHECK (
                severity IN ('MILD', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')
            );
    END IF;
END //

DELIMITER ;

CALL update_prescription_warning_logs_v18();

DROP PROCEDURE IF EXISTS update_prescription_warning_logs_v18;

