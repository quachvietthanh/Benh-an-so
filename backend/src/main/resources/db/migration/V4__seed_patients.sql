-- =====================================================
-- V4__seed_patients.sql
-- Seed Patient Data & Change Logs (Consolidated)
-- Includes: Portal User linking, diverse Consent states, and audit change logs
-- =====================================================

INSERT INTO patients (
    id, patient_code, full_name, date_of_birth, gender, phone, email, address,
    identity_number, insurance_number, blood_type, emergency_contact, emergency_phone,
    active, user_id, created_by, created_at, updated_at,
    consent_agreed, consent_agreed_at, consent_version, consent_withdrawn, consent_withdrawn_at, consent_withdrawn_reason, non_medical_use_restricted
) VALUES
-- BN000001: Nguyen Van An (Linked to patient1, Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), 'BN000001', 'Nguyen Van An', '1988-01-15', 'MALE', '0910000001', 'an@example.com', 'Ha Noi', '001000000001', 'BH000001', 'O_POSITIVE', 'Nguyen Thi Hoa', '0911000001', TRUE, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000002: Tran Thi Binh (Linked to patient2, Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002'), 'BN000002', 'Tran Thi Binh', '1992-02-20', 'FEMALE', '0910000002', 'binh@example.com', 'Ha Noi', '001000000002', 'BH000002', 'A_POSITIVE', 'Tran Van Long', '0911000002', TRUE, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000003: Le Minh Chau (Pending consent)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'), 'BN000003', 'Le Minh Chau', '1979-03-12', 'MALE', '0910000003', 'chau@example.com', 'Da Nang', '001000000003', 'BH000003', 'B_POSITIVE', 'Le Thi Mai', '0911000003', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, NULL, NULL, FALSE, NULL, NULL, FALSE),

-- BN000004: Pham Ngoc Diep (Withdrawn consent - Demo US-68)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004'), 'BN000004', 'Pham Ngoc Diep', '2000-04-08', 'FEMALE', '0910000004', 'diep@example.com', 'Hue', '001000000004', 'BH000004', 'AB_POSITIVE', 'Pham Van Duc', '0911000004', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY), 'v1.0', TRUE, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY), 'Yêu cầu rút lại sự đồng ý chia sẻ thông tin phi y tế theo Nghị định 13', TRUE),

-- BN000005: Hoang Gia Duc (Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb005'), 'BN000005', 'Hoang Gia Duc', '1985-05-23', 'MALE', '0910000005', 'duc@example.com', 'Hai Phong', '001000000005', 'BH000005', 'O_NEGATIVE', 'Hoang Thi Lan', '0911000005', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000006: Vu Thanh Giang (Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb006'), 'BN000006', 'Vu Thanh Giang', '1996-06-18', 'FEMALE', '0910000006', 'giang@example.com', 'Ha Noi', '001000000006', 'BH000006', 'A_NEGATIVE', 'Vu Van Hiep', '0911000006', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000007: Do Quang Huy (Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb007'), 'BN000007', 'Do Quang Huy', '1975-07-29', 'MALE', '0910000007', 'huy@example.com', 'Can Tho', '001000000007', 'BH000007', 'B_NEGATIVE', 'Do Thi Nga', '0911000007', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000008: Bui Thu Khanh (Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb008'), 'BN000008', 'Bui Thu Khanh', '1999-08-14', 'FEMALE', '0910000008', 'khanh@example.com', 'Nha Trang', '001000000008', 'BH000008', 'AB_NEGATIVE', 'Bui Van Nam', '0911000008', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000009: Nguyen Tuan Long (Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb009'), 'BN000009', 'Nguyen Tuan Long', '1982-09-30', 'MALE', '0910000009', 'long@example.com', 'Vinh', '001000000009', 'BH000009', 'UNKNOWN', 'Nguyen Thi Yen', '0911000009', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE),

-- BN000010: Dang My Linh (Consented)
(UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb010'), 'BN000010', 'Dang My Linh', '1994-10-05', 'FEMALE', '0910000010', 'linh@example.com', 'Ho Chi Minh City', '001000000010', 'BH000010', 'O_POSITIVE', 'Dang Van Son', '0911000010', TRUE, NULL, UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE, CURRENT_TIMESTAMP, 'v1.0', FALSE, NULL, NULL, FALSE);

-- Seed Patient Change Logs (Demo US-07: Cập nhật thông tin hành chính có Audit)
INSERT INTO patient_change_logs (id, patient_id, changed_by, action, change_detail, created_at)
VALUES
(UUID_TO_BIN('88888888-8888-8888-8888-888888888001'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'UPDATE', '{"field": "phone", "oldValue": "0910999999", "newValue": "0910000001", "reason": "Bệnh nhân cập nhật số điện thoại chính thức"}', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 5 DAY)),
(UUID_TO_BIN('88888888-8888-8888-8888-888888888002'), UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb003'), UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5'), 'UPDATE', '{"field": "address", "oldValue": "Quang Nam", "newValue": "Da Nang", "reason": "Bệnh nhân chuyển hộ khẩu thường trú"}', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 DAY));
