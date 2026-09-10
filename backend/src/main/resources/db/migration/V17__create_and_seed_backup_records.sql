-- =====================================================
-- V17__create_and_seed_backup_records.sql
-- Backup & Restore schema and demonstration seeds (Demo US-43)
-- =====================================================

CREATE TABLE backup_records (
    id BINARY(16) NOT NULL,
    backup_code VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NULL,
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
    CONSTRAINT fk_backup_records_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_backup_records_restored_by FOREIGN KEY (restored_by) REFERENCES users(id),
    CONSTRAINT chk_backup_records_status CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_backup_records_type CHECK (backup_type IN ('FULL', 'MANUAL')),
    CONSTRAINT chk_backup_records_file_size CHECK (file_size >= 0)
);

CREATE INDEX idx_backup_records_created_at ON backup_records(created_at);
CREATE INDEX idx_backup_records_status ON backup_records(status);

-- Seed Sample Backup Records (Demo US-43: Sao lưu và phục hồi dữ liệu)
INSERT INTO backup_records (
    id, backup_code, file_name, file_size, status, backup_type, description, created_by, created_at, restored_at, restored_by
) VALUES
(UUID_TO_BIN('b1000000-0000-0000-0000-000000000001'), 'BKP-20260901-FULL', 'backup_full_20260901_020000.sql.gz', 45892048, 'SUCCESS', 'FULL', 'Sao lưu toàn bộ định kỳ đầu tháng tự động.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), '2026-09-01 02:00:00', NULL, NULL),
(UUID_TO_BIN('b1000000-0000-0000-0000-000000000002'), 'BKP-20260908-FULL', 'backup_full_20260908_020000.sql.gz', 46381920, 'SUCCESS', 'FULL', 'Sao lưu toàn bộ định kỳ tuần tự động.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), '2026-09-08 02:00:00', NULL, NULL),
(UUID_TO_BIN('b1000000-0000-0000-0000-000000000003'), 'BKP-20260910-MANUAL', 'backup_manual_before_update.sql.gz', 46520114, 'SUCCESS', 'MANUAL', 'Sao lưu thủ công trước khi bảo trì hệ thống.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'), CURRENT_TIMESTAMP, NULL, NULL);
