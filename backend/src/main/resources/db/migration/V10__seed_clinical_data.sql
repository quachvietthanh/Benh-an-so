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


-- =====================================================
-- V15 - Seed ICD-10 Diagnosis Catalog
-- =====================================================

-- Common ICD-10 diagnosis codes for outpatient clinic
INSERT INTO diagnosis_catalog (id, code, name, description, active, created_at, updated_at) VALUES
-- Infectious & Parasitic
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000001'), 'A09.0', 'Nhiễm trùng đường ruột', 'Nhiễm trùng đường ruột do vi khuẩn', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000002'), 'B34.9', 'Nhiễm virus không xác định', 'Nhiễm virus, không xác định vị trí', TRUE, NOW(), NULL),

-- Neoplasms
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000003'), 'C04.9', 'U ác tính miệng', 'U ác tính khoang miệng, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000004'), 'D18.0', 'U máu', 'U máu ở bất kỳ vị trí nào', TRUE, NOW(), NULL),

-- Endocrine, Nutritional & Metabolic
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000005'), 'E10.9', 'Đái tháo đường type 1', 'Đái tháo đường type 1 không có biến chứng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000006'), 'E11.9', 'Đái tháo đường type 2', 'Đái tháo đường type 2 không có biến chứng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000007'), 'E78.5', 'Mỡ máu cao', 'Tăng lipid máu, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000008'), 'E66.9', 'Béo phì', 'Béo phì, không xác định', TRUE, NOW(), NULL),

-- Mental & Behavioral
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000009'), 'F32.9', 'Trầm cảm', 'Rối loạn trầm cảm, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000a'), 'F41.9', 'Lo âu', 'Rối loạn lo âu, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000b'), 'F51.0', 'Mất ngủ', 'Mất ngủ không do thực thể', TRUE, NOW(), NULL),

-- Nervous System
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000c'), 'G43.9', 'Đau nửa đầu', 'Migraine, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000d'), 'G44.2', 'Đau đầu căng thẳng', 'Đau đầu kiểu căng thẳng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000e'), 'G47.9', 'Rối loạn giấc ngủ', 'Rối loạn giấc ngủ, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000f'), 'G56.0', 'Hội chứng ống cổ tay', 'Hội chứng ống cổ tay', TRUE, NOW(), NULL),

-- Eye & Adnexa
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000010'), 'H10.9', 'Viêm kết mạc', 'Viêm kết mạc, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000011'), 'H52.1', 'Cận thị', 'Cận thị', TRUE, NOW(), NULL),

-- Ear & Mastoid
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000012'), 'H66.9', 'Viêm tai giữa', 'Viêm tai giữa, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000013'), 'H91.9', 'Giảm thính lực', 'Giảm thính lực, không xác định', TRUE, NOW(), NULL),

-- Circulatory System
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000014'), 'I10', 'Tăng huyết áp', 'Tăng huyết áp nguyên phát', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000015'), 'I20.9', 'Đau thắt ngực', 'Đau thắt ngực, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000016'), 'I25.9', 'Bệnh mạch vành', 'Bệnh thiếu máu cơ tim mạn tính', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000017'), 'I48', 'Rung nhĩ', 'Rung nhĩ', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000018'), 'I84.9', 'Trĩ', 'Trĩ không biến chứng', TRUE, NOW(), NULL),

-- Respiratory System
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000019'), 'J00', 'Cảm lạnh thông thường', 'Viêm mũi hầu cấp tính', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001a'), 'J01.9', 'Viêm xoang', 'Viêm xoang cấp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001b'), 'J02.9', 'Viêm họng cấp', 'Viêm họng cấp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001c'), 'J03.9', 'Viêm amidan cấp', 'Viêm amidan cấp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001d'), 'J04.0', 'Viêm thanh quản cấp', 'Viêm thanh quản cấp', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001e'), 'J06.9', 'Nhiễm trùng hô hấp trên', 'Nhiễm trùng hô hấp trên cấp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001f'), 'J15.9', 'Viêm phổi', 'Viêm phổi do vi khuẩn, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000020'), 'J20.9', 'Viêm phế quản cấp', 'Viêm phế quản cấp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000021'), 'J30.4', 'Viêm mũi dị ứng', 'Viêm mũi dị ứng, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000022'), 'J32.9', 'Viêm xoang mạn', 'Viêm xoang mạn tính, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000023'), 'J45.9', 'Hen phế quản', 'Hen phế quản, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000024'), 'J47', 'Giãn phế quản', 'Giãn phế quản', TRUE, NOW(), NULL),

