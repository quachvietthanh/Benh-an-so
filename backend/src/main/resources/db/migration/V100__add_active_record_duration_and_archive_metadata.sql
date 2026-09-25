-- =====================================================
-- V100__add_active_record_duration_and_archive_metadata.sql
-- NCL-11-CN-008: Kho lưu trữ hồ sơ hết thời hạn và tra cứu kho lưu trữ
-- =====================================================

-- 1. Thêm cấu hình thời hạn hoạt động của bệnh án vào bảng clinic_configuration
ALTER TABLE clinic_configuration
    ADD COLUMN active_record_duration_months INT NOT NULL DEFAULT 12;

ALTER TABLE clinic_configuration
    ADD CONSTRAINT chk_clinic_configuration_active_duration
    CHECK (active_record_duration_months >= 1);

-- 2. Thêm thông tin vết lưu trữ vào bảng medical_records
ALTER TABLE medical_records
    ADD COLUMN archived_at TIMESTAMP NULL,
    ADD COLUMN archived_by BINARY(16) NULL;

ALTER TABLE medical_records
    ADD CONSTRAINT fk_medical_records_archived_by
    FOREIGN KEY (archived_by) REFERENCES users(id);

CREATE INDEX idx_medical_records_status_archived_at
    ON medical_records(status, archived_at);

-- 3. Bổ sung các permissions phục vụ quản lý và tra cứu kho lưu trữ
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_ARCHIVE_MANAGE', 'MEDICAL RECORD ARCHIVE MANAGE', 'MEDICAL_RECORD',
       'Manage medical record archiving (view eligible, archive single/batch)', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_ARCHIVE_MANAGE'
);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_ARCHIVE_READ', 'MEDICAL RECORD ARCHIVE READ', 'MEDICAL_RECORD',
       'Search and view archived medical records in archive repository', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_ARCHIVE_READ'
);

-- 4. Phân quyền:
-- MEDICAL_RECORD_ARCHIVE_MANAGE: ADMIN và MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'MEDICAL_RECORD_ARCHIVE_MANAGE'
WHERE roles.name IN ('ADMIN', 'MANAGER')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );

-- MEDICAL_RECORD_ARCHIVE_READ: ADMIN, MANAGER và DOCTOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'MEDICAL_RECORD_ARCHIVE_READ'
WHERE roles.name IN ('ADMIN', 'MANAGER', 'DOCTOR')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id AND role_permissions.permission_id = permissions.id
  );
