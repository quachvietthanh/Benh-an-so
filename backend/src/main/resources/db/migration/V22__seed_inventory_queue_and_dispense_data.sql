-- =====================================================
-- V22 - Seed inventory, queue and dispense history data
-- Completes missing runtime data so Postman/manual API
-- testing can exercise successful and failing flows.
-- =====================================================

-- ===========================
-- Same-day appointments for queue testing
-- ===========================

INSERT INTO appointments (
    id, appointment_code, patient_id, doctor_id, start_time, end_time, status,
    reason, cancel_reason, checked_in_at, completed_at, created_by, created_at
) VALUES
(
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc007'),
    'LH000007',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-07 08:30:00',
    '2026-08-07 09:00:00',
    'CHECKED_IN',
    'Kham noi khoa trong ngay',
    NULL,
    '2026-08-07 08:15:00',
    NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 08:00:00'
),
(
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc008'),
    'LH000008',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'),
    '2026-08-07 10:00:00',
    '2026-08-07 10:30:00',
    'IN_PROGRESS',
    'Kham co chi dinh can lam sang',
    NULL,
    '2026-08-07 10:00:00',
    NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 09:40:00'
);

-- ===========================
-- Same-day queues and queue items
-- ===========================

INSERT INTO medical_queues (
    id, doctor_id, room_id, queue_date, status, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('92000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    UUID_TO_BIN('90000000-0000-0000-0000-000000000001'),
    '2026-08-07',
    'OPEN',
    '2026-08-07 07:30:00',
    '2026-08-07 09:15:00'
),
(
    UUID_TO_BIN('92000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'),
    UUID_TO_BIN('90000000-0000-0000-0000-000000000002'),
    '2026-08-07',
    'OPEN',
    '2026-08-07 07:45:00',
    '2026-08-07 10:05:00'
);

INSERT INTO visits (
    id, visit_code, patient_id, doctor_id, appointment_id, queue_item_id,
    visit_type, status, visit_at, started_at, completed_at,
    reason, note, created_by, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000003'),
    'VIS000003',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc007'),
    NULL,
    'APPOINTMENT',
    'WAITING',
    '2026-08-07 08:30:00',
    NULL,
    NULL,
    'Kham noi khoa trong ngay',
    'Benh nhan da check-in va dang cho kham.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 08:15:00',
    '2026-08-07 08:15:00'
),
(
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000004'),
    'VIS000004',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    NULL,
    NULL,
    'WALK_IN',
    'IN_PROGRESS',
    '2026-08-07 09:00:00',
    '2026-08-07 09:15:00',
    NULL,
    'Dau bung cap',
    'Benh nhan walk-in dang duoc kham.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 09:00:00',
    '2026-08-07 09:15:00'
),
(
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000005'),
    'VIS000005',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'),
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc008'),
    NULL,
    'APPOINTMENT',
    'WAITING_FOR_RESULT',
    '2026-08-07 10:00:00',
    '2026-08-07 10:05:00',
    NULL,
    'Cho ket qua can lam sang',
    'Da vao kham va dang cho ket qua.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 10:00:00',
    '2026-08-07 10:05:00'
);

INSERT INTO queue_items (
    id, medical_queue_id, patient_id, appointment_id, visit_id, source_type, status,
    queue_number, queue_date, checked_in_at, called_at, completed_at, cancelled_at,
    skipped_at, cancel_reason, skip_reason, created_by, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('93000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('92000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'),
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc007'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000003'),
    'APPOINTMENT',
    'WAITING',
    1,
    '2026-08-07',
    '2026-08-07 08:15:00',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 08:15:00',
    '2026-08-07 08:15:00'
),
(
    UUID_TO_BIN('93000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('92000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'),
    NULL,
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000004'),
    'WALK_IN',
    'IN_PROGRESS',
    2,
    '2026-08-07',
    '2026-08-07 09:00:00',
    '2026-08-07 09:15:00',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 09:00:00',
    '2026-08-07 09:15:00'
),
(
    UUID_TO_BIN('93000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('92000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004'),
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc008'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000005'),
    'APPOINTMENT',
    'WAITING_FOR_RESULT',
    1,
    '2026-08-07',
    '2026-08-07 10:00:00',
    '2026-08-07 10:05:00',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'),
    '2026-08-07 10:00:00',
    '2026-08-07 10:05:00'
);

UPDATE visits
SET queue_item_id = UUID_TO_BIN('93000000-0000-0000-0000-000000000001')
WHERE id = UUID_TO_BIN('d0000000-0000-0000-0000-000000000003');

UPDATE visits
SET queue_item_id = UUID_TO_BIN('93000000-0000-0000-0000-000000000002')
WHERE id = UUID_TO_BIN('d0000000-0000-0000-0000-000000000004');

UPDATE visits
SET queue_item_id = UUID_TO_BIN('93000000-0000-0000-0000-000000000003')
WHERE id = UUID_TO_BIN('d0000000-0000-0000-0000-000000000005');

-- ===========================
-- Inventory stock, batches and receipts
-- ===========================

UPDATE medicines
SET stock_quantity = CASE medicine_code
    WHEN 'MED-PARA-500' THEN 91
    WHEN 'MED-AMOX-500' THEN 65
    WHEN 'MED-IBU-400' THEN 20
    WHEN 'MED-OMEP-20' THEN 43
    WHEN 'MED-AMBRO-30' THEN 45
    WHEN 'MED-METFO-500' THEN 115
    WHEN 'MED-GLIC-30' THEN 40
    WHEN 'MED-AMLO-5' THEN 20
    WHEN 'MED-LOSAR-50' THEN 30
    WHEN 'MED-ATOR-20' THEN 30
    WHEN 'MED-WARF-2' THEN 20
    ELSE stock_quantity
END
WHERE medicine_code IN (
    'MED-PARA-500',
    'MED-AMOX-500',
    'MED-IBU-400',
    'MED-OMEP-20',
    'MED-AMBRO-30',
    'MED-METFO-500',
    'MED-GLIC-30',
    'MED-AMLO-5',
    'MED-LOSAR-50',
    'MED-ATOR-20',
    'MED-WARF-2'
);

INSERT INTO medicine_batches (
    id, medicine_id, batch_number, expiry_date, quantity, status, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    'PARA-2027-01',
    '2027-03-31',
    91,
    'ACTIVE',
    '2026-08-02 08:00:00',
    '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000007'),
    'OMEP-2027-01',
    '2027-05-31',
    43,
    'ACTIVE',
    '2026-08-02 08:00:00',
    '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    'AMOX-2026-09',
    '2026-09-30',
    0,
    'DEPLETED',
    '2026-08-02 08:10:00',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    'AMOX-2027-02',
    '2027-02-28',
    65,
    'ACTIVE',
    '2026-08-02 08:10:00',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000005'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000012'),
    'AMBRO-2027-01',
    '2027-01-31',
    45,
    'ACTIVE',
    '2026-08-02 08:15:00',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000006'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    'METF-2026-10',
    '2026-10-15',
    40,
    'ACTIVE',
    '2026-08-03 08:00:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    'METF-2027-01',
    '2027-01-15',
    50,
    'ACTIVE',
    '2026-08-03 08:05:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000008'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    'METF-2026-07',
    '2026-07-31',
    25,
    'EXPIRED',
    '2026-06-15 08:00:00',
    '2026-08-01 00:00:00'
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-000000000009'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000017'),
    'GLIC-2027-02',
    '2027-02-28',
    40,
    'ACTIVE',
    '2026-08-03 08:10:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000a'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000002'),
    'IBU-2027-03',
    '2027-03-31',
    20,
    'ACTIVE',
    '2026-08-03 08:15:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000b'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000029'),
    'WARF-2027-04',
    '2027-04-30',
    20,
    'ACTIVE',
    '2026-08-03 08:20:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000c'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000019'),
    'AMLO-2027-01',
    '2027-01-31',
    20,
    'ACTIVE',
    '2026-08-03 08:25:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000d'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000020'),
    'LOSAR-2027-02',
    '2027-02-28',
    30,
    'ACTIVE',
    '2026-08-03 08:30:00',
    NULL
),
(
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000e'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000018'),
    'ATOR-2027-03',
    '2027-03-31',
    30,
    'ACTIVE',
    '2026-08-03 08:35:00',
    NULL
);

