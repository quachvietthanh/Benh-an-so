-- =====================================================
-- V84__create_discount_requests_and_adjust_billing_constraints.sql
-- NCL-07-CN-008: Giảm giá và miễn phí có phê duyệt
-- QTN-37: Giảm giá phải được phê duyệt trước khi lập hóa đơn
-- =====================================================

-- 1. Create table discount_requests
CREATE TABLE discount_requests (
    id BINARY(16) NOT NULL,
    visit_id BINARY(16) NOT NULL,
    discount_type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(15, 2) NOT NULL,
    original_amount DECIMAL(15, 2) NOT NULL,
    discount_amount DECIMAL(15, 2) NOT NULL,
    final_amount DECIMAL(15, 2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    active_status VARCHAR(20) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'APPROVED') THEN 'ACTIVE' ELSE NULL END
    ) STORED,
    requested_by BINARY(16) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    approved_by BINARY(16) NULL,
    approved_at TIMESTAMP NULL,
    rejected_by BINARY(16) NULL,
    rejection_reason TEXT NULL,
    rejected_at TIMESTAMP NULL,
    invoice_id BINARY(16) NULL,

    CONSTRAINT pk_discount_requests PRIMARY KEY (id),

    CONSTRAINT uk_discount_requests_active_visit
        UNIQUE (visit_id, active_status),

    CONSTRAINT fk_discount_requests_visit
        FOREIGN KEY (visit_id)
        REFERENCES visits(id),

    CONSTRAINT fk_discount_requests_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES users(id),

    CONSTRAINT fk_discount_requests_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES users(id),

    CONSTRAINT fk_discount_requests_rejected_by
        FOREIGN KEY (rejected_by)
        REFERENCES users(id),

    CONSTRAINT chk_discount_requests_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FULL_FREE')),

    CONSTRAINT chk_discount_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),

    CONSTRAINT chk_discount_requests_value
        CHECK (discount_value >= 0),

    CONSTRAINT chk_discount_requests_amounts
        CHECK (original_amount >= 0 AND discount_amount >= 0 AND final_amount >= 0),

    CONSTRAINT chk_discount_requests_calculation
        CHECK (final_amount = original_amount - discount_amount)
);

CREATE INDEX idx_discount_requests_visit_status
    ON discount_requests(visit_id, status);

CREATE INDEX idx_discount_requests_requested_by
    ON discount_requests(requested_by);

CREATE INDEX idx_discount_requests_approved_by
    ON discount_requests(approved_by);

CREATE INDEX idx_discount_requests_invoice_id
    ON discount_requests(invoice_id);


-- 2. Adjust payments table for discount support
ALTER TABLE payments
    ADD COLUMN discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD COLUMN discount_request_id BINARY(16) NULL;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_discount_request
        FOREIGN KEY (discount_request_id)
        REFERENCES discount_requests(id);

ALTER TABLE payments
    DROP CHECK chk_payments_amount_match;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_amount_match
        CHECK (amount_paid = total_amount - discount_amount);

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_discount_amount
        CHECK (discount_amount >= 0);


-- 3. Adjust invoices table for discount support and free (zero total) support
ALTER TABLE invoices
    ADD COLUMN discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0;

ALTER TABLE invoices
    ADD COLUMN discount_request_id BINARY(16) NULL;

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_discount_request
        FOREIGN KEY (discount_request_id)
        REFERENCES discount_requests(id);

ALTER TABLE invoices
    DROP CHECK chk_invoices_original_shape;

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_original_shape
        CHECK (
            (
                invoice_type = 'ORIGINAL'
                AND payment_id IS NOT NULL
                AND original_invoice_id IS NULL
                AND adjustment_reason IS NULL
                AND total_amount >= 0
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
        );

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_discount_amount
        CHECK (discount_amount >= 0);


-- 4. Adjust invoice_lines table for DISCOUNT line_type
ALTER TABLE invoice_lines
    DROP CHECK chk_invoice_lines_type;

ALTER TABLE invoice_lines
    ADD CONSTRAINT chk_invoice_lines_type
        CHECK (line_type IN ('EXAM_FEE', 'MEDICINE_FEE', 'SERVICE_FEE', 'ADJUSTMENT', 'DISCOUNT'));
