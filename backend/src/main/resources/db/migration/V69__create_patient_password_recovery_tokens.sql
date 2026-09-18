-- =====================================================
-- V67__create_patient_password_recovery_tokens.sql
-- Patient Portal Password Recovery (NCL-14-CN-006 / QTN-28)
-- Stores verification codes with expiration, attempts counter,
-- and used status to support anti-brute-force and replay protection.
-- =====================================================

CREATE TABLE patient_password_recovery_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_patient_password_recovery_tokens PRIMARY KEY (id),
    CONSTRAINT fk_patient_password_recovery_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_recovery_tokens_phone_created
    ON patient_password_recovery_tokens(phone, created_at);

CREATE INDEX idx_recovery_tokens_user_expires
    ON patient_password_recovery_tokens(user_id, expires_at);
