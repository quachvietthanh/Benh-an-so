-- =====================================================
-- V77__seed_sample_contraindication_rules.sql
-- NCL-05-CN-006: Nạp dữ liệu 9 quy tắc chống chỉ định mẫu chuẩn lâm sàng
-- Áp dụng theo độ tuổi, thai kỳ và bệnh nền (ICD-10)
-- Đối chiếu chính xác medicine_id từ V12 và diagnosis_catalog_id từ V10
-- =====================================================

INSERT INTO contraindication_rules (
    id, medicine_id, active_ingredient, contraindication_type,
    min_age_years, max_age_years, diagnosis_catalog_id, severity,
    message, recommendation, active, created_at, updated_at
) VALUES
-- 1. Ibuprofen 400 mg - Thai kỳ (Chống chỉ định tuyệt đối)
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000002'),
    'Ibuprofen',
    'PREGNANCY',
    NULL,
    NULL,
    NULL,
    'CONTRAINDICATED',
    'Chống chỉ định tuyệt đối cho phụ nữ có thai (nguy cơ đóng sớm ống động mạch thai nhi và suy thận sơ sinh).',
    'Thay thế bằng Paracetamol với liều thấp nhất có hiệu quả lâm sàng.',
    TRUE,
    NOW(),
    NULL
),
-- 2. Ibuprofen 400 mg - Hen phế quản (ICD-10: J45.9)
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000002'),
    'Ibuprofen',
    'DISEASE',
    NULL,
    NULL,
    UUID_TO_BIN('a1000000-0000-0000-0000-000000000023'),
    'SEVERE',
    'Chống chỉ định cho bệnh nhân có tiền sử hen phế quản do nguy cơ kích phát cơn co thắt phế quản cấp tính nặng.',
    'Cân nhắc Paracetamol hoặc thuốc giảm đau không thuộc nhóm NSAID.',
    TRUE,
    NOW(),
    NULL
),
-- 3. Ibuprofen 400 mg - Loét dạ dày (ICD-10: K25.9)
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000002'),
    'Ibuprofen',
    'DISEASE',
    NULL,
    NULL,
    UUID_TO_BIN('a1000000-0000-0000-0000-000000000026'),
    'SEVERE',
    'Chống chỉ định cho bệnh nhân có tiền sử loét dạ dày do nguy cơ tái phát loét và xuất huyết tiêu hóa.',
    'Chọn thuốc thay thế an toàn cho đường tiêu hóa hoặc dùng kèm PPI nếu bắt buộc.',
    TRUE,
    NOW(),
    NULL
),
-- 4. Aspirin 81 mg - Trẻ em dưới 16 tuổi (Hội chứng Reye)
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000030'),
    'Aspirin',
    'AGE',
    16,
    NULL,
    NULL,
    'SEVERE',
    'Chống chỉ định cho trẻ em và thanh thiếu niên dưới 16 tuổi do nguy cơ mắc hội chứng Reye gây tổn thương gan não cấp tính.',
    'Sử dụng Paracetamol thay thế cho mục đích hạ sốt và giảm đau.',
    TRUE,
    NOW(),
    NULL
),
-- 5. Aspirin 81 mg - Thai kỳ
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000005'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000030'),
    'Aspirin',
    'PREGNANCY',
    NULL,
    NULL,
    NULL,
    'CONTRAINDICATED',
    'Chống chỉ định liều giảm đau cho phụ nữ mang thai do nguy cơ xuất huyết và biến chứng chuyển dạ.',
    'Sử dụng Paracetamol liều chuẩn nếu cần hạ sốt, giảm đau.',
    TRUE,
    NOW(),
    NULL
),
-- 6. Ciprofloxacin 500 mg - Dưới 18 tuổi
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000006'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000041'),
    'Ciprofloxacin',
    'AGE',
    18,
    NULL,
    NULL,
    'MODERATE',
    'Thận trọng và hạn chế dùng cho người dưới 18 tuổi do nguy cơ tổn thương và thoái hóa sụn khớp chịu lực.',
    'Cân nhắc nhóm kháng sinh Beta-lactam (Amoxicillin, Cefuroxime) thay thế nếu phù hợp.',
    TRUE,
    NOW(),
    NULL
),
-- 7. Ciprofloxacin 500 mg - Thai kỳ
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000007'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000041'),
    'Ciprofloxacin',
    'PREGNANCY',
    NULL,
    NULL,
    NULL,
    'CONTRAINDICATED',
    'Chống chỉ định cho phụ nữ mang thai vì nguy cơ gây tổn thương thoái hóa khớp ở thai nhi.',
    'Sử dụng kháng sinh an toàn trong thai kỳ như Amoxicillin hoặc Cefuroxime.',
    TRUE,
    NOW(),
    NULL
),
-- 8. Warfarin 2 mg - Thai kỳ
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000008'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000029'),
    'Warfarin',
    'PREGNANCY',
    NULL,
    NULL,
    NULL,
    'CONTRAINDICATED',
    'Chống chỉ định tuyệt đối cho phụ nữ mang thai do gây dị tật thai nhi nghiêm trọng và xuất huyết tử vong.',
    'Chuyển sang Heparin trọng lượng phân tử thấp (LMWH) theo phác đồ chuyên khoa tim mạch.',
    TRUE,
    NOW(),
    NULL
),
-- 9. Diclofenac 50 mg - Loét dạ dày (ICD-10: K25.9)
(
    UUID_TO_BIN('c0100000-0000-0000-0000-000000000009'),
    UUID_TO_BIN('16000000-0000-0000-0000-000000000021'),
    'Diclofenac',
    'DISEASE',
    NULL,
    NULL,
    UUID_TO_BIN('a1000000-0000-0000-0000-000000000026'),
    'CONTRAINDICATED',
    'Chống chỉ định tuyệt đối cho bệnh nhân có tiền sử hoặc đang bị loét dạ dày tá tràng tiến triển.',
    'Tránh sử dụng NSAID toàn thân; chuyển hướng điều trị giảm đau thay thế.',
    TRUE,
    NOW(),
    NULL
);
