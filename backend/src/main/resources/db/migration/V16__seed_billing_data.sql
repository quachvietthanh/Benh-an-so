-- =====================================================
-- V26__seed_billing_data.sql
-- Seed payment and invoice data for billing flows
-- Depends on V14 medical record/visit seed data,
-- V19 prescription data and V22 inventory seed data.
-- Current seed keeps the historical sample payment in RECORDED
-- status; V25 schema already supports SUCCESS/REFUNDED/CANCELLED
-- for follow-up stories such as refund/cancel receipt.
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
-- Completed payable visit without payment/invoice
-- For manual billing flow testing:
-- payable -> record payment -> create invoice -> adjustment
-- ===========================

INSERT INTO visits (
    id, visit_code, patient_id, doctor_id, appointment_id, queue_item_id,
    visit_type, status, visit_at, started_at, completed_at,
    reason, note, created_by, created_at, updated_at
) VALUES (
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000006'),
    'VIS000006',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb005'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    NULL,
    NULL,
    'WALK_IN',
    'COMPLETED',
    '2026-08-11 08:00:00',
    '2026-08-11 08:10:00',
    '2026-08-11 08:40:00',
    'Dau hong va sot nhe',
    'Ca seed de test billing payable, chua co payment va invoice.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-11 08:00:00',
    '2026-08-11 08:40:00'
);

INSERT INTO medical_records (
    id, visit_id, chief_complaint, symptoms, medical_history,
    physical_examination, clinical_progress, treatment_plan, doctor_instructions,
    conclusion, status, locked_at, locked_by, created_by, created_at, updated_by, updated_at
) VALUES (
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000006'),
    'Dau hong 2 ngay',
    'Sot nhe, ho it, khong kho tho',
    'Khong co benh nen dang ke',
    'Hong do nhe, phoi thong khi tot',
    'Trieu chung on dinh sau tham kham',
    'Dieu tri trieu chung va theo doi tai nha',
    'Uong nhieu nuoc am, tai kham neu sot cao hon',
    'Viem hong cap muc do nhe',
    'LOCKED',
    '2026-08-11 08:40:00',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-11 08:15:00',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-11 08:40:00'
);

INSERT INTO medical_record_diagnoses (
    id, medical_record_id, diagnosis_catalog_id, diagnosis_code, diagnosis_name,
    diagnosis_type, note, diagnosed_by, diagnosed_at, created_at, updated_at
) VALUES (
    UUID_TO_BIN('e4000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('a1000000-0000-0000-0000-00000000001b'),
    'J02.9',
    'Viem hong cap',
    'PRIMARY',
    'Seed cho luong billing co medical record va don thuoc da cap phat.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-11 08:20:00',
    '2026-08-11 08:20:00',
    NULL
);

INSERT INTO prescriptions (
    id, prescription_code, medical_record_id, status, note,
    prescribed_by, prescribed_at, updated_by, updated_at
) VALUES (
    UUID_TO_BIN('16200000-0000-0000-0000-000000000007'),
    'RX000007',
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000002'),
    'DISPENSED',
    'Don seed cho test thanh toan billing.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-11 08:25:00',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-11 08:30:00'
);

INSERT INTO prescription_items (
    id, prescription_id, medicine_id, medicine_name, active_ingredient,
    strength, unit, dosage, frequency, route, duration_days, quantity,
    instructions, created_at, updated_at
) VALUES (
    UUID_TO_BIN('16300000-0000-0000-0000-000000000013'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    'Paracetamol 500 mg',
    'Paracetamol',
    '500 mg',
    'vien',
    '1 vien',
    'Moi 8 gio khi sot',
    'ORAL',
    2,
    6,
    'Uong sau an, toi da 3 vien moi ngay.',
    '2026-08-11 08:25:00',
    NULL
);

INSERT INTO prescription_dispense_items (
    id, prescription_id, prescription_item_id, medicine_id, medicine_batch_id,
    dispensed_quantity, dispensed_by, dispensed_at, created_at
) VALUES (
    UUID_TO_BIN('18300000-0000-0000-0000-000000000006'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('16300000-0000-0000-0000-000000000013'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    6,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-11 08:30:00',
    '2026-08-11 08:30:00'
);

UPDATE medicines
SET stock_quantity = 85
WHERE id = UUID_TO_BIN('16000000-0000-0000-0000-000000000001');

UPDATE medicine_batches
SET quantity = 85,
    updated_at = '2026-08-11 08:30:00'
WHERE id = UUID_TO_BIN('18000000-0000-0000-0000-000000000001');

INSERT INTO stock_movements (
    id, medicine_id, medicine_batch_id, movement_type, reference_type, reference_id,
    quantity_change, quantity_before, quantity_after, performed_by, performed_at, note, created_at
) VALUES (
    UUID_TO_BIN('18400000-0000-0000-0000-000000000020'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    'DISPENSE',
    'PRESCRIPTION_ITEM',
    UUID_TO_BIN('16300000-0000-0000-0000-000000000013'),
    -6,
    91,
    85,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-11 08:30:00',
    'Dispensed for seeded billing prescription item 16300000-0000-0000-0000-000000000013',
    '2026-08-11 08:30:00'
);

UPDATE prescription_code_sequences
SET `last_value` = 7
WHERE code_prefix = 'RX' AND `last_value` < 7;

-- ===========================
-- Invoice code sequences
-- ===========================

INSERT INTO invoice_code_sequences (code_prefix, `last_value`) VALUES
('HD', 1),
('HDDC', 1);
