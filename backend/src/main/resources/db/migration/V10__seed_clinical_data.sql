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
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000001'), 'A09.0', 'Nhiá»…m trÃ¹ng Ä‘Æ°á»ng ruá»™t', 'Nhiá»…m trÃ¹ng Ä‘Æ°á»ng ruá»™t do vi khuáº©n', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000002'), 'B34.9', 'Nhiá»…m virus khÃ´ng xÃ¡c Ä‘á»‹nh', 'Nhiá»…m virus, khÃ´ng xÃ¡c Ä‘á»‹nh vá»‹ trÃ­', TRUE, NOW(), NULL),

-- Neoplasms
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000003'), 'C04.9', 'U Ã¡c tÃ­nh miá»‡ng', 'U Ã¡c tÃ­nh khoang miá»‡ng, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000004'), 'D18.0', 'U mÃ¡u', 'U mÃ¡u á»Ÿ báº¥t ká»³ vá»‹ trÃ­ nÃ o', TRUE, NOW(), NULL),

-- Endocrine, Nutritional & Metabolic
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000005'), 'E10.9', 'ÄÃ¡i thÃ¡o Ä‘Æ°á»ng type 1', 'ÄÃ¡i thÃ¡o Ä‘Æ°á»ng type 1 khÃ´ng cÃ³ biáº¿n chá»©ng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000006'), 'E11.9', 'ÄÃ¡i thÃ¡o Ä‘Æ°á»ng type 2', 'ÄÃ¡i thÃ¡o Ä‘Æ°á»ng type 2 khÃ´ng cÃ³ biáº¿n chá»©ng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000007'), 'E78.5', 'Má»¡ mÃ¡u cao', 'TÄƒng lipid mÃ¡u, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000008'), 'E66.9', 'BÃ©o phÃ¬', 'BÃ©o phÃ¬, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),

-- Mental & Behavioral
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000009'), 'F32.9', 'Tráº§m cáº£m', 'Rá»‘i loáº¡n tráº§m cáº£m, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000a'), 'F41.9', 'Lo Ã¢u', 'Rá»‘i loáº¡n lo Ã¢u, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000b'), 'F51.0', 'Máº¥t ngá»§', 'Máº¥t ngá»§ khÃ´ng do thá»±c thá»ƒ', TRUE, NOW(), NULL),

-- Nervous System
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000c'), 'G43.9', 'Äau ná»­a Ä‘áº§u', 'Migraine, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000d'), 'G44.2', 'Äau Ä‘áº§u cÄƒng tháº³ng', 'Äau Ä‘áº§u kiá»ƒu cÄƒng tháº³ng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000e'), 'G47.9', 'Rá»‘i loáº¡n giáº¥c ngá»§', 'Rá»‘i loáº¡n giáº¥c ngá»§, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000000f'), 'G56.0', 'Há»™i chá»©ng á»‘ng cá»• tay', 'Há»™i chá»©ng á»‘ng cá»• tay', TRUE, NOW(), NULL),

-- Eye & Adnexa
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000010'), 'H10.9', 'ViÃªm káº¿t máº¡c', 'ViÃªm káº¿t máº¡c, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000011'), 'H52.1', 'Cáº­n thá»‹', 'Cáº­n thá»‹', TRUE, NOW(), NULL),

-- Ear & Mastoid
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000012'), 'H66.9', 'ViÃªm tai giá»¯a', 'ViÃªm tai giá»¯a, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000013'), 'H91.9', 'Giáº£m thÃ­nh lá»±c', 'Giáº£m thÃ­nh lá»±c, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),

-- Circulatory System
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000014'), 'I10', 'TÄƒng huyáº¿t Ã¡p', 'TÄƒng huyáº¿t Ã¡p nguyÃªn phÃ¡t', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000015'), 'I20.9', 'Äau tháº¯t ngá»±c', 'Äau tháº¯t ngá»±c, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000016'), 'I25.9', 'Bá»‡nh máº¡ch vÃ nh', 'Bá»‡nh thiáº¿u mÃ¡u cÆ¡ tim máº¡n tÃ­nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000017'), 'I48', 'Rung nhÄ©', 'Rung nhÄ©', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000018'), 'I84.9', 'TrÄ©', 'TrÄ© khÃ´ng biáº¿n chá»©ng', TRUE, NOW(), NULL),

