-- =====================================================
-- V90__create_patient_consent_history.sql
-- NCL-15-CN-005: Rút lại và cập nhật phiếu đồng ý xử lý dữ liệu cá nhân
-- Lưu trữ lịch sử các phiên bản phiếu đồng ý theo thời gian (AC-02),
-- hỗ trợ thu hẹp phạm vi đồng ý (AC-01) và bảo vệ dữ liệu theo QTN-19, QTN-24.
-- =====================================================

CREATE TABLE patient_consent_history (
    id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    version_number INT NOT NULL,
    version_code VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scopes VARCHAR(500) NOT NULL,
    consent_agreed BOOLEAN NOT NULL DEFAULT TRUE,
    consent_agreed_at TIMESTAMP NULL,
    consent_withdrawn BOOLEAN NOT NULL DEFAULT FALSE,
    consent_withdrawn_at TIMESTAMP NULL,
    consent_withdrawn_reason VARCHAR(500) NULL,
    non_medical_use_restricted BOOLEAN NOT NULL DEFAULT FALSE,
    signer_name VARCHAR(255) NULL,
    created_by BINARY(16) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_patient_consent_history PRIMARY KEY (id),
    CONSTRAINT fk_patient_consent_history_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_patient_consent_history_version
        UNIQUE (patient_id, version_number)
);

CREATE INDEX idx_patient_consent_history_patient
    ON patient_consent_history(patient_id, created_at DESC);

-- Backfill bản ghi lịch sử ban đầu (version 1) cho các bệnh nhân đã có thông tin consent
INSERT INTO patient_consent_history (
    id,
    patient_id,
    version_number,
    version_code,
    status,
    scopes,
    consent_agreed,
    consent_agreed_at,
    consent_withdrawn,
    consent_withdrawn_at,
    consent_withdrawn_reason,
    non_medical_use_restricted,
    signer_name,
    created_by,
    created_at
)
SELECT
    UUID_TO_BIN(UUID()),
    p.id,
    1,
    COALESCE(p.consent_version, 'v1.0'),
    CASE
        WHEN p.consent_withdrawn = TRUE THEN 'WITHDRAWN'
        WHEN p.non_medical_use_restricted = TRUE THEN 'PARTIALLY_WITHDRAWN'
        ELSE 'AGREED'
    END,
    CASE
        WHEN p.consent_withdrawn = TRUE THEN ''
        WHEN p.non_medical_use_restricted = TRUE THEN 'TREATMENT'
        ELSE 'TREATMENT,COMMUNICATION,RESEARCH'
    END,
    p.consent_agreed,
    p.consent_agreed_at,
    p.consent_withdrawn,
    p.consent_withdrawn_at,
    p.consent_withdrawn_reason,
    p.non_medical_use_restricted,
    p.consent_signer_name,
    p.created_by,
    COALESCE(p.consent_agreed_at, p.created_at, CURRENT_TIMESTAMP)
FROM patients p
WHERE p.consent_agreed = TRUE OR p.consent_agreed_at IS NOT NULL;
