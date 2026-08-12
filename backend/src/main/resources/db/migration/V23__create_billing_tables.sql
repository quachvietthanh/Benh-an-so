-- =====================================================
-- V23__create_billing_tables.sql
-- Billing schema for payments and invoices
-- =====================================================

-- ===========================
-- Payments
-- ===========================

CREATE TABLE payments (
    id BINARY(16) NOT NULL,
    visit_id BINARY(16) NOT NULL,
    exam_fee DECIMAL(15, 2) NOT NULL,
    medicine_fee DECIMAL(15, 2) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    amount_paid DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    collected_by BINARY(16) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_visit UNIQUE (visit_id),

    CONSTRAINT fk_payments_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_payments_collected_by
        FOREIGN KEY (collected_by)
        REFERENCES users(id),

    CONSTRAINT chk_payments_exam_fee
        CHECK (exam_fee >= 0),

    CONSTRAINT chk_payments_medicine_fee
        CHECK (medicine_fee >= 0),

    CONSTRAINT chk_payments_total_amount
        CHECK (total_amount >= 0),

    CONSTRAINT chk_payments_amount_paid
        CHECK (amount_paid >= 0),

    CONSTRAINT chk_payments_total_formula
        CHECK (total_amount = exam_fee + medicine_fee),

    CONSTRAINT chk_payments_amount_match
        CHECK (amount_paid = total_amount),

    CONSTRAINT chk_payments_method
        CHECK (
            payment_method IN (
                'CASH',
                'CARD',
                'BANK_TRANSFER',
                'QR_CODE',
                'E_WALLET'
            )
        ),

    CONSTRAINT chk_payments_status
        CHECK (status IN ('RECORDED', 'SUCCESS', 'REFUNDED', 'CANCELLED')),

    CONSTRAINT chk_payments_created_at
        CHECK (created_at <= paid_at)
);

CREATE INDEX idx_payments_paid_at
    ON payments(paid_at);

CREATE INDEX idx_payments_collected_by
    ON payments(collected_by);

-- ===========================
-- Invoices
-- ===========================

CREATE TABLE invoices (
    id BINARY(16) NOT NULL,
    invoice_code VARCHAR(30) NOT NULL,
    visit_id BINARY(16) NOT NULL,
    payment_id BINARY(16) NULL,
    invoice_type VARCHAR(30) NOT NULL,
    original_invoice_id BINARY(16) NULL,
    adjustment_reason TEXT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT uk_invoices_code UNIQUE (invoice_code),
    CONSTRAINT uk_invoices_payment UNIQUE (payment_id),

    CONSTRAINT fk_invoices_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_invoices_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id),

    CONSTRAINT fk_invoices_original
        FOREIGN KEY (original_invoice_id)
        REFERENCES invoices(id),

    CONSTRAINT fk_invoices_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id),

    CONSTRAINT chk_invoices_type
        CHECK (invoice_type IN ('ORIGINAL', 'ADJUSTMENT')),

    CONSTRAINT chk_invoices_original_shape
        CHECK (
            (
                invoice_type = 'ORIGINAL'
                AND payment_id IS NOT NULL
                AND original_invoice_id IS NULL
                AND adjustment_reason IS NULL
                AND total_amount > 0
            )
            OR
            (
                invoice_type = 'ADJUSTMENT'
                AND payment_id IS NULL
                AND original_invoice_id IS NOT NULL
                AND adjustment_reason IS NOT NULL
                AND CHAR_LENGTH(TRIM(adjustment_reason)) > 0
                AND total_amount <> 0
            )
        )
);

CREATE INDEX idx_invoices_visit_created
    ON invoices(visit_id, created_at);

CREATE INDEX idx_invoices_type_created
    ON invoices(invoice_type, created_at);

CREATE INDEX idx_invoices_original_invoice
    ON invoices(original_invoice_id);

-- ===========================
-- Invoice lines
-- ===========================

CREATE TABLE invoice_lines (
    id BINARY(16) NOT NULL,
    invoice_id BINARY(16) NOT NULL,
    line_type VARCHAR(30) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    reference_id BINARY(16) NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_invoice_lines PRIMARY KEY (id),

    CONSTRAINT fk_invoice_lines_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_invoice_lines_type
        CHECK (line_type IN ('EXAM_FEE', 'MEDICINE_FEE', 'ADJUSTMENT')),

    CONSTRAINT chk_invoice_lines_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_invoice_lines_unit_price
        CHECK (unit_price <> 0),

    CONSTRAINT chk_invoice_lines_amount
        CHECK (amount = quantity * unit_price)
);

CREATE INDEX idx_invoice_lines_invoice
    ON invoice_lines(invoice_id);

CREATE INDEX idx_invoice_lines_type
    ON invoice_lines(line_type);
