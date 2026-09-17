-- =====================================================
-- V67__create_visit_handover_tables.sql
-- Add initial_doctor_id to visits table,
-- create visit_handovers table for patient handover history,
-- and seed MEDICAL_RECORD_HANDOVER permission (NCL-04-CN-014 / QTN-17, QTN-11).
-- =====================================================

-- 1. Add initial_doctor_id to visits
ALTER TABLE visits
    ADD COLUMN initial_doctor_id BINARY(16) NULL;

ALTER TABLE visits
    ADD CONSTRAINT fk_visits_initial_doctor
    FOREIGN KEY (initial_doctor_id)
    REFERENCES users(id);

CREATE INDEX idx_visits_initial_doctor
    ON visits(initial_doctor_id);

-- 2. Create visit_handovers table
CREATE TABLE visit_handovers (
    id BINARY(16) NOT NULL,
    visit_id BINARY(16) NOT NULL,
    from_doctor_id BINARY(16) NOT NULL,
    to_doctor_id BINARY(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    handed_over_at TIMESTAMP NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_visit_handovers PRIMARY KEY (id),
    CONSTRAINT fk_visit_handovers_visit FOREIGN KEY (visit_id) REFERENCES visits(id),
    CONSTRAINT fk_visit_handovers_from_doctor FOREIGN KEY (from_doctor_id) REFERENCES users(id),
    CONSTRAINT fk_visit_handovers_to_doctor FOREIGN KEY (to_doctor_id) REFERENCES users(id),
    CONSTRAINT fk_visit_handovers_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_visit_handovers_reason_not_blank CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT ck_visit_handovers_different_doctors CHECK (from_doctor_id <> to_doctor_id)
);

CREATE INDEX idx_visit_handovers_visit ON visit_handovers(visit_id);
CREATE INDEX idx_visit_handovers_to_doctor ON visit_handovers(to_doctor_id);
CREATE INDEX idx_visit_handovers_from_doctor ON visit_handovers(from_doctor_id);

-- 3. Seed MEDICAL_RECORD_HANDOVER permission
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_HANDOVER', 'MEDICAL RECORD HANDOVER', 'MEDICAL_RECORD', 'Handover patient to another doctor with reason', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = 'MEDICAL_RECORD_HANDOVER'
);

-- 4. Grant MEDICAL_RECORD_HANDOVER to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code = 'MEDICAL_RECORD_HANDOVER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
        AND rp.permission_id = p.id
  );

-- 5. Grant MEDICAL_RECORD_HANDOVER and USER_READ to DOCTOR (22222222-2222-2222-2222-222222222222)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), p.id
FROM permissions p
WHERE p.code IN ('MEDICAL_RECORD_HANDOVER', 'USER_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
        AND rp.permission_id = p.id
  );
