CREATE TABLE payment_service_fees (
    id BINARY(16) NOT NULL,
    payment_id BINARY(16) NOT NULL,
    clinical_order_item_id BINARY(16) NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_payment_service_fees PRIMARY KEY (id),
    CONSTRAINT uk_payment_service_fee_item UNIQUE (payment_id, clinical_order_item_id),
    CONSTRAINT fk_payment_service_fee_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_payment_service_fee_clinical_item
        FOREIGN KEY (clinical_order_item_id) REFERENCES clinical_order_items(id),
    CONSTRAINT chk_payment_service_fee_amount CHECK (amount >= 0)
);

CREATE INDEX idx_payment_service_fees_payment
    ON payment_service_fees(payment_id);