-- Respiratory System
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000019'), 'J00', 'Cáº£m láº¡nh thÃ´ng thÆ°á»ng', 'ViÃªm mÅ©i háº§u cáº¥p tÃ­nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001a'), 'J01.9', 'ViÃªm xoang', 'ViÃªm xoang cáº¥p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001b'), 'J02.9', 'ViÃªm há»ng cáº¥p', 'ViÃªm há»ng cáº¥p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001c'), 'J03.9', 'ViÃªm amidan cáº¥p', 'ViÃªm amidan cáº¥p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001d'), 'J04.0', 'ViÃªm thanh quáº£n cáº¥p', 'ViÃªm thanh quáº£n cáº¥p', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001e'), 'J06.9', 'Nhiá»…m trÃ¹ng hÃ´ háº¥p trÃªn', 'Nhiá»…m trÃ¹ng hÃ´ háº¥p trÃªn cáº¥p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000001f'), 'J15.9', 'ViÃªm phá»•i', 'ViÃªm phá»•i do vi khuáº©n, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000020'), 'J20.9', 'ViÃªm pháº¿ quáº£n cáº¥p', 'ViÃªm pháº¿ quáº£n cáº¥p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000021'), 'J30.4', 'ViÃªm mÅ©i dá»‹ á»©ng', 'ViÃªm mÅ©i dá»‹ á»©ng, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000022'), 'J32.9', 'ViÃªm xoang máº¡n', 'ViÃªm xoang máº¡n tÃ­nh, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000023'), 'J45.9', 'Hen pháº¿ quáº£n', 'Hen pháº¿ quáº£n, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000024'), 'J47', 'GiÃ£n pháº¿ quáº£n', 'GiÃ£n pháº¿ quáº£n', TRUE, NOW(), NULL),

-- Digestive System
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000025'), 'K21.9', 'TrÃ o ngÆ°á»£c dáº¡ dÃ y', 'Bá»‡nh trÃ o ngÆ°á»£c dáº¡ dÃ y thá»±c quáº£n', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000026'), 'K25.9', 'LoÃ©t dáº¡ dÃ y', 'LoÃ©t dáº¡ dÃ y, khÃ´ng xuáº¥t huyáº¿t', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000027'), 'K29.7', 'ViÃªm dáº¡ dÃ y', 'ViÃªm dáº¡ dÃ y, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000028'), 'K30', 'KhÃ³ tiÃªu', 'Rá»‘i loáº¡n tiÃªu hÃ³a chá»©c nÄƒng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000029'), 'K52.9', 'ViÃªm Ä‘áº¡i trÃ ng', 'ViÃªm Ä‘áº¡i trÃ ng khÃ´ng nhiá»…m trÃ¹ng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002a'), 'K59.0', 'TÃ¡o bÃ³n', 'TÃ¡o bÃ³n', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002b'), 'K59.1', 'TiÃªu cháº£y', 'TiÃªu cháº£y chá»©c nÄƒng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002c'), 'K80.2', 'Sá»i máº­t', 'Sá»i tÃºi máº­t khÃ´ng viÃªm', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002d'), 'K92.1', 'Xuáº¥t huyáº¿t tiÃªu hÃ³a', 'Xuáº¥t huyáº¿t tiÃªu hÃ³a, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),

-- Skin & Subcutaneous
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002e'), 'L20.8', 'ChÃ m', 'ViÃªm da cÆ¡ Ä‘á»‹a', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000002f'), 'L23.9', 'Dá»‹ á»©ng da', 'ViÃªm da tiáº¿p xÃºc dá»‹ á»©ng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000030'), 'L30.9', 'ViÃªm da', 'ViÃªm da, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),

