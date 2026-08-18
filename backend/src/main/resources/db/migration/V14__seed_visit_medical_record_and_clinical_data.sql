-- =====================================================
-- V14 - Seed Visit, Medical Record and Clinical Data
-- =====================================================

-- Patient BN000001 has two visits for medical-history API testing.
INSERT INTO visits (
    id, visit_code, patient_id, doctor_id, appointment_id, queue_item_id,
    visit_type, status, visit_at, started_at, completed_at,
    reason, note, created_by, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    'VIS000001',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    UUID_TO_BIN('cccccccc-cccc-cccc-cccc-ccccccccc001'),
    NULL,
    'APPOINTMENT', 'COMPLETED',
    '2026-08-20 02:00:00', '2026-08-20 02:10:00', '2026-08-20 03:00:00',
    'Kham dau dau', 'Tai kham neu trieu chung tai phat.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:00:00', '2026-08-20 03:00:00'
),
(
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000002'),
    'VIS000002',
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'),
    NULL,
    NULL,
    'FOLLOW_UP', 'WAITING',
    '2026-09-15 02:00:00', NULL, NULL,
    'Kham tai phat', NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3'),
    '2026-09-15 02:00:00', NULL
);

INSERT INTO medical_records (
    id, visit_id, chief_complaint, symptoms, medical_history,
    physical_examination, clinical_progress, treatment_plan, doctor_instructions,
    conclusion, status, locked_at, locked_by, created_by, created_at, updated_by, updated_at
) VALUES (
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    'Dau dau 2 ngay', 'Dau dau nhe, khong sot', 'Khong co benh nen dang ke',
    'Huyet ap 120/80 mmHg', 'On dinh sau dieu tri trieu chung',
    'Nghi ngoi va theo doi', 'Tai kham neu dau dau tang',
    'Dau dau co nang, da on dinh', 'LOCKED',
    '2026-08-20 03:00:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:15:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 03:00:00'
);

INSERT INTO medical_record_amendments (
    id, medical_record_id, content, reason, amended_by, amended_at
) VALUES (
    UUID_TO_BIN('e1000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'),
    'Bo sung huong dan theo doi tai nha.', 'Lam ro huong dan sau kham',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 03:05:00'
);