INSERT INTO inventory_receipts (
    id, received_by, received_at, note, created_at
) VALUES
(
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-02 08:00:00',
    'Nhap kho ban dau cho cac don mau RX000001-RX000006.',
    '2026-08-02 08:00:00'
),
(
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:00:00',
    'Bo sung thuoc cho don chua cap phat va test inventory API.',
    '2026-08-03 08:00:00'
),
(
    UUID_TO_BIN('18100000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
    '2026-06-15 08:00:00',
    'Lo thuoc Metformin da het han de test filter eligibleForDispense.',
    '2026-06-15 08:00:00'
);

INSERT INTO inventory_receipt_items (
    id, inventory_receipt_id, medicine_id, medicine_batch_id, quantity,
    import_price, total_value, created_at
) VALUES
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    100,
    1200.00,
    120000.00,
    '2026-08-02 08:00:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000002'),
    50,
    1800.00,
    90000.00,
    '2026-08-02 08:00:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000003'),
    10,
    2300.00,
    23000.00,
    '2026-08-02 08:10:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000004'),
    70,
    2250.00,
    157500.00,
    '2026-08-02 08:10:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000005'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000012'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000005'),
    60,
    1600.00,
    96000.00,
    '2026-08-02 08:15:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000006'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000006'),
    40,
    900.00,
    36000.00,
    '2026-08-03 08:00:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000007'),
    50,
    920.00,
    46000.00,
    '2026-08-03 08:05:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000008'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000017'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000009'),
    40,
    1500.00,
    60000.00,
    '2026-08-03 08:10:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-000000000009'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000a'),
    20,
    1400.00,
    28000.00,
    '2026-08-03 08:15:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-00000000000a'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000029'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000b'),
    20,
    3500.00,
    70000.00,
    '2026-08-03 08:20:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-00000000000b'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000019'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000c'),
    20,
    1100.00,
    22000.00,
    '2026-08-03 08:25:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-00000000000c'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000020'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000d'),
    30,
    1300.00,
    39000.00,
    '2026-08-03 08:30:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-00000000000d'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000018'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000e'),
    30,
    2000.00,
    60000.00,
    '2026-08-03 08:35:00'
),
(
    UUID_TO_BIN('18200000-0000-0000-0000-00000000000e'),
    UUID_TO_BIN('18100000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000008'),
    25,
    850.00,
    21250.00,
    '2026-06-15 08:00:00'
);

