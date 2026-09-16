-- =====================================================
-- V59__add_signing_deadline_and_create_signing_reminders.sql
-- NCL-11-CN-006: Theo dõi và nhắc ký bệnh án quá hạn (QTN-29, QTN-17)
-- 1. Add signing_deadline_hours to clinic_configuration
-- 2. Create medical_record_signing_reminders table for logging reminders
-- 3. Seed permissions and role assignments
-- =====================================================

-- 1. Clinic configuration signing deadline
ALTER TABLE clinic_configuration
    ADD COLUMN signing_deadline_hours INT NOT NULL DEFAULT 24;

ALTER TABLE clinic_configuration
    ADD CONSTRAINT chk_clinic_configuration_signing_deadline CHECK (signing_deadline_hours >= 1);

-- 2. Medical record signing reminders log
CREATE TABLE medical_record_signing_reminders (
    id BINARY(16) NOT NULL,
    medical_record_id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    reminded_by BINARY(16) NOT NULL,
    reminded_at TIMESTAMP NOT NULL,
    overdue_hours BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'MOCK',
    notes TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SENT',

    CONSTRAINT pk_medical_record_signing_reminders PRIMARY KEY (id),
    CONSTRAINT fk_signing_reminders_record FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_signing_reminders_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_signing_reminders_reminded_by FOREIGN KEY (reminded_by) REFERENCES users(id),
    CONSTRAINT chk_signing_reminders_status CHECK (status IN ('SENT', 'FAILED'))
);

CREATE INDEX idx_signing_reminders_record ON medical_record_signing_reminders(medical_record_id, reminded_at);
CREATE INDEX idx_signing_reminders_doctor ON medical_record_signing_reminders(doctor_id, reminded_at);
CREATE INDEX idx_visits_completed_status_doctor ON visits(completed_at, status, doctor_id);
CREATE INDEX idx_medical_records_visit_status ON medical_records(visit_id, status);

-- 3. Seed permissions
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_OVERDUE_READ', 'MEDICAL RECORD OVERDUE READ', 'MEDICAL_RECORD',
       'View overdue medical record signing list (NCL-11-CN-006)', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_OVERDUE_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_REMIND_SIGN', 'MEDICAL RECORD REMIND SIGN', 'MEDICAL_RECORD',
       'Send medical record signing reminders to doctors (NCL-11-CN-006)', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_REMIND_SIGN');

-- Grant MEDICAL_RECORD_OVERDUE_READ to ADMIN, MANAGER, DOCTOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'MEDICAL_RECORD_OVERDUE_READ'
  AND r.id IN (
      UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), -- ADMIN
      UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), -- MANAGER
      UUID_TO_BIN('22222222-2222-2222-2222-222222222222')  -- DOCTOR
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant MEDICAL_RECORD_REMIND_SIGN to ADMIN and MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'MEDICAL_RECORD_REMIND_SIGN'
  AND r.id IN (
      UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), -- ADMIN
      UUID_TO_BIN('66666666-6666-6666-6666-666666666666')  -- MANAGER
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
