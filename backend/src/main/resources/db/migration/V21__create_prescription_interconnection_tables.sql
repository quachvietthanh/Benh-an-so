-- =====================================================
-- NCL-12-CN-004 / NCL-12-CN-005
-- Prescription interconnection state and append-only attempt history
-- =====================================================

ALTER TABLE prescriptions
    ADD COLUMN interconnection_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SENT',
    ADD COLUMN last_interconnection_at TIMESTAMP NULL,
    ADD COLUMN last_interconnection_error TEXT NULL,
    ADD COLUMN interconnection_receipt_code VARCHAR(50) NULL,
    ADD CONSTRAINT chk_prescriptions_interconnection_status
        CHECK (interconnection_status IN ('NOT_SENT', 'SUCCESS', 'FAILED')),
    ADD CONSTRAINT chk_prescriptions_interconnection_state
        CHECK (
            (interconnection_status = 'NOT_SENT'
                AND last_interconnection_at IS NULL
                AND last_interconnection_error IS NULL
                AND interconnection_receipt_code IS NULL)
            OR (interconnection_status = 'SUCCESS'
                AND last_interconnection_at IS NOT NULL
                AND last_interconnection_error IS NULL
                AND interconnection_receipt_code IS NOT NULL)
            OR (interconnection_status = 'FAILED'
                AND last_interconnection_at IS NOT NULL
                AND last_interconnection_error IS NOT NULL
                AND interconnection_receipt_code IS NULL)
        );

-- Explicitly documents the backfill for existing prescriptions. The NOT NULL
-- default above makes this update safe on populated MySQL tables.
UPDATE prescriptions
SET interconnection_status = 'NOT_SENT'
WHERE interconnection_status IS NULL;

CREATE INDEX idx_prescriptions_interconnection_status_at
    ON prescriptions(interconnection_status, last_interconnection_at);

CREATE TABLE prescription_interconnection_logs (
    id BINARY(16) NOT NULL,
    prescription_id BINARY(16) NOT NULL,
    attempt_number INT NOT NULL,
    attempt_type VARCHAR(10) NOT NULL,
    outcome VARCHAR(10) NOT NULL,
    request_payload JSON NOT NULL,
    response_payload JSON NULL,
    receipt_code VARCHAR(50) NULL,
    failure_reason TEXT NULL,
    attempted_by BINARY(16) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_prescription_interconnection_logs PRIMARY KEY (id),
    CONSTRAINT uk_prescription_interconnection_logs_attempt
        UNIQUE (prescription_id, attempt_number),
    CONSTRAINT fk_prescription_interconnection_logs_prescription
        FOREIGN KEY (prescription_id)
        REFERENCES prescriptions(id),
    CONSTRAINT fk_prescription_interconnection_logs_attempted_by
        FOREIGN KEY (attempted_by)
        REFERENCES users(id),
    CONSTRAINT chk_prescription_interconnection_logs_attempt_number
        CHECK (attempt_number > 0),
    CONSTRAINT chk_prescription_interconnection_logs_type
        CHECK (attempt_type IN ('SEND', 'RETRY')),
    CONSTRAINT chk_prescription_interconnection_logs_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_prescription_interconnection_logs_completed_after_started
        CHECK (completed_at >= started_at),
    CONSTRAINT chk_prescription_interconnection_logs_result
        CHECK (
            (outcome = 'SUCCESS'
                AND receipt_code IS NOT NULL
                AND failure_reason IS NULL)
            OR (outcome = 'FAILED'
                AND receipt_code IS NULL
                AND failure_reason IS NOT NULL)
        )
);

CREATE INDEX idx_prescription_interconnection_logs_prescription_attempt
    ON prescription_interconnection_logs(prescription_id, attempt_number);

CREATE INDEX idx_prescription_interconnection_logs_attempted_by_started
    ON prescription_interconnection_logs(attempted_by, started_at);