-- Digestive System
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000025'), 'K21.9', 'Trào ngược dạ dày', 'Bệnh trào ngược dạ dày thực quản', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000026'), 'K25.9', 'Loét dạ dày', 'Loét dạ dày, không xuất huyết', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000027'), 'K29.7', 'Viêm dạ dày', 'Viêm dạ dày, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000028'), 'K30', 'Khó tiêu', 'Rối loạn tiêu hóa chức năng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000029'), 'K52.9', 'Viêm đại tràng', 'Viêm đại tràng không nhiễm trùng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002a'), 'K59.0', 'Táo bón', 'Táo bón', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002b'), 'K59.1', 'Tiêu chảy', 'Tiêu chảy chức năng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002c'), 'K80.2', 'Sỏi mật', 'Sỏi túi mật không viêm', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002d'), 'K92.1', 'Xuất huyết tiêu hóa', 'Xuất huyết tiêu hóa, không xác định', TRUE, NOW(), NULL),

-- Skin & Subcutaneous
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002e'), 'L20.8', 'Chàm', 'Viêm da cơ địa', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002f'), 'L23.9', 'Dị ứng da', 'Viêm da tiếp xúc dị ứng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000030'), 'L30.9', 'Viêm da', 'Viêm da, không xác định', TRUE, NOW(), NULL),

-- Musculoskeletal & Connective Tissue
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000031'), 'M06.9', 'Viêm khớp dạng thấp', 'Viêm khớp dạng thấp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000032'), 'M10.9', 'Gout', 'Gout, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000033'), 'M17.9', 'Thoái hóa khớp gối', 'Thoái hóa khớp gối', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000034'), 'M19.9', 'Thoái hóa khớp', 'Thoái hóa khớp, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000035'), 'M47.9', 'Thoái hóa cột sống', 'Thoái hóa cột sống, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000036'), 'M51.1', 'Thoát vị đĩa đệm', 'Thoát vị đĩa đệm thắt lưng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000037'), 'M54.5', 'Đau thắt lưng', 'Đau thắt lưng dưới', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000038'), 'M54.1', 'Đau cổ vai gáy', 'Đau cột sống cổ', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000039'), 'M79.2', 'Đau cơ', 'Đau cơ, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003a'), 'M79.7', 'Đau nhức chân', 'Đau nhức chân', TRUE, NOW(), NULL),

-- Genitourinary System
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003b'), 'N30.9', 'Viêm bàng quang', 'Viêm bàng quang, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003c'), 'N39.0', 'Nhiễm trùng tiểu', 'Nhiễm trùng đường tiểu', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003d'), 'N40', 'Phì đại tiền liệt tuyến', 'Phì đại tiền liệt tuyến lành tính', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003e'), 'N95.1', 'Mãn kinh', 'Rối loạn mãn kinh', TRUE, NOW(), NULL),

-- Symptoms & Signs (R codes)
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003f'), 'R05', 'Ho', 'Ho', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000040'), 'R06.0', 'Khó thở', 'Khó thở', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000041'), 'R10.4', 'Đau bụng', 'Đau bụng, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000042'), 'R11', 'Buồn nôn', 'Buồn nôn và nôn', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000043'), 'R42', 'Chóng mặt', 'Chóng mặt', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000044'), 'R50.9', 'Sốt', 'Sốt, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000045'), 'R51', 'Đau đầu', 'Đau đầu', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000046'), 'R52.9', 'Đau', 'Đau, không xác định', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000047'), 'R53', 'Mệt mỏi', 'Mệt mỏi, suy nhược', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000048'), 'R55', 'Ngất', 'Ngất', TRUE, NOW(), NULL),
-- R51.9 (Headache) already seeded in V14, skipping.

-- Injury & Poisoning
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004a'), 'S06.0', 'Chấn động não', 'Chấn động não', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004b'), 'S93.4', 'Bong gân cổ chân', 'Bong gân cổ chân', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004c'), 'T14.2', 'Gãy xương', 'Gãy xương vùng không xác định', TRUE, NOW(), NULL),

