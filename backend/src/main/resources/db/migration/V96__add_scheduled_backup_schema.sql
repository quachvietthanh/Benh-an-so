-- =====================================================
-- V96__add_scheduled_backup_schema.sql
-- Automatic scheduled backup (NCL-09-CN-009).
-- Adds a singleton schedule table, a failure reason on the existing
-- backup_records history, and widens the backup type check to include
-- the scheduled trigger type.
-- =====================================================

CREATE TABLE backup_schedules (
    id INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    backup_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_backup_schedules PRIMARY KEY (id),
    CONSTRAINT chk_backup_schedules_singleton CHECK (id = 1)
);

-- Disabled by default; the administrator enables and sets the time via the API.
INSERT INTO backup_schedules (id, enabled, backup_time, created_at, updated_at)
VALUES (1, FALSE, '02:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE backup_records ADD COLUMN failure_reason VARCHAR(500) NULL;

ALTER TABLE backup_records DROP CONSTRAINT chk_backup_records_type;
ALTER TABLE backup_records ADD CONSTRAINT chk_backup_records_type
    CHECK (backup_type IN ('FULL', 'MANUAL', 'SCHEDULED'));
