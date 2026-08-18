-- =====================================================
-- V30__create_backup_records_table.sql
-- Backup & restore metadata for NCL-09-CN-005.
-- =====================================================

CREATE TABLE backup_records (
    id BINARY(16) NOT NULL,
    backup_code VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    backup_type VARCHAR(30) NOT NULL,
    description TEXT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    restored_at TIMESTAMP NULL,
    restored_by BINARY(16) NULL,

    CONSTRAINT pk_backup_records PRIMARY KEY (id),
    CONSTRAINT uk_backup_records_code UNIQUE (backup_code),

    CONSTRAINT fk_backup_records_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT fk_backup_records_restored_by
        FOREIGN KEY (restored_by)
        REFERENCES users(id),

    CONSTRAINT chk_backup_records_status
        CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'FAILED')),

    CONSTRAINT chk_backup_records_type
        CHECK (backup_type IN ('FULL', 'MANUAL')),

    CONSTRAINT chk_backup_records_file_size
        CHECK (file_size >= 0)
);

CREATE INDEX idx_backup_records_created_at
    ON backup_records(created_at);

CREATE INDEX idx_backup_records_status
    ON backup_records(status);