INSERT INTO medical_record_access_logs (
    id, patient_id, visit_id, medical_record_id, accessed_by, action, detail, ip_address, accessed_at
) VALUES
(
    UUID_TO_BIN('e2000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    'VIEW', 'Medical record reviewed', NULL, '2026-08-20 03:10:00'
),
(
    UUID_TO_BIN('e2000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
    NULL, NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1'),
    'VIEW_HISTORY', 'Seeded history access', NULL, '2026-08-21 01:00:00'
);

INSERT INTO diagnosis_catalog (
    id, code, name, description, active, created_at, updated_at
) VALUES (
    UUID_TO_BIN('e3000000-0000-0000-0000-000000000001'),
    'R51.9', 'Headache', 'Headache, unspecified', TRUE,
    '2026-08-20 02:20:00', NULL
);

INSERT INTO medical_record_diagnoses (
    id, medical_record_id, diagnosis_catalog_id, diagnosis_code, diagnosis_name,
    diagnosis_type, note, diagnosed_by, diagnosed_at, created_at, updated_at
) VALUES (
    UUID_TO_BIN('e4000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('e3000000-0000-0000-0000-000000000001'),
    'R51.9', 'Headache', 'PRIMARY', 'Theo doi tai kham neu tai phat.',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:25:00', '2026-08-20 02:25:00', NULL
);

INSERT INTO clinical_service_catalog (
    id, service_catalog_id, service_code, service_name, service_type, result_data_type,
    unit, reference_range, description, active, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('f0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('c1000000-0000-0000-0000-000000000001'),
    'LAB-GLU', 'Blood glucose', 'LAB_TEST', 'NUMBER',
    'mmol/L', '3.9-5.5', 'Fasting blood glucose', TRUE,
    '2026-08-20 02:20:00', NULL
),
(
    UUID_TO_BIN('f0000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('c1000000-0000-0000-0000-000000000002'),
    'IMG-CTH', 'Head CT scan', 'IMAGING', 'TEXT',
    NULL, NULL, 'Non-contrast head CT', TRUE,
    '2026-08-20 02:20:00', NULL
);

INSERT INTO clinical_orders (
    id, order_code, visit_id, medical_record_id, patient_id, ordered_by,
    clinical_reason, status, ordered_at, completed_at, created_at, updated_at
) VALUES (
    UUID_TO_BIN('f1000000-0000-0000-0000-000000000001'),
    'ORD000001',
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    'Danh gia nguyen nhan dau dau', 'COMPLETED',
    '2026-08-20 02:20:00', '2026-08-20 02:50:00',
    '2026-08-20 02:20:00', '2026-08-20 02:50:00'
);

INSERT INTO clinical_order_items (
    id, clinical_order_id, clinical_service_id, service_code, service_name,
    instruction, status, created_at, updated_at
) VALUES
(
    UUID_TO_BIN('f2000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('f1000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('f0000000-0000-0000-0000-000000000001'),
    'LAB-GLU', 'Blood glucose', 'Lay mau luc doi', 'COMPLETED',
    '2026-08-20 02:20:00', '2026-08-20 02:35:00'
),
(
    UUID_TO_BIN('f2000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('f1000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('f0000000-0000-0000-0000-000000000002'),
    'IMG-CTH', 'Head CT scan', 'Khong can tiem can quang', 'COMPLETED',
    '2026-08-20 02:20:00', '2026-08-20 02:45:00'
);

INSERT INTO clinical_results (
    id, clinical_order_item_id, visit_id, result_type, numeric_value, text_value,
    unit, reference_range, abnormal_flag, conclusion, status,
    entered_by, entered_at, updated_by, updated_at
) VALUES
(
    UUID_TO_BIN('f3000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('f2000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    'NUMBER', 4.8000, NULL, 'mmol/L', '3.9-5.5', 'NORMAL',
    'Blood glucose within reference range', 'FINAL',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:35:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:36:00'
),
(
    UUID_TO_BIN('f3000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('f2000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    'TEXT', NULL, 'No acute intracranial abnormality', NULL, NULL, 'NORMAL',
    'Head CT result is normal', 'FINAL',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:45:00', NULL, NULL
);

INSERT INTO clinical_result_histories (
    id, clinical_result_id,
    old_result_type, new_result_type, old_numeric_value, new_numeric_value,
    old_text_value, new_text_value, old_unit, new_unit,
    old_reference_range, new_reference_range, old_abnormal_flag, new_abnormal_flag,
    old_conclusion, new_conclusion, old_status, new_status,
    change_reason, changed_by, changed_at
) VALUES (
    UUID_TO_BIN('f4000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('f3000000-0000-0000-0000-000000000001'),
    'NUMBER', 'NUMBER', 4.8000, 4.8000,
    NULL, NULL, 'mmol/L', 'mmol/L',
    '3.9-5.5', '3.9-5.5', 'NORMAL', 'NORMAL',
    'Blood glucose within reference range', 'Blood glucose within reference range',
    'DRAFT', 'FINAL', 'Result reviewed and finalized',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:36:00'
);

INSERT INTO medical_attachments (
    id, visit_id, medical_record_id, clinical_result_id,
    file_name, original_file_name, storage_key, content_type, file_size,
    checksum, attachment_type, uploaded_by, uploaded_at
) VALUES
(
    UUID_TO_BIN('f5000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), NULL,
    'visit-summary.pdf', 'visit-summary.pdf', 'seed/medical-records/visit-summary.pdf',
    'application/pdf', 2048, NULL, 'MEDICAL_RECORD',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 03:00:00'
),
(
    UUID_TO_BIN('f5000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('d0000000-0000-0000-0000-000000000001'),
    NULL, UUID_TO_BIN('f3000000-0000-0000-0000-000000000002'),
    'head-ct-report.pdf', 'head-ct-report.pdf', 'seed/clinical-results/head-ct-report.pdf',
    'application/pdf', 4096, NULL, 'IMAGING_RESULT',
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:50:00'
);
