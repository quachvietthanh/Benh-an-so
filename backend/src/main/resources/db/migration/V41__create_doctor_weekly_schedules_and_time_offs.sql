-- =====================================================
-- V41__create_doctor_weekly_schedules_and_time_offs.sql
-- NCL-03-CN-006: Manage doctor working schedule & time off (QTN-30, QTN-04)
-- =====================================================

-- 1. Weekly recurring schedule for doctors (Monday=1 .. Sunday=7)
CREATE TABLE doctor_weekly_schedules (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    day_of_week TINYINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_weekly_schedules PRIMARY KEY (id),
    CONSTRAINT uk_doctor_weekly_schedules UNIQUE (doctor_id, day_of_week, start_time),
    CONSTRAINT fk_doctor_weekly_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT ck_doctor_weekly_schedules_time_range CHECK (end_time > start_time),
    CONSTRAINT ck_doctor_weekly_schedules_day_of_week CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE INDEX idx_doctor_weekly_schedules_doctor_day
    ON doctor_weekly_schedules(doctor_id, day_of_week);

-- 2. Doctor time-off / leave intervals (ad-hoc breaks, vacations, leaves)
CREATE TABLE doctor_time_offs (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_doctor_time_offs PRIMARY KEY (id),
    CONSTRAINT fk_doctor_time_offs_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_doctor_time_offs_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_doctor_time_offs_time_range CHECK (end_time > start_time)
);

CREATE INDEX idx_doctor_time_offs_lookup
    ON doctor_time_offs(doctor_id, status, start_time, end_time);

-- 3. Register permissions for Doctor Schedule and Doctor Time Off management
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'DOCTOR_SCHEDULE_READ',
       'DOCTOR SCHEDULE READ',
       'APPOINTMENT',
       'Read doctor working schedules',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_SCHEDULE_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'DOCTOR_SCHEDULE_UPDATE',
       'DOCTOR SCHEDULE UPDATE',
       'APPOINTMENT',
       'Create and update doctor working schedules',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_SCHEDULE_UPDATE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'DOCTOR_TIME_OFF_CREATE',
       'DOCTOR TIME OFF CREATE',
       'APPOINTMENT',
       'Register doctor time-off and leave intervals',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_TIME_OFF_CREATE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'DOCTOR_TIME_OFF_READ',
       'DOCTOR TIME OFF READ',
       'APPOINTMENT',
       'Read doctor time-off and leave intervals',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_TIME_OFF_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'DOCTOR_TIME_OFF_DELETE',
       'DOCTOR TIME OFF DELETE',
       'APPOINTMENT',
       'Cancel doctor time-off intervals',
       TRUE,
       NOW(),
       NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DOCTOR_TIME_OFF_DELETE');

-- Assign permissions to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code IN ('DOCTOR_SCHEDULE_READ', 'DOCTOR_SCHEDULE_UPDATE', 'DOCTOR_TIME_OFF_CREATE', 'DOCTOR_TIME_OFF_READ', 'DOCTOR_TIME_OFF_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );

-- Assign permissions to MANAGER (66666666-6666-6666-6666-666666666666)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), p.id
FROM permissions p
WHERE p.code IN ('DOCTOR_SCHEDULE_READ', 'DOCTOR_SCHEDULE_UPDATE', 'DOCTOR_TIME_OFF_CREATE', 'DOCTOR_TIME_OFF_READ', 'DOCTOR_TIME_OFF_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('66666666-6666-6666-6666-666666666666')
        AND rp.permission_id = p.id
  );

-- Assign READ permissions to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code IN ('DOCTOR_SCHEDULE_READ', 'DOCTOR_TIME_OFF_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
        AND rp.permission_id = p.id
  );

-- Assign READ permissions to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code IN ('DOCTOR_SCHEDULE_READ', 'DOCTOR_TIME_OFF_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
        AND rp.permission_id = p.id
  );
