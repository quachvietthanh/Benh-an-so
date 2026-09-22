-- =====================================================
-- V77__create_patient_import_schema_and_permission.sql
-- NCL-02-CN-010: Nhập hồ sơ bệnh nhân từ tệp bảng tính
-- Creates patient import logging tables and seeds PATIENT_IMPORT permission.
-- =====================================================

CREATE TABLE patient_import_logs (
    id BINARY(16) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    total_rows INT NOT NULL,
    success_rows INT NOT NULL,
    error_rows INT NOT NULL,
    duplicate_rows INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    imported_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patient_import_logs PRIMARY KEY (id),
    CONSTRAINT fk_patient_import_logs_imported_by FOREIGN KEY (imported_by) REFERENCES users(id)
);

CREATE TABLE patient_import_log_errors (
    id BINARY(16) NOT NULL,
    import_log_id BINARY(16) NOT NULL,
    row_number INT NOT NULL,
    error_field VARCHAR(100) NULL,
    error_message VARCHAR(500) NOT NULL,
    raw_data TEXT NULL,
    CONSTRAINT pk_patient_import_log_errors PRIMARY KEY (id),
    CONSTRAINT fk_patient_import_errors_log FOREIGN KEY (import_log_id) REFERENCES patient_import_logs(id) ON DELETE CASCADE
);

CREATE INDEX idx_patient_import_logs_created_at ON patient_import_logs(created_at);
CREATE INDEX idx_patient_import_errors_log_id ON patient_import_log_errors(import_log_id);

-- Seed PATIENT_IMPORT permission
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'PATIENT_IMPORT',
       'PATIENT IMPORT',
       'PATIENT',
       'Import patient records from spreadsheet (NCL-02-CN-010).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PATIENT_IMPORT'
);

-- Grant to ADMIN and RECEPTIONIST
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PATIENT_IMPORT'
WHERE r.name IN ('ADMIN', 'RECEPTIONIST')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
