-- =====================================================
-- V19__create_and_seed_configurations.sql
-- Clinic & System Configurations (Consolidated from V19, V23, V40)
-- =====================================================

CREATE TABLE clinic_configuration (
    id INT NOT NULL,
    clinic_name VARCHAR(150) NOT NULL,
    address VARCHAR(500) NULL,
    phone VARCHAR(30) NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    retention_years INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_clinic_configuration PRIMARY KEY (id),
    CONSTRAINT chk_clinic_configuration_singleton CHECK (id = 1),
    CONSTRAINT chk_clinic_configuration_hours CHECK (closing_time > opening_time),
    CONSTRAINT chk_clinic_configuration_retention_years CHECK (retention_years >= 10)
);

CREATE TABLE system_configuration (
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_system_configuration PRIMARY KEY (config_key),
    CONSTRAINT fk_system_configuration_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Seed clinic configuration (Demo US-40: Cấu hình thông tin phòng khám & US-68: Lưu trữ hồ sơ >= 10 năm)
INSERT INTO clinic_configuration (
    id, clinic_name, address, phone, opening_time, closing_time, retention_years, created_at, updated_at
) VALUES (
    1,
    'Phòng Khám Đa Khoa Bệnh Án Số',
    '123 Đường Giải Phóng, Quận Đống Đa, TP. Hà Nội',
    '024 3869 1234',
    '07:30:00',
    '17:30:00',
    10,
    '2026-08-01 08:00:00',
    CURRENT_TIMESTAMP
);

-- Seed system configuration (Demo US-70: Chế độ ẩn danh dữ liệu)
INSERT INTO system_configuration (config_key, config_value, updated_by, updated_at)
VALUES (
    'anonymization.enabled',
    'false',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
    CURRENT_TIMESTAMP
);
