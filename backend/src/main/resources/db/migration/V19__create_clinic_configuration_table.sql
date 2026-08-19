-- =====================================================
-- NCL-09-CN-002 - Shared clinic configuration.
-- The fixed primary key enforces one configuration per system.
-- =====================================================

CREATE TABLE clinic_configuration (
    id TINYINT NOT NULL,
    clinic_name VARCHAR(150) NOT NULL,
    address VARCHAR(500) NULL,
    phone VARCHAR(30) NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_clinic_configuration PRIMARY KEY (id),
    CONSTRAINT chk_clinic_configuration_singleton CHECK (id = 1),
    CONSTRAINT chk_clinic_configuration_hours CHECK (closing_time > opening_time)
);

INSERT INTO clinic_configuration (
    id, clinic_name, address, phone, opening_time, closing_time, created_at, updated_at
) VALUES (
    1,
    'Phong kham Benh So An',
    'Thai Nguyen',
    '0345678910',
    '08:00:00',
    '17:00:00',
    '2026-08-01 08:00:00',
    '2026-08-01 08:00:00'
);