-- External causes
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004d'), 'Z00.0', 'Khám sức khỏe tổng quát', 'Khám sức khỏe tổng quát', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004e'), 'Z01.0', 'Khám mắt', 'Khám mắt', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004f'), 'Z23', 'Tiêm chủng', 'Tiêm chủng vắc xin', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000050'), 'Z30.0', 'Kế hoạch hóa gia đình', 'Tư vấn và dịch vụ kế hoạch hóa gia đình', TRUE, NOW(), NULL);


-- =====================================================
-- V16 - Seed Clinical Service Catalog
-- =====================================================

INSERT INTO clinical_service_catalog (
    id, service_catalog_id, service_code, service_name, service_type, result_data_type,
    unit, reference_range, description, active, created_at, updated_at
) VALUES
-- Hematology
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000010'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000003'), 'LAB-CBC', 'Công thức máu toàn bộ', 'LAB_TEST', 'MIXED', NULL, NULL, 'Đánh giá các chỉ số hồng cầu, bạch cầu và tiểu cầu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000011'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000004'), 'LAB-HGB', 'Định lượng Hemoglobin', 'LAB_TEST', 'NUMBER', 'g/dL', 'Nam: 13.5-17.5; Nữ: 12.0-16.0', 'Đánh giá tình trạng thiếu máu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000012'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000005'), 'LAB-PLT', 'Đếm số lượng tiểu cầu', 'LAB_TEST', 'NUMBER', 'G/L', '150-450', 'Đánh giá số lượng tiểu cầu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000013'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000006'), 'LAB-ESR', 'Tốc độ máu lắng', 'LAB_TEST', 'NUMBER', 'mm/giờ', 'Nam: <15; Nữ: <20', 'Chỉ dấu viêm không đặc hiệu.', TRUE, NOW(), NULL),

-- Biochemistry
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000014'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000007'), 'LAB-HBA1C', 'HbA1c', 'LAB_TEST', 'NUMBER', '%', '<5.7', 'Theo dõi đường huyết trung bình trong 2-3 tháng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000015'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000008'), 'LAB-UREA', 'Định lượng Ure máu', 'LAB_TEST', 'NUMBER', 'mmol/L', '2.5-7.5', 'Đánh giá chức năng thận.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000016'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000009'), 'LAB-CREA', 'Định lượng Creatinin máu', 'LAB_TEST', 'NUMBER', 'µmol/L', 'Nam: 62-106; Nữ: 44-80', 'Đánh giá chức năng thận.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000017'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000010'), 'LAB-AST', 'Men gan AST', 'LAB_TEST', 'NUMBER', 'U/L', '<40', 'Đánh giá tổn thương tế bào gan.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000018'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000011'), 'LAB-ALT', 'Men gan ALT', 'LAB_TEST', 'NUMBER', 'U/L', '<41', 'Đánh giá tổn thương tế bào gan.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000019'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000012'), 'LAB-CHOL', 'Cholesterol toàn phần', 'LAB_TEST', 'NUMBER', 'mmol/L', '<5.2', 'Đánh giá rối loạn mỡ máu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000020'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000013'), 'LAB-TG', 'Triglycerid', 'LAB_TEST', 'NUMBER', 'mmol/L', '<1.7', 'Đánh giá rối loạn mỡ máu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000021'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000014'), 'LAB-LDL', 'Cholesterol LDL', 'LAB_TEST', 'NUMBER', 'mmol/L', '<3.4', 'Đánh giá nguy cơ tim mạch.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000022'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000015'), 'LAB-HDL', 'Cholesterol HDL', 'LAB_TEST', 'NUMBER', 'mmol/L', '>1.0', 'Đánh giá nguy cơ tim mạch.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000023'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000016'), 'LAB-URIC', 'Acid uric máu', 'LAB_TEST', 'NUMBER', 'µmol/L', 'Nam: 210-420; Nữ: 150-360', 'Hỗ trợ chẩn đoán bệnh gout.', TRUE, NOW(), NULL),

