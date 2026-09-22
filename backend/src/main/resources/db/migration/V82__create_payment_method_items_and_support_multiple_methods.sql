-- =====================================================
-- V82__create_payment_method_items_and_support_multiple_methods.sql
-- NCL-07-CN-007: Thu phí nhiều phương thức và ghi nhận phương thức thanh toán
-- Supports multiple payment method breakdowns per payment, transaction reference
-- numbers for non-cash transfers, and updates payments.payment_method constraint.
-- =====================================================

CREATE TABLE payment_method_items (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    reference_number VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_payment_method_items PRIMARY KEY (id),

    CONSTRAINT fk_payment_method_items_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_method_items_amount
        CHECK (amount > 0),

    CONSTRAINT chk_payment_method_items_method
        CHECK (
            payment_method IN (
                'CASH',
                'CARD',
                'BANK_TRANSFER',
                'QR_CODE',
                'E_WALLET'
            )
        )
);

CREATE INDEX idx_payment_method_items_payment
    ON payment_method_items(payment_id);

ALTER TABLE payments DROP CHECK chk_payments_method;

ALTER TABLE payments ADD CONSTRAINT chk_payments_method
    CHECK (
        payment_method IN (
            'CASH',
            'CARD',
            'BANK_TRANSFER',
            'QR_CODE',
            'E_WALLET',
            'MULTIPLE'
        )
    );

INSERT INTO payment_method_items (id, payment_id, payment_method, amount, reference_number, created_at)
SELECT UUID_TO_BIN(UUID()), id, payment_method, amount_paid, NULL, paid_at
FROM payments
WHERE amount_paid IS NOT NULL AND amount_paid > 0 AND payment_method IS NOT NULL;

