-- =====================================================
-- V42__create_doctor_weekly_schedules_and_time_offs.sql
-- NCL-03-CN-006: Quản lý lịch làm việc và thời gian nghỉ của bác sĩ
-- Business Rules: QTN-30, QTN-04, QTN-01, QTN-31
-- Acceptance Criteria: TC-01, TC-02, TC-03, TC-04, TC-05
-- =====================================================

-- 1. Weekly recurring schedule for doctors (TC-01 / QTN-30)
CREATE TABLE doctor_weekly_schedules (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_weekly_schedules PRIMARY KEY (id),
    CONSTRAINT uk_doctor_weekly_schedules UNIQUE (doctor_id, day_of_week),
    CONSTRAINT fk_doctor_weekly_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT ck_doctor_weekly_schedules_time_range CHECK (end_time > start_time),
    CONSTRAINT ck_doctor_weekly_schedules_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

CREATE INDEX idx_doctor_weekly_schedules_doctor ON doctor_weekly_schedules(doctor_id, active);

-- 2. Doctor time-off / leave intervals (TC-02, TC-03 / QTN-30)
CREATE TABLE doctor_time_offs (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_time_offs PRIMARY KEY (id),
    CONSTRAINT fk_doctor_time_offs_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_doctor_time_offs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_doctor_time_offs_time_range CHECK (end_time > start_time),
    CONSTRAINT ck_doctor_time_offs_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT ck_doctor_time_offs_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
);

CREATE INDEX idx_doctor_time_offs_doctor_time ON doctor_time_offs(doctor_id, status, start_time, end_time);
CREATE INDEX idx_doctor_time_offs_created_by ON doctor_time_offs(created_by);

-- 3. Seed new permissions for doctor schedule and time-off management
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'DOCTOR_SCHEDULE_READ', 'DOCTOR SCHEDULE READ', 'APPOINTMENT', 'View doctor working schedules', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_SCHEDULE_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'DOCTOR_SCHEDULE_UPDATE', 'DOCTOR SCHEDULE UPDATE', 'APPOINTMENT', 'Configure doctor working schedules', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_SCHEDULE_UPDATE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'DOCTOR_TIMEOFF_CREATE', 'DOCTOR TIMEOFF CREATE', 'APPOINTMENT', 'Register doctor time-off intervals', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_TIMEOFF_CREATE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'DOCTOR_TIMEOFF_READ', 'DOCTOR TIMEOFF READ', 'APPOINTMENT', 'View doctor time-off intervals and affected appointments', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_TIMEOFF_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'DOCTOR_TIMEOFF_CANCEL', 'DOCTOR TIMEOFF CANCEL', 'APPOINTMENT', 'Cancel doctor time-off intervals', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_TIMEOFF_CANCEL');

-- 4. Grant permissions to roles:
-- ADMIN (11111111-1111-1111-1111-111111111111) and MANAGER (66666666-6666-6666-6666-666666666666) get all 5 permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.id IN (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), UUID_TO_BIN('66666666-6666-6666-6666-666666666666'))
  AND p.code IN ('DOCTOR_SCHEDULE_READ', 'DOCTOR_SCHEDULE_UPDATE', 'DOCTOR_TIMEOFF_CREATE', 'DOCTOR_TIMEOFF_READ', 'DOCTOR_TIMEOFF_CANCEL')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- RECEPTIONIST (44444444-4444-4444-4444-444444444444) and DOCTOR (22222222-2222-2222-2222-222222222222) get read permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.id IN (UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), UUID_TO_BIN('22222222-2222-2222-2222-222222222222'))
  AND p.code IN ('DOCTOR_SCHEDULE_READ', 'DOCTOR_TIMEOFF_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