-- Musculoskeletal & Connective Tissue
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000031'), 'M06.9', 'ViÃªm khá»›p dáº¡ng tháº¥p', 'ViÃªm khá»›p dáº¡ng tháº¥p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000032'), 'M10.9', 'Gout', 'Gout, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000033'), 'M17.9', 'ThoÃ¡i hÃ³a khá»›p gá»‘i', 'ThoÃ¡i hÃ³a khá»›p gá»‘i', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000034'), 'M19.9', 'ThoÃ¡i hÃ³a khá»›p', 'ThoÃ¡i hÃ³a khá»›p, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000035'), 'M47.9', 'ThoÃ¡i hÃ³a cá»™t sá»‘ng', 'ThoÃ¡i hÃ³a cá»™t sá»‘ng, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000036'), 'M51.1', 'ThoÃ¡t vá»‹ Ä‘Ä©a Ä‘á»‡m', 'ThoÃ¡t vá»‹ Ä‘Ä©a Ä‘á»‡m tháº¯t lÆ°ng', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000037'), 'M54.5', 'Äau tháº¯t lÆ°ng', 'Äau tháº¯t lÆ°ng dÆ°á»›i', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000038'), 'M54.1', 'Äau cá»• vai gÃ¡y', 'Äau cá»™t sá»‘ng cá»•', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000039'), 'M79.2', 'Äau cÆ¡', 'Äau cÆ¡, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003a'), 'M79.7', 'Äau nhá»©c chÃ¢n', 'Äau nhá»©c chÃ¢n', TRUE, NOW(), NULL),

-- Genitourinary System
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003b'), 'N30.9', 'ViÃªm bÃ ng quang', 'ViÃªm bÃ ng quang, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003c'), 'N39.0', 'Nhiá»…m trÃ¹ng tiá»ƒu', 'Nhiá»…m trÃ¹ng Ä‘Æ°á»ng tiá»ƒu', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003d'), 'N40', 'PhÃ¬ Ä‘áº¡i tiá»n liá»‡t tuyáº¿n', 'PhÃ¬ Ä‘áº¡i tiá»n liá»‡t tuyáº¿n lÃ nh tÃ­nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003e'), 'N95.1', 'MÃ£n kinh', 'Rá»‘i loáº¡n mÃ£n kinh', TRUE, NOW(), NULL),

-- Symptoms & Signs (R codes)
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000003f'), 'R05', 'Ho', 'Ho', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000040'), 'R06.0', 'KhÃ³ thá»Ÿ', 'KhÃ³ thá»Ÿ', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000041'), 'R10.4', 'Äau bá»¥ng', 'Äau bá»¥ng, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000042'), 'R11', 'Buá»“n nÃ´n', 'Buá»“n nÃ´n vÃ  nÃ´n', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000043'), 'R42', 'ChÃ³ng máº·t', 'ChÃ³ng máº·t', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000044'), 'R50.9', 'Sá»‘t', 'Sá»‘t, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000045'), 'R51', 'Äau Ä‘áº§u', 'Äau Ä‘áº§u', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000046'), 'R52.9', 'Äau', 'Äau, khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000047'), 'R53', 'Má»‡t má»i', 'Má»‡t má»i, suy nhÆ°á»£c', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000048'), 'R55', 'Ngáº¥t', 'Ngáº¥t', TRUE, NOW(), NULL),
-- R51.9 (Headache) already seeded in V14, skipping.

-- Injury & Poisoning
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004a'), 'S06.0', 'Cháº¥n Ä‘á»™ng nÃ£o', 'Cháº¥n Ä‘á»™ng nÃ£o', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004b'), 'S93.4', 'Bong gÃ¢n cá»• chÃ¢n', 'Bong gÃ¢n cá»• chÃ¢n', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004c'), 'T14.2', 'GÃ£y xÆ°Æ¡ng', 'GÃ£y xÆ°Æ¡ng vÃ¹ng khÃ´ng xÃ¡c Ä‘á»‹nh', TRUE, NOW(), NULL),

