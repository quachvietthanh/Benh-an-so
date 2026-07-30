-- =====================================================
-- V13 - Seed ICD-10 Diagnosis Catalog
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
-- R51.9 (Headache) already seeded in V12, skipping.

-- Injury & Poisoning
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004a'), 'S06.0', 'Chấn động não', 'Chấn động não', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004b'), 'S93.4', 'Bong gân cổ chân', 'Bong gân cổ chân', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004c'), 'T14.2', 'Gãy xương', 'Gãy xương vùng không xác định', TRUE, NOW(), NULL),

-- External causes
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004d'), 'Z00.0', 'Khám sức khỏe tổng quát', 'Khám sức khỏe tổng quát', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004e'), 'Z01.0', 'Khám mắt', 'Khám mắt', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-00000000004f'), 'Z23', 'Tiêm chủng', 'Tiêm chủng vắc xin', TRUE, NOW(), NULL),
(UUID_TO_BIN('a1000000-0000-0000-0000-000000000050'), 'Z30.0', 'Kế hoạch hóa gia đình', 'Tư vấn và dịch vụ kế hoạch hóa gia đình', TRUE, NOW(), NULL);