-- ===========================
-- Historical dispense allocation data
-- ===========================

INSERT INTO prescription_dispense_items (
    id, prescription_id, prescription_item_id, medicine_id, medicine_batch_id,
    dispensed_quantity, dispensed_by, dispensed_at, created_at
) VALUES
(
    UUID_TO_BIN('18300000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16300000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    9,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:35:00',
    '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('18300000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16300000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000002'),
    7,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:35:00',
    '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('18300000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16300000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000003'),
    10,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:40:00',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18300000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16300000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000004'),
    5,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:40:00',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18300000-0000-0000-0000-000000000005'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16300000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000012'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000005'),
    15,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:40:00',
    '2026-08-20 02:40:00'
);

INSERT INTO stock_movements (
    id, medicine_id, medicine_batch_id, movement_type, reference_type, reference_id,
    quantity_change, quantity_before, quantity_after, performed_by, performed_at, note, created_at
) VALUES
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    100,
    0,
    100,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-02 08:00:00',
    'Nhap kho Paracetamol 500 mg.',
    '2026-08-02 08:00:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000002'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    50,
    0,
    50,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-02 08:00:00',
    'Nhap kho Omeprazole 20 mg.',
    '2026-08-02 08:00:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000003'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    10,
    0,
    10,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-02 08:10:00',
    'Nhap kho lo Amoxicillin som het han de test FEFO.',
    '2026-08-02 08:10:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000004'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    70,
    0,
    70,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-02 08:10:00',
    'Nhap kho lo Amoxicillin bo sung.',
    '2026-08-02 08:10:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000005'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000012'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000005'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000001'),
    60,
    0,
    60,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-02 08:15:00',
    'Nhap kho Ambroxol 30 mg.',
    '2026-08-02 08:15:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000006'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000006'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    40,
    0,
    40,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:00:00',
    'Nhap kho Metformin lo 1.',
    '2026-08-03 08:00:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000007'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    50,
    0,
    50,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:05:00',
    'Nhap kho Metformin lo 2.',
    '2026-08-03 08:05:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000008'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000016'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000008'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000003'),
    25,
    0,
    25,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
    '2026-06-15 08:00:00',
    'Nhap kho lo Metformin da het han de test inventory filter.',
    '2026-06-15 08:00:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000009'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000017'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000009'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    40,
    0,
    40,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:10:00',
    'Nhap kho Gliclazide MR 30 mg.',
    '2026-08-03 08:10:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-00000000000a'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000a'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    20,
    0,
    20,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:15:00',
    'Nhap kho Ibuprofen 400 mg.',
    '2026-08-03 08:15:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-00000000000b'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000029'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000b'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    20,
    0,
    20,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:20:00',
    'Nhap kho Warfarin 2 mg.',
    '2026-08-03 08:20:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-00000000000c'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000019'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000c'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    20,
    0,
    20,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:25:00',
    'Nhap kho Amlodipine 5 mg.',
    '2026-08-03 08:25:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-00000000000d'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000020'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000d'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    30,
    0,
    30,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:30:00',
    'Nhap kho Losartan 50 mg.',
    '2026-08-03 08:30:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-00000000000e'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000018'),
    UUID_TO_BIN('18000000-0000-0000-0000-00000000000e'),
    'RECEIPT',
    'INVENTORY_RECEIPT',
    UUID_TO_BIN('18100000-0000-0000-0000-000000000002'),
    30,
    0,
    30,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-03 08:35:00',
    'Nhap kho Atorvastatin 20 mg.',
    '2026-08-03 08:35:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-00000000000f'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000001'),
    'DISPENSE',
    'PRESCRIPTION_ITEM',
    UUID_TO_BIN('16300000-0000-0000-0000-000000000001'),
    -9,
    100,
    91,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:35:00',
    'Dispensed for prescription item 16300000-0000-0000-0000-000000000001',
    '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000010'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000002'),
    'DISPENSE',
    'PRESCRIPTION_ITEM',
    UUID_TO_BIN('16300000-0000-0000-0000-000000000002'),
    -7,
    50,
    43,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:35:00',
    'Dispensed for prescription item 16300000-0000-0000-0000-000000000002',
    '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000011'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000003'),
    'DISPENSE',
    'PRESCRIPTION_ITEM',
    UUID_TO_BIN('16300000-0000-0000-0000-000000000003'),
    -10,
    10,
    0,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:40:00',
    'Dispensed for prescription item 16300000-0000-0000-0000-000000000003',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000012'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000004'),
    'DISPENSE',
    'PRESCRIPTION_ITEM',
    UUID_TO_BIN('16300000-0000-0000-0000-000000000003'),
    -5,
    70,
    65,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:40:00',
    'Dispensed for prescription item 16300000-0000-0000-0000-000000000003',
    '2026-08-20 02:40:00'
),
(
    UUID_TO_BIN('18400000-0000-0000-0000-000000000013'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000012'),
    UUID_TO_BIN('18000000-0000-0000-0000-000000000005'),
    'DISPENSE',
    'PRESCRIPTION_ITEM',
    UUID_TO_BIN('16300000-0000-0000-0000-000000000004'),
    -15,
    60,
    45,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'),
    '2026-08-20 02:40:00',
    'Dispensed for prescription item 16300000-0000-0000-0000-000000000004',
    '2026-08-20 02:40:00'
);