-- External causes
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004d'), 'Z00.0', 'KhÃ¡m sá»©c khá»e tá»•ng quÃ¡t', 'KhÃ¡m sá»©c khá»e tá»•ng quÃ¡t', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004e'), 'Z01.0', 'KhÃ¡m máº¯t', 'KhÃ¡m máº¯t', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004f'), 'Z23', 'TiÃªm chá»§ng', 'TiÃªm chá»§ng váº¯c xin', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000050'), 'Z30.0', 'Káº¿ hoáº¡ch hÃ³a gia Ä‘Ã¬nh', 'TÆ° váº¥n vÃ  dá»‹ch vá»¥ káº¿ hoáº¡ch hÃ³a gia Ä‘Ã¬nh', TRUE, NOW(), NULL);


-- =====================================================
-- V16 - Seed Clinical Service Catalog
-- =====================================================

INSERT INTO clinical_service_catalog (
    id, service_catalog_id, service_code, service_name, service_type, result_data_type,
    unit, reference_range, description, active, created_at, updated_at
) VALUES
-- Hematology
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000010'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000003'), 'LAB-CBC', 'CÃ´ng thá»©c mÃ¡u toÃ n bá»™', 'LAB_TEST', 'MIXED', NULL, NULL, 'ÄÃ¡nh giÃ¡ cÃ¡c chá»‰ sá»‘ há»“ng cáº§u, báº¡ch cáº§u vÃ  tiá»ƒu cáº§u.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000011'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000004'), 'LAB-HGB', 'Äá»‹nh lÆ°á»£ng Hemoglobin', 'LAB_TEST', 'NUMBER', 'g/dL', 'Nam: 13.5-17.5; Ná»¯: 12.0-16.0', 'ÄÃ¡nh giÃ¡ tÃ¬nh tráº¡ng thiáº¿u mÃ¡u.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000012'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000005'), 'LAB-PLT', 'Äáº¿m sá»‘ lÆ°á»£ng tiá»ƒu cáº§u', 'LAB_TEST', 'NUMBER', 'G/L', '150-450', 'ÄÃ¡nh giÃ¡ sá»‘ lÆ°á»£ng tiá»ƒu cáº§u.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000013'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000006'), 'LAB-ESR', 'Tá»‘c Ä‘á»™ mÃ¡u láº¯ng', 'LAB_TEST', 'NUMBER', 'mm/giá»', 'Nam: <15; Ná»¯: <20', 'Chá»‰ dáº¥u viÃªm khÃ´ng Ä‘áº·c hiá»‡u.', TRUE, NOW(), NULL),

-- Biochemistry
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000014'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000007'), 'LAB-HBA1C', 'HbA1c', 'LAB_TEST', 'NUMBER', '%', '<5.7', 'Theo dÃµi Ä‘Æ°á»ng huyáº¿t trung bÃ¬nh trong 2-3 thÃ¡ng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000015'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000008'), 'LAB-UREA', 'Äá»‹nh lÆ°á»£ng Ure mÃ¡u', 'LAB_TEST', 'NUMBER', 'mmol/L', '2.5-7.5', 'ÄÃ¡nh giÃ¡ chá»©c nÄƒng tháº­n.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000016'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000009'), 'LAB-CREA', 'Äá»‹nh lÆ°á»£ng Creatinin mÃ¡u', 'LAB_TEST', 'NUMBER', 'Âµmol/L', 'Nam: 62-106; Ná»¯: 44-80', 'ÄÃ¡nh giÃ¡ chá»©c nÄƒng tháº­n.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000017'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000010'), 'LAB-AST', 'Men gan AST', 'LAB_TEST', 'NUMBER', 'U/L', '<40', 'ÄÃ¡nh giÃ¡ tá»•n thÆ°Æ¡ng táº¿ bÃ o gan.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000018'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000011'), 'LAB-ALT', 'Men gan ALT', 'LAB_TEST', 'NUMBER', 'U/L', '<41', 'ÄÃ¡nh giÃ¡ tá»•n thÆ°Æ¡ng táº¿ bÃ o gan.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000019'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000012'), 'LAB-CHOL', 'Cholesterol toÃ n pháº§n', 'LAB_TEST', 'NUMBER', 'mmol/L', '<5.2', 'ÄÃ¡nh giÃ¡ rá»‘i loáº¡n má»¡ mÃ¡u.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000020'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000013'), 'LAB-TG', 'Triglycerid', 'LAB_TEST', 'NUMBER', 'mmol/L', '<1.7', 'ÄÃ¡nh giÃ¡ rá»‘i loáº¡n má»¡ mÃ¡u.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000021'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000014'), 'LAB-LDL', 'Cholesterol LDL', 'LAB_TEST', 'NUMBER', 'mmol/L', '<3.4', 'ÄÃ¡nh giÃ¡ nguy cÆ¡ tim máº¡ch.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000022'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000015'), 'LAB-HDL', 'Cholesterol HDL', 'LAB_TEST', 'NUMBER', 'mmol/L', '>1.0', 'ÄÃ¡nh giÃ¡ nguy cÆ¡ tim máº¡ch.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000023'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000016'), 'LAB-URIC', 'Acid uric mÃ¡u', 'LAB_TEST', 'NUMBER', 'Âµmol/L', 'Nam: 210-420; Ná»¯: 150-360', 'Há»— trá»£ cháº©n Ä‘oÃ¡n bá»‡nh gout.', TRUE, NOW(), NULL),

