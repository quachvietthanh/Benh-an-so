-- =====================================================
-- V29 - NCL-13-CN-003 medical-record template foundation.
-- Existing visits are assigned to the seeded GENERAL specialty.
-- =====================================================

CREATE TABLE specialties (
    id BINARY(16) NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_specialties PRIMARY KEY (id),
    CONSTRAINT uk_specialties_code UNIQUE (code)
);

INSERT INTO specialties (id, code, name, active, created_at, updated_at)
SELECT UUID_TO_BIN('f0000000-0000-0000-0000-000000000001'), 'GENERAL', 'General', TRUE,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM specialties WHERE code = 'GENERAL'
);

INSERT INTO specialties (id, code, name, active, created_at, updated_at)
SELECT UUID_TO_BIN('f0000000-0000-0000-0000-000000000002'), 'INTERNAL_MEDICINE', 'Internal medicine', TRUE,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM specialties WHERE code = 'INTERNAL_MEDICINE'
);

ALTER TABLE visits
    ADD COLUMN specialty_id BINARY(16) NULL;

UPDATE visits
SET specialty_id = UUID_TO_BIN('f0000000-0000-0000-0000-000000000001')
WHERE specialty_id IS NULL;

ALTER TABLE visits
    MODIFY COLUMN specialty_id BINARY(16) NOT NULL
        DEFAULT X'f0000000000000000000000000000001';

ALTER TABLE visits
    ADD CONSTRAINT fk_visits_specialty
    FOREIGN KEY (specialty_id) REFERENCES specialties (id);

CREATE INDEX idx_visits_specialty
    ON visits (specialty_id);

CREATE TABLE medical_record_templates (
    id BINARY(16) NOT NULL,
    specialty_id BINARY(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    name_key VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL,
    is_default BOOLEAN NOT NULL,
    active_default_specialty_id BINARY(16)
        GENERATED ALWAYS AS (
            CASE WHEN active = TRUE AND is_default = TRUE THEN specialty_id ELSE NULL END
        ) STORED,
    current_version_no INT NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by BINARY(16) NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_medical_record_templates PRIMARY KEY (id),
    CONSTRAINT uk_medical_record_templates_specialty_name UNIQUE (specialty_id, name_key),
    CONSTRAINT uk_medical_record_templates_active_default UNIQUE (active_default_specialty_id),
    CONSTRAINT fk_medical_record_templates_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    CONSTRAINT fk_medical_record_templates_created_by
        FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_medical_record_templates_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
);

CREATE INDEX idx_medical_record_templates_specialty_active
    ON medical_record_templates (specialty_id, active);

CREATE TABLE medical_record_template_versions (
    id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    version_no INT NOT NULL,
    specialty_id BINARY(16) NOT NULL,
    template_name VARCHAR(150) NOT NULL,
    change_note VARCHAR(500) NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_medical_record_template_versions PRIMARY KEY (id),
    CONSTRAINT uk_medical_record_template_versions_template_version UNIQUE (template_id, version_no),
    CONSTRAINT fk_medical_record_template_versions_template
        FOREIGN KEY (template_id) REFERENCES medical_record_templates (id),
    CONSTRAINT fk_medical_record_template_versions_specialty
        FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    CONSTRAINT fk_medical_record_template_versions_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_medical_record_template_versions_template
    ON medical_record_template_versions (template_id, version_no);

CREATE TABLE medical_record_template_sections (
    id BINARY(16) NOT NULL,
    template_version_id BINARY(16) NOT NULL,
    field_code VARCHAR(50) NOT NULL,
    label VARCHAR(150) NOT NULL,
    required BOOLEAN NOT NULL,
    display_order INT NOT NULL,

    CONSTRAINT pk_medical_record_template_sections PRIMARY KEY (id),
    CONSTRAINT uk_medical_record_template_sections_field UNIQUE (template_version_id, field_code),
    CONSTRAINT uk_medical_record_template_sections_order UNIQUE (template_version_id, display_order),
    CONSTRAINT fk_medical_record_template_sections_version
        FOREIGN KEY (template_version_id) REFERENCES medical_record_template_versions (id)
);

CREATE INDEX idx_medical_record_template_sections_version_order
    ON medical_record_template_sections (template_version_id, display_order);

ALTER TABLE medical_records
    ADD COLUMN applied_template_version_id BINARY(16) NULL;

ALTER TABLE medical_records
    ADD COLUMN template_applied_by BINARY(16) NULL;

ALTER TABLE medical_records
    ADD COLUMN template_applied_at TIMESTAMP NULL;

ALTER TABLE medical_records
    ADD CONSTRAINT fk_medical_records_template_version
    FOREIGN KEY (applied_template_version_id) REFERENCES medical_record_template_versions (id);

ALTER TABLE medical_records
    ADD CONSTRAINT fk_medical_records_template_applied_by
    FOREIGN KEY (template_applied_by) REFERENCES users (id);

CREATE INDEX idx_medical_records_template_version
    ON medical_records (applied_template_version_id);

INSERT INTO permissions (id, code, name, module, description, active, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), 'MEDICAL_RECORD_TEMPLATE_MANAGE', 'MEDICAL RECORD TEMPLATE MANAGE',
       'MEDICAL_RECORD_TEMPLATE', 'Manage specialty medical-record templates.', TRUE,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'MEDICAL_RECORD_TEMPLATE_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'MEDICAL_RECORD_TEMPLATE_MANAGE'
WHERE roles.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions
      WHERE role_permissions.role_id = roles.id
        AND role_permissions.permission_id = permissions.id
  );