-- Microbiology and immunology
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000024'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000017'), 'LAB-CRP', 'Protein C phản ứng định lượng', 'LAB_TEST', 'NUMBER', 'mg/L', '<5', 'Đánh giá tình trạng viêm cấp.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000025'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000018'), 'LAB-HBSAG', 'Xét nghiệm HBsAg', 'LAB_TEST', 'TEXT', NULL, 'Âm tính', 'Sàng lọc viêm gan B.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000026'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000019'), 'LAB-ANTI-HCV', 'Xét nghiệm kháng thể viêm gan C', 'LAB_TEST', 'TEXT', NULL, 'Âm tính', 'Sàng lọc viêm gan C.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000027'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000020'), 'LAB-HIV', 'Xét nghiệm HIV', 'LAB_TEST', 'TEXT', NULL, 'Âm tính', 'Sàng lọc nhiễm HIV.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000028'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000021'), 'LAB-URINE', 'Tổng phân tích nước tiểu', 'LAB_TEST', 'MIXED', NULL, NULL, 'Đánh giá chỉ số hóa sinh và cặn lắng nước tiểu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000029'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000022'), 'LAB-STOOL', 'Xét nghiệm phân', 'LAB_TEST', 'MIXED', NULL, NULL, 'Xét nghiệm ký sinh trùng, máu ẩn và các chỉ số phân.', TRUE, NOW(), NULL),

-- Diagnostic imaging
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000030'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000023'), 'IMG-CXR', 'X-quang ngực thẳng', 'IMAGING', 'FILE', NULL, NULL, 'Chụp X-quang tim phổi tư thế thẳng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000031'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000024'), 'IMG-AXR', 'X-quang bụng không chuẩn bị', 'IMAGING', 'FILE', NULL, NULL, 'Khảo sát ổ bụng không dùng thuốc cản quang.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000032'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000025'), 'IMG-US-ABD', 'Siêu âm bụng tổng quát', 'IMAGING', 'FILE', NULL, NULL, 'Khảo sát gan mật tụy lách thận và ổ bụng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000033'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000026'), 'IMG-US-THY', 'Siêu âm tuyến giáp', 'IMAGING', 'FILE', NULL, NULL, 'Khảo sát cấu trúc và nhân tuyến giáp.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000034'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000027'), 'IMG-CT-ABD', 'CT scan bụng', 'IMAGING', 'FILE', NULL, NULL, 'Chụp cắt lớp vi tính ổ bụng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000035'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000028'), 'IMG-MRI-BRAIN', 'MRI sọ não', 'IMAGING', 'FILE', NULL, NULL, 'Chụp cộng hưởng từ sọ não.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000036'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000029'), 'IMG-MAMMO', 'Chụp X-quang tuyến vú', 'IMAGING', 'FILE', NULL, NULL, 'Sàng lọc và chẩn đoán bệnh lý tuyến vú.', TRUE, NOW(), NULL),

-- Functional investigations
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000037'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000030'), 'OTH-ECG', 'Điện tâm đồ', 'OTHER', 'FILE', NULL, NULL, 'Ghi nhận hoạt động điện học của tim.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000038'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000031'), 'OTH-ECHO', 'Siêu âm tim Doppler', 'OTHER', 'FILE', NULL, NULL, 'Đánh giá cấu trúc và chức năng tim.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000039'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000032'), 'OTH-SPIRO', 'Đo chức năng hô hấp', 'OTHER', 'MIXED', NULL, NULL, 'Đánh giá thông khí phổi bằng hô hấp ký.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000040'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000033'), 'OTH-ENDO-GI', 'Nội soi dạ dày tá tràng', 'OTHER', 'FILE', NULL, NULL, 'Nội soi khảo sát thực quản, dạ dày và tá tràng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000041'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000034'), 'OTH-ENDO-COLON', 'Nội soi đại tràng', 'OTHER', 'FILE', NULL, NULL, 'Nội soi khảo sát đại tràng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000042'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000035'), 'OTH-HOLTER', 'Holter điện tâm đồ 24 giờ', 'OTHER', 'FILE', NULL, NULL, 'Theo dõi điện tâm đồ liên tục trong 24 giờ.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000043'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000036'), 'OTH-ABPM', 'Đo huyết áp lưu động 24 giờ', 'OTHER', 'MIXED', 'mmHg', NULL, 'Theo dõi huyết áp liên tục trong 24 giờ.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000044'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000037'), 'OTH-DEXA', 'Đo mật độ xương DEXA', 'OTHER', 'FILE', NULL, NULL, 'Đánh giá mật độ khoáng của xương.', TRUE, NOW(), NULL);
