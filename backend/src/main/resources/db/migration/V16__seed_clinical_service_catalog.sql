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