-- Microbiology and immunology
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000024'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000017'), 'LAB-CRP', 'Protein C pháº£n á»©ng Ä‘á»‹nh lÆ°á»£ng', 'LAB_TEST', 'NUMBER', 'mg/L', '<5', 'ÄÃ¡nh giÃ¡ tÃ¬nh tráº¡ng viÃªm cáº¥p.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000025'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000018'), 'LAB-HBSAG', 'XÃ©t nghiá»‡m HBsAg', 'LAB_TEST', 'TEXT', NULL, 'Ã‚m tÃ­nh', 'SÃ ng lá»c viÃªm gan B.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000026'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000019'), 'LAB-ANTI-HCV', 'XÃ©t nghiá»‡m khÃ¡ng thá»ƒ viÃªm gan C', 'LAB_TEST', 'TEXT', NULL, 'Ã‚m tÃ­nh', 'SÃ ng lá»c viÃªm gan C.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000027'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000020'), 'LAB-HIV', 'XÃ©t nghiá»‡m HIV', 'LAB_TEST', 'TEXT', NULL, 'Ã‚m tÃ­nh', 'SÃ ng lá»c nhiá»…m HIV.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000028'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000021'), 'LAB-URINE', 'Tá»•ng phÃ¢n tÃ­ch nÆ°á»›c tiá»ƒu', 'LAB_TEST', 'MIXED', NULL, NULL, 'ÄÃ¡nh giÃ¡ chá»‰ sá»‘ hÃ³a sinh vÃ  cáº·n láº¯ng nÆ°á»›c tiá»ƒu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000029'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000022'), 'LAB-STOOL', 'XÃ©t nghiá»‡m phÃ¢n', 'LAB_TEST', 'MIXED', NULL, NULL, 'XÃ©t nghiá»‡m kÃ½ sinh trÃ¹ng, mÃ¡u áº©n vÃ  cÃ¡c chá»‰ sá»‘ phÃ¢n.', TRUE, NOW(), NULL),

