ALTER TABLE payments
    ADD COLUMN service_fee DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER medicine_fee;

ALTER TABLE payments
    DROP CHECK chk_payments_total_formula;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_service_fee
        CHECK (service_fee >= 0),
    ADD CONSTRAINT chk_payments_total_formula
        CHECK (total_amount = exam_fee + medicine_fee + service_fee);

ALTER TABLE invoice_lines
    DROP CHECK chk_invoice_lines_type;

ALTER TABLE invoice_lines
    ADD CONSTRAINT chk_invoice_lines_type
        CHECK (line_type IN ('EXAM_FEE', 'MEDICINE_FEE', 'SERVICE_FEE', 'ADJUSTMENT'));
