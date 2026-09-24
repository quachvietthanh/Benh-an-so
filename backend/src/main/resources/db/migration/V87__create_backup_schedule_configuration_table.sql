-- =====================================================
-- V87__create_backup_schedule_configuration_table.sql
-- NCL-09-CN-009: Sao lưu tự động theo lịch và kiểm tra bản sao lưu
-- =====================================================

-- 1. Create table for backup schedule configuration
CREATE TABLE backup_schedule_configurations (
    id BINARY(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    daily_time VARCHAR(10) NOT NULL DEFAULT '02:00',
    cron_expression VARCHAR(50) NOT NULL DEFAULT '0 0 2 * * *',
    last_run_at TIMESTAMP NULL,
    last_status VARCHAR(30) NULL,
    last_failure_reason TEXT NULL,
    alert_active BOOLEAN NOT NULL DEFAULT FALSE,
    last_verified_at TIMESTAMP NULL,
    last_verification_status VARCHAR(30) NULL,
    updated_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_backup_schedule_configurations PRIMARY KEY (id),
    CONSTRAINT fk_backup_schedule_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
);

-- Seed default initial configuration (disabled by default, daily at 02:00)
INSERT INTO backup_schedule_configurations (
    id, enabled, daily_time, cron_expression, last_run_at, last_status,
    last_failure_reason, alert_active, last_verified_at, last_verification_status,
    updated_by, updated_at
) VALUES (
    UUID_TO_BIN('99999999-9999-9999-9999-999999999999'),
    FALSE,
    '02:00',
    '0 0 2 * * *',
    NULL,
    NULL,
    NULL,
    FALSE,
    NULL,
    NULL,
    UUID_TO_BIN('00000000-0000-0000-0000-000000000000'),
    CURRENT_TIMESTAMP
);

-- 2. Add failure_reason column to backup_records
ALTER TABLE backup_records
    ADD COLUMN failure_reason TEXT NULL;

-- 3. Extend backup_type CHECK constraint to accept 'SCHEDULED'
ALTER TABLE backup_records
    DROP CHECK chk_backup_records_type;

ALTER TABLE backup_records
    ADD CONSTRAINT chk_backup_records_type
        CHECK (backup_type IN ('FULL', 'MANUAL', 'SCHEDULED'));