-- Diagnostic imaging
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000030'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000023'), 'IMG-CXR', 'X-quang ngá»±c tháº³ng', 'IMAGING', 'FILE', NULL, NULL, 'Chá»¥p X-quang tim phá»•i tÆ° tháº¿ tháº³ng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000031'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000024'), 'IMG-AXR', 'X-quang bá»¥ng khÃ´ng chuáº©n bá»‹', 'IMAGING', 'FILE', NULL, NULL, 'Kháº£o sÃ¡t á»• bá»¥ng khÃ´ng dÃ¹ng thuá»‘c cáº£n quang.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000032'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000025'), 'IMG-US-ABD', 'SiÃªu Ã¢m bá»¥ng tá»•ng quÃ¡t', 'IMAGING', 'FILE', NULL, NULL, 'Kháº£o sÃ¡t gan máº­t tá»¥y lÃ¡ch tháº­n vÃ  á»• bá»¥ng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000033'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000026'), 'IMG-US-THY', 'SiÃªu Ã¢m tuyáº¿n giÃ¡p', 'IMAGING', 'FILE', NULL, NULL, 'Kháº£o sÃ¡t cáº¥u trÃºc vÃ  nhÃ¢n tuyáº¿n giÃ¡p.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000034'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000027'), 'IMG-CT-ABD', 'CT scan bá»¥ng', 'IMAGING', 'FILE', NULL, NULL, 'Chá»¥p cáº¯t lá»›p vi tÃ­nh á»• bá»¥ng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000035'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000028'), 'IMG-MRI-BRAIN', 'MRI sá» nÃ£o', 'IMAGING', 'FILE', NULL, NULL, 'Chá»¥p cá»™ng hÆ°á»Ÿng tá»« sá» nÃ£o.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000036'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000029'), 'IMG-MAMMO', 'Chá»¥p X-quang tuyáº¿n vÃº', 'IMAGING', 'FILE', NULL, NULL, 'SÃ ng lá»c vÃ  cháº©n Ä‘oÃ¡n bá»‡nh lÃ½ tuyáº¿n vÃº.', TRUE, NOW(), NULL),

-- Functional investigations
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000037'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000030'), 'OTH-ECG', 'Äiá»‡n tÃ¢m Ä‘á»“', 'OTHER', 'FILE', NULL, NULL, 'Ghi nháº­n hoáº¡t Ä‘á»™ng Ä‘iá»‡n há»c cá»§a tim.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000038'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000031'), 'OTH-ECHO', 'SiÃªu Ã¢m tim Doppler', 'OTHER', 'FILE', NULL, NULL, 'ÄÃ¡nh giÃ¡ cáº¥u trÃºc vÃ  chá»©c nÄƒng tim.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000039'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000032'), 'OTH-SPIRO', 'Äo chá»©c nÄƒng hÃ´ háº¥p', 'OTHER', 'MIXED', NULL, NULL, 'ÄÃ¡nh giÃ¡ thÃ´ng khÃ­ phá»•i báº±ng hÃ´ háº¥p kÃ½.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000040'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000033'), 'OTH-ENDO-GI', 'Ná»™i soi dáº¡ dÃ y tÃ¡ trÃ ng', 'OTHER', 'FILE', NULL, NULL, 'Ná»™i soi kháº£o sÃ¡t thá»±c quáº£n, dáº¡ dÃ y vÃ  tÃ¡ trÃ ng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000041'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000034'), 'OTH-ENDO-COLON', 'Ná»™i soi Ä‘áº¡i trÃ ng', 'OTHER', 'FILE', NULL, NULL, 'Ná»™i soi kháº£o sÃ¡t Ä‘áº¡i trÃ ng.', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000042'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000035'), 'OTH-HOLTER', 'Holter Ä‘iá»‡n tÃ¢m Ä‘á»“ 24 giá»', 'OTHER', 'FILE', NULL, NULL, 'Theo dÃµi Ä‘iá»‡n tÃ¢m Ä‘á»“ liÃªn tá»¥c trong 24 giá».', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000043'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000036'), 'OTH-ABPM', 'Äo huyáº¿t Ã¡p lÆ°u Ä‘á»™ng 24 giá»', 'OTHER', 'MIXED', 'mmHg', NULL, 'Theo dÃµi huyáº¿t Ã¡p liÃªn tá»¥c trong 24 giá».', TRUE, NOW(), NULL),
(UUID_TO_BIN('f0000000-0000-0000-0000-000000000044'), UUID_TO_BIN('c1000000-0000-0000-0000-000000000037'), 'OTH-DEXA', 'Äo máº­t Ä‘á»™ xÆ°Æ¡ng DEXA', 'OTHER', 'FILE', NULL, NULL, 'ÄÃ¡nh giÃ¡ máº­t Ä‘á»™ khoÃ¡ng cá»§a xÆ°Æ¡ng.', TRUE, NOW(), NULL);
