ALTER TABLE payments
    ADD COLUMN refund_reason TEXT NULL;

ALTER TABLE payments
    ADD COLUMN refunded_by BINARY(16) NULL;

ALTER TABLE payments
    ADD COLUMN refunded_at TIMESTAMP NULL;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_refunded_by
    FOREIGN KEY (refunded_by)
    REFERENCES users(id);

CREATE INDEX idx_payments_refunded_at
    ON payments(refunded_at);
