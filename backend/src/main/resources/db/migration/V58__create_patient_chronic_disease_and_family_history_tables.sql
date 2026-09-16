-- =====================================================
-- V58__create_patient_chronic_disease_and_family_history_tables.sql
-- NCL-02-CN-009: Ghi tiền sử bệnh mạn tính và tiền sử gia đình
-- Chronic disease history and family history are patient-level records
-- referencing the existing diagnosis_catalog (disease/ICD code catalog).
-- =====================================================

CREATE TABLE patient_chronic_diseases (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    diagnosis_catalog_id BINARY(16) NOT NULL,
    year_detected INT NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    active_diagnosis_catalog_id BINARY(16) GENERATED ALWAYS AS (CASE WHEN active = TRUE THEN diagnosis_catalog_id ELSE NULL END),
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patient_chronic_diseases PRIMARY KEY (id),
    CONSTRAINT fk_patient_chronic_diseases_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_chronic_diseases_catalog FOREIGN KEY (diagnosis_catalog_id) REFERENCES diagnosis_catalog(id),
    CONSTRAINT fk_patient_chronic_diseases_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_patient_chronic_diseases_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT uk_patient_active_chronic_disease UNIQUE (patient_id, active_diagnosis_catalog_id)
);

CREATE INDEX idx_patient_chronic_diseases_patient ON patient_chronic_diseases(patient_id, active);

CREATE TABLE patient_family_histories (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    diagnosis_catalog_id BINARY(16) NOT NULL,
    notes TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_patient_family_histories PRIMARY KEY (id),
    CONSTRAINT fk_patient_family_histories_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_patient_family_histories_catalog FOREIGN KEY (diagnosis_catalog_id) REFERENCES diagnosis_catalog(id),
    CONSTRAINT fk_patient_family_histories_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_patient_family_histories_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_patient_family_histories_relationship CHECK (CHAR_LENGTH(TRIM(relationship)) BETWEEN 1 AND 100)
);

CREATE INDEX idx_patient_family_histories_patient ON patient_family_histories(patient_id, active);

-- Seed permissions (mirrors patient allergy permission pattern)
INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'PATIENT_CHRONIC_DISEASE_WRITE', 'PATIENT CHRONIC DISEASE WRITE', 'PATIENT',
       'Record or remove patient chronic disease history (NCL-02-CN-009)', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PATIENT_CHRONIC_DISEASE_WRITE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'PATIENT_CHRONIC_DISEASE_READ', 'PATIENT CHRONIC DISEASE READ', 'PATIENT',
       'View patient chronic disease history', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PATIENT_CHRONIC_DISEASE_READ');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'PATIENT_FAMILY_HISTORY_WRITE', 'PATIENT FAMILY HISTORY WRITE', 'PATIENT',
       'Record or remove patient family history (NCL-02-CN-009)', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PATIENT_FAMILY_HISTORY_WRITE');

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'PATIENT_FAMILY_HISTORY_READ', 'PATIENT FAMILY HISTORY READ', 'PATIENT',
       'View patient family history', TRUE, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PATIENT_FAMILY_HISTORY_READ');

-- Grant WRITE to ADMIN and DOCTOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code IN ('PATIENT_CHRONIC_DISEASE_WRITE', 'PATIENT_FAMILY_HISTORY_WRITE')
  AND r.id IN (
      UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), -- ADMIN
      UUID_TO_BIN('22222222-2222-2222-2222-222222222222')  -- DOCTOR
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant READ to ADMIN, DOCTOR, PHARMACIST, MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code IN ('PATIENT_CHRONIC_DISEASE_READ', 'PATIENT_FAMILY_HISTORY_READ')
  AND r.id IN (
      UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), -- ADMIN
      UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), -- DOCTOR
      UUID_TO_BIN('55555555-5555-5555-5555-555555555555'), -- PHARMACIST
      UUID_TO_BIN('66666666-6666-6666-6666-666666666666')  -- MANAGER
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
