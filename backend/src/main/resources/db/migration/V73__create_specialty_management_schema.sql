-- =====================================================
-- V73 - NCL-09-CN-007: Specialty and Room/Doctor Management Schema.
-- Allows Admin to manage specialties, prevent duplicates,
-- and associate doctors and rooms with specialties.
-- =====================================================

ALTER TABLE specialties
    ADD COLUMN description VARCHAR(500) NULL;

ALTER TABLE specialties
    ADD COLUMN name_key VARCHAR(100) NULL;

UPDATE specialties
SET name_key = LOWER(TRIM(name))
WHERE name_key IS NULL;

ALTER TABLE specialties
    MODIFY COLUMN name_key VARCHAR(100) NOT NULL;

ALTER TABLE specialties
    ADD CONSTRAINT uk_specialties_name_key UNIQUE (name_key);

CREATE TABLE doctor_specialties (
    id BINARY(16) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    specialty_id BINARY(16) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    assigned_by BINARY(16) NOT NULL,

    CONSTRAINT pk_doctor_specialties PRIMARY KEY (id),
    CONSTRAINT uk_doctor_specialties UNIQUE (doctor_id, specialty_id),
    CONSTRAINT fk_doctor_specialties_doctor FOREIGN KEY (doctor_id) REFERENCES users (id),
    CONSTRAINT fk_doctor_specialties_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id)
);

CREATE INDEX idx_doctor_specialties_specialty
    ON doctor_specialties (specialty_id);

CREATE INDEX idx_doctor_specialties_doctor
    ON doctor_specialties (doctor_id);

CREATE TABLE room_specialties (
    id BINARY(16) NOT NULL,
    room_id BINARY(16) NOT NULL,
    specialty_id BINARY(16) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_room_specialties PRIMARY KEY (id),
    CONSTRAINT uk_room_specialties UNIQUE (room_id, specialty_id),
    CONSTRAINT fk_room_specialties_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_room_specialties_specialty FOREIGN KEY (specialty_id) REFERENCES specialties (id)
);

CREATE INDEX idx_room_specialties_specialty
    ON room_specialties (specialty_id);

CREATE INDEX idx_room_specialties_room
    ON room_specialties (room_id);

-- Seed SPECIALTY_MANAGE permission and assign to ADMIN
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()),
       'SPECIALTY_MANAGE',
       'SPECIALTY MANAGE',
       'SPECIALTY',
       'Manage clinic specialties, assign doctors and rooms (NCL-09-CN-007 / QTN-01).',
       TRUE,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'SPECIALTY_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'SPECIALTY_MANAGE'
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
