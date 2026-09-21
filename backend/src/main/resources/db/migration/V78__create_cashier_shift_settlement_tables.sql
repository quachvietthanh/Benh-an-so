-- =====================================================
-- V78__create_cashier_shift_settlement_tables.sql
-- Cashier shift end-of-day settlement (NCL-07-CN-009, QTN-38)
-- =====================================================

-- 1. Code sequences for cashier shift receipts
CREATE TABLE cashier_shift_code_sequences (
    code_prefix VARCHAR(10) NOT NULL,
    `last_value` BIGINT NOT NULL,

    CONSTRAINT pk_cashier_shift_code_sequences PRIMARY KEY (code_prefix),
    CONSTRAINT chk_cashier_shift_code_sequences_last_value CHECK (`last_value` >= 0)
);

INSERT INTO cashier_shift_code_sequences (code_prefix, `last_value`) VALUES ('CS', 0);

-- 2. Cashier shifts table
CREATE TABLE cashier_shifts (
    id BINARY(16) NOT NULL,
    shift_code VARCHAR(30) NOT NULL,
    cashier_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    total_transactions INT NOT NULL DEFAULT 0,
    total_system_amount DECIMAL(15, 2) NOT NULL,
    system_cash_amount DECIMAL(15, 2) NOT NULL,
    system_transfer_amount DECIMAL(15, 2) NOT NULL,
    system_card_amount DECIMAL(15, 2) NOT NULL,
    system_other_amount DECIMAL(15, 2) NOT NULL,
    actual_cash_amount DECIMAL(15, 2) NOT NULL,
    difference_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT NULL,
    confirmed_by BINARY(16) NULL,
    confirmed_at TIMESTAMP NULL,
    confirmation_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_cashier_shifts PRIMARY KEY (id),
    CONSTRAINT uk_cashier_shifts_code UNIQUE (shift_code),

    CONSTRAINT fk_cashier_shifts_cashier
        FOREIGN KEY (cashier_id)
        REFERENCES users(id),

    CONSTRAINT fk_cashier_shifts_confirmed_by
        FOREIGN KEY (confirmed_by)
        REFERENCES users(id),

    CONSTRAINT chk_cashier_shifts_actual_cash
        CHECK (actual_cash_amount >= 0),

    CONSTRAINT chk_cashier_shifts_total_transactions
        CHECK (total_transactions >= 0),

    CONSTRAINT chk_cashier_shifts_difference
        CHECK (difference_amount = actual_cash_amount - system_cash_amount),

    CONSTRAINT chk_cashier_shifts_status
        CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'REJECTED'))
);

CREATE INDEX idx_cashier_shifts_cashier
    ON cashier_shifts(cashier_id);

CREATE INDEX idx_cashier_shifts_created_at
    ON cashier_shifts(created_at);

CREATE INDEX idx_cashier_shifts_status
    ON cashier_shifts(status);

-- 3. Link payments to cashier shifts
ALTER TABLE payments
    ADD COLUMN cashier_shift_id BINARY(16) NULL;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_cashier_shift
        FOREIGN KEY (cashier_shift_id)
        REFERENCES cashier_shifts(id);

CREATE INDEX idx_payments_cashier_shift
    ON payments(cashier_shift_id);

-- 4. Permissions for Cashier Shift Settlement
INSERT INTO permissions (id, code, category, active, created_at)
SELECT UUID_TO_BIN(UUID()), 'CASHIER_SHIFT_READ', 'BILLING', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CASHIER_SHIFT_READ');

INSERT INTO permissions (id, code, category, active, created_at)
SELECT UUID_TO_BIN(UUID()), 'CASHIER_SHIFT_CREATE', 'BILLING', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CASHIER_SHIFT_CREATE');

INSERT INTO permissions (id, code, category, active, created_at)
SELECT UUID_TO_BIN(UUID()), 'CASHIER_SHIFT_CONFIRM', 'BILLING', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CASHIER_SHIFT_CONFIRM');

-- Grant permissions to RECEPTIONIST (44444444-4444-4444-4444-444444444444)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('44444444-4444-4444-4444-444444444444'), p.id
FROM permissions p
WHERE p.code IN ('CASHIER_SHIFT_READ', 'CASHIER_SHIFT_CREATE')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('44444444-4444-4444-4444-444444444444')
    AND rp.permission_id = p.id
);

-- Grant permissions to MANAGER (66666666-6666-6666-6666-666666666666)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('66666666-6666-6666-6666-666666666666'), p.id
FROM permissions p
WHERE p.code IN ('CASHIER_SHIFT_READ', 'CASHIER_SHIFT_CONFIRM')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('66666666-6666-6666-6666-666666666666')
    AND rp.permission_id = p.id
);

-- Grant permissions to ADMIN (11111111-1111-1111-1111-111111111111)
INSERT INTO role_permissions (role_id, permission_id)
SELECT UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), p.id
FROM permissions p
WHERE p.code IN ('CASHIER_SHIFT_READ', 'CASHIER_SHIFT_CREATE', 'CASHIER_SHIFT_CONFIRM')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
    AND rp.permission_id = p.id
);
