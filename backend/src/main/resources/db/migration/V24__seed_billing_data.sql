-- =====================================================
-- V24__seed_billing_data.sql
-- Seed payment and invoice data for billing flows
-- Depends on V12 medical record/visit seed data and
-- V17 prescription data.
-- =====================================================

-- ===========================
-- Recorded payment for completed visit VIS000001
-- ===========================

INSERT INTO payments (
    id, visit_id, exam_fee, medicine_fee, total_amount, amount_paid,
    payment_method, status, collected_by, paid_at, created_at
) VALUES (
    UUID_TO_BIN('23000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    100000.00,
    150000.00,
    250000.00,
    250000.00,
    'CASH',
    'RECORDED',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-20 03:20:00',
    '2026-08-20 03:20:00'
);

-- ===========================
-- Original invoice after payment
-- ===========================

INSERT INTO invoices (
    id, invoice_code, visit_id, payment_id, invoice_type,
    original_invoice_id, adjustment_reason, total_amount, created_by, created_at
) VALUES (
    UUID_TO_BIN('23100000-0000-0000-0000-000000000001'),
    'HD000001',
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('23000000-0000-0000-0000-000000000001'),
    'ORIGINAL',
    NULL,
    NULL,
    250000.00,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-20 03:25:00'
);

INSERT INTO invoice_lines (
    id, invoice_id, line_type, item_name, reference_id,
    quantity, unit_price, amount, created_at
) VALUES
(
    UUID_TO_BIN('23200000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('23100000-0000-0000-0000-000000000001'),
    'EXAM_FEE',
    'Phi kham',
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    1,
    100000.00,
    100000.00,
    '2026-08-20 03:25:00'
),
(
    UUID_TO_BIN('23200000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('23100000-0000-0000-0000-000000000001'),
    'MEDICINE_FEE',
    'Tien thuoc',
    UUID_TO_BIN('16200000-0000-0000-0000-000000000002'),
    1,
    150000.00,
    150000.00,
    '2026-08-20 03:25:00'
);

-- ===========================
-- Adjustment invoice linked to original invoice
-- ===========================

INSERT INTO invoices (
    id, invoice_code, visit_id, payment_id, invoice_type,
    original_invoice_id, adjustment_reason, total_amount, created_by, created_at
) VALUES (
    UUID_TO_BIN('23100000-0000-0000-0000-000000000002'),
    'HDDC000001',
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    NULL,
    'ADJUSTMENT',
    UUID_TO_BIN('23100000-0000-0000-0000-000000000001'),
    'Giam tien thuoc do dieu chinh khoan thu da ghi thua.',
    -20000.00,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
    '2026-08-20 03:40:00'
);

INSERT INTO invoice_lines (
    id, invoice_id, line_type, item_name, reference_id,
    quantity, unit_price, amount, created_at
) VALUES (
    UUID_TO_BIN('23200000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('23100000-0000-0000-0000-000000000002'),
    'ADJUSTMENT',
    'Dieu chinh giam tien thuoc',
    UUID_TO_BIN('23100000-0000-0000-0000-000000000001'),
    1,
    -20000.00,
    -20000.00,
    '2026-08-20 03:40:00'
);

-- ===========================
-- Invoice code sequences
-- ===========================

INSERT INTO invoice_code_sequences (code_prefix, `last_value`) VALUES
('HD', 1),
('HDDC', 1);
