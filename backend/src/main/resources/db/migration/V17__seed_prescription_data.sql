-- =====================================================
-- V17 - Seed medicine, interaction and prescription data
-- Requires V3 users and V12 medical record seed data.
-- =====================================================

-- ===========================
-- Medicine catalog
-- ===========================

INSERT INTO medicines (
    id, medicine_code, medicine_name, active_ingredient, strength,
    dosage_form, unit, default_route, active, created_at, updated_at
) VALUES
(UUID_TO_BIN('16000000-0000-0000-0000-000000000001'), 'MED-PARA-500', 'Paracetamol 500 mg', 'Paracetamol', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), 'MED-IBU-400', 'Ibuprofen 400 mg', 'Ibuprofen', '400 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000003'), 'MED-AMOX-500', 'Amoxicillin 500 mg', 'Amoxicillin', '500 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000004'), 'MED-AMCL-625', 'Amoxicillin Clavulanate 625 mg', 'Amoxicillin + Clavulanic acid', '625 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000005'), 'MED-AZI-500', 'Azithromycin 500 mg', 'Azithromycin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000006'), 'MED-CEFU-500', 'Cefuroxime 500 mg', 'Cefuroxime', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000007'), 'MED-OMEP-20', 'Omeprazole 20 mg', 'Omeprazole', '20 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000008'), 'MED-ESOM-40', 'Esomeprazole 40 mg', 'Esomeprazole', '40 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000009'), 'MED-CETI-10', 'Cetirizine 10 mg', 'Cetirizine', '10 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000010'), 'MED-LORA-10', 'Loratadine 10 mg', 'Loratadine', '10 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000011'), 'MED-DEXTRO-15', 'Dextromethorphan 15 mg/5 ml', 'Dextromethorphan', '15 mg/5 ml', 'SYRUP', 'chai', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000012'), 'MED-AMBRO-30', 'Ambroxol 30 mg', 'Ambroxol', '30 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000013'), 'MED-ACETY-200', 'Acetylcysteine 200 mg', 'Acetylcysteine', '200 mg', 'POWDER', 'goi', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000014'), 'MED-SALBU-2', 'Salbutamol 2 mg', 'Salbutamol', '2 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000015'), 'MED-VENTO', 'Salbutamol inhaler', 'Salbutamol', '100 mcg/lieu', 'INHALER', 'binh', 'INHALATION', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), 'MED-METFO-500', 'Metformin 500 mg', 'Metformin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'MED-GLIC-30', 'Gliclazide MR 30 mg', 'Gliclazide', '30 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000018'), 'MED-ATOR-20', 'Atorvastatin 20 mg', 'Atorvastatin', '20 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000019'), 'MED-AMLO-5', 'Amlodipine 5 mg', 'Amlodipine', '5 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000020'), 'MED-LOSAR-50', 'Losartan 50 mg', 'Losartan', '50 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000021'), 'MED-DICLO-50', 'Diclofenac 50 mg', 'Diclofenac', '50 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000022'), 'MED-MELOX-7', 'Meloxicam 7.5 mg', 'Meloxicam', '7.5 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000023'), 'MED-PRED-5', 'Prednisolone 5 mg', 'Prednisolone', '5 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000024'), 'MED-HYDRO-1', 'Hydrocortisone cream 1%', 'Hydrocortisone', '1%', 'CREAM', 'tuyp', 'TOPICAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000025'), 'MED-ORS', 'Oresol', 'Oral rehydration salts', 'goi 27.9 g', 'POWDER', 'goi', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000026'), 'MED-LOPE-2', 'Loperamide 2 mg', 'Loperamide', '2 mg', 'CAPSULE', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000027'), 'MED-BISAC-5', 'Bisacodyl 5 mg', 'Bisacodyl', '5 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000028'), 'MED-LEVO-500', 'Levofloxacin 500 mg', 'Levofloxacin', '500 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'MED-WARF-2', 'Warfarin 2 mg', 'Warfarin', '2 mg', 'TABLET', 'vien', 'ORAL', TRUE, '2026-08-01 01:00:00', NULL),
(UUID_TO_BIN('16000000-0000-0000-0000-000000000030'), 'MED-ASP-81', 'Aspirin 81 mg', 'Acetylsalicylic acid', '81 mg', 'TABLET', 'vien', 'ORAL', FALSE, '2026-08-01 01:00:00', '2026-08-15 01:00:00');

-- ===========================
-- Active drug interactions
-- ===========================

INSERT INTO drug_interactions (
    id, first_medicine_id, second_medicine_id, severity, description,
    recommendation, active, created_at, updated_at
) VALUES
(UUID_TO_BIN('16100000-0000-0000-0000-000000000001'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000021'), 'MAJOR', 'Ibuprofen va diclofenac lam tang nguy co xuat huyet tieu hoa.', 'Khong phoi hop hai NSAID; chon mot thuoc giam dau phu hop.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000003'), UUID_TO_BIN('16000000-0000-0000-0000-000000000004'), 'MODERATE', 'Trung lap dieu tri khang sinh penicillin.', 'Khong ke dong thoi amoxicillin va amoxicillin clavulanate.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000003'), UUID_TO_BIN('16000000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000028'), 'MAJOR', 'Tang nguy co keo dai QT khi dung azithromycin va levofloxacin.', 'Tranh phoi hop; xem xet khang sinh thay the.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000004'), UUID_TO_BIN('16000000-0000-0000-0000-000000000007'), UUID_TO_BIN('16000000-0000-0000-0000-000000000008'), 'MODERATE', 'Trung lap dieu tri PPI.', 'Chi su dung mot thuoc uc che bom proton.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000009'), UUID_TO_BIN('16000000-0000-0000-0000-000000000010'), 'MODERATE', 'Trung lap dieu tri khang histamine H1.', 'Chi dung mot khang histamine de han che buon ngu.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000014'), UUID_TO_BIN('16000000-0000-0000-0000-000000000015'), 'MODERATE', 'Trung lap salbutamol duong uong va duong hit.', 'Danh gia tong lieu salbutamol va nhip tim truoc khi phoi hop.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000007'), UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'MINOR', 'Tang nguy co ha duong huyet khi dung metformin va gliclazide.', 'Theo doi duong huyet va huong dan nhan biet ha duong huyet.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000008'), UUID_TO_BIN('16000000-0000-0000-0000-000000000021'), UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'CONTRAINDICATED', 'Diclofenac lam tang manh nguy co xuat huyet khi dung warfarin.', 'Tranh phoi hop; dung paracetamol neu can giam dau.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000009'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'MAJOR', 'Ibuprofen lam tang nguy co xuat huyet khi dung warfarin.', 'Tranh NSAID; can nhac paracetamol va theo doi INR.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000010'), UUID_TO_BIN('16000000-0000-0000-0000-000000000018'), UUID_TO_BIN('16000000-0000-0000-0000-000000000005'), 'MODERATE', 'Azithromycin co the lam tang nong do atorvastatin.', 'Theo doi dau co; can nhac tam dung statin khi dieu tri ngan ngay.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000011'), UUID_TO_BIN('16000000-0000-0000-0000-000000000023'), UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), 'MODERATE', 'Prednisolone co the lam tang duong huyet o nguoi dung metformin.', 'Theo doi duong huyet trong thoi gian su dung corticosteroid.', TRUE, '2026-08-01 01:10:00', NULL),
(UUID_TO_BIN('16100000-0000-0000-0000-000000000012'), UUID_TO_BIN('16000000-0000-0000-0000-000000000025'), UUID_TO_BIN('16000000-0000-0000-0000-000000000026'), 'MINOR', 'Loperamide co the che lap trieu chung khi tieu chay nhiem trung.', 'Chi dung sau khi danh gia nguyen nhan va ket hop bu nuoc.', TRUE, '2026-08-01 01:10:00', NULL);

-- ===========================
-- Prescription headers
-- ===========================

INSERT INTO prescriptions (
    id, prescription_code, medical_record_id, status, note,
    prescribed_by, prescribed_at, updated_by, updated_at
) VALUES
(UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), 'RX000001', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'DISPENSED', 'Dieu tri dau dau trieu chung.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:30:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'), '2026-08-20 02:35:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000002'), 'RX000002', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'DISPENSED', 'Don dieu tri viem duong ho hap cap.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:35:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6'), '2026-08-20 02:40:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), 'RX000003', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'PENDING_DISPENSE', 'Don mau cho cap phat va dieu chinh.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:40:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:45:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000004'), 'RX000004', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'CANCELLED', 'Don huy do thay doi phuong an dieu tri.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:42:00', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:46:00'),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), 'RX000005', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'PENDING_DISPENSE', 'Don mau co canh bao tuong tac da duoc xac nhan.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:47:00', NULL, NULL),
(UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), 'RX000006', UUID_TO_BIN('e0000000-0000-0000-0000-000000000001'), 'PENDING_DISPENSE', 'Don theo doi benh man tinh.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:50:00', NULL, NULL);

-- ===========================
-- Prescription items
-- ===========================

INSERT INTO prescription_items (
    id, prescription_id, medicine_id, medicine_name, active_ingredient,
    strength, unit, dosage, frequency, route, duration_days, quantity,
    instructions, created_at, updated_at
) VALUES
(UUID_TO_BIN('16300000-0000-0000-0000-000000000001'), UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), UUID_TO_BIN('16000000-0000-0000-0000-000000000001'), 'Paracetamol 500 mg', 'Paracetamol', '500 mg', 'vien', '1 vien', 'Moi 8 gio khi dau', 'ORAL', 3, 9, 'Uong sau an, toi da 3 g moi ngay.', '2026-08-20 02:30:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000002'), UUID_TO_BIN('16200000-0000-0000-0000-000000000001'), UUID_TO_BIN('16000000-0000-0000-0000-000000000007'), 'Omeprazole 20 mg', 'Omeprazole', '20 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 7, 7, 'Uong truoc an sang 30 phut.', '2026-08-20 02:30:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000003'), UUID_TO_BIN('16200000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000003'), 'Amoxicillin 500 mg', 'Amoxicillin', '500 mg', 'vien', '1 vien', 'Moi 8 gio', 'ORAL', 5, 15, 'Dung du lieu trinh dieu tri.', '2026-08-20 02:35:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000004'), UUID_TO_BIN('16200000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000012'), 'Ambroxol 30 mg', 'Ambroxol', '30 mg', 'vien', '1 vien', 'Ngay 3 lan', 'ORAL', 5, 15, 'Uong nhieu nuoc.', '2026-08-20 02:35:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000005'), UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), 'Metformin 500 mg', 'Metformin', '500 mg', 'vien', '1 vien', 'Ngay 2 lan', 'ORAL', 30, 60, 'Uong trong hoac sau an.', '2026-08-20 02:40:00', '2026-08-20 02:45:00'),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000006'), UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'Gliclazide MR 30 mg', 'Gliclazide', '30 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 30, 30, 'Uong cung bua an sang.', '2026-08-20 02:45:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000007'), UUID_TO_BIN('16200000-0000-0000-0000-000000000004'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), 'Ibuprofen 400 mg', 'Ibuprofen', '400 mg', 'vien', '1 vien', 'Ngay 2 lan', 'ORAL', 3, 6, 'Don nay da huy, khong cap phat.', '2026-08-20 02:42:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000008'), UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), 'Ibuprofen 400 mg', 'Ibuprofen', '400 mg', 'vien', '1 vien', 'Ngay 2 lan', 'ORAL', 3, 6, 'Can nhac nguy co xuat huyet va theo doi sat.', '2026-08-20 02:47:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000009'), UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'Warfarin 2 mg', 'Warfarin', '2 mg', 'vien', '1 vien', 'Moi toi', 'ORAL', 7, 7, 'Dung theo chi dinh chong dong hien tai.', '2026-08-20 02:47:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000010'), UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000019'), 'Amlodipine 5 mg', 'Amlodipine', '5 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 30, 30, 'Theo doi huyet ap tai nha.', '2026-08-20 02:50:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000011'), UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000020'), 'Losartan 50 mg', 'Losartan', '50 mg', 'vien', '1 vien', 'Moi sang', 'ORAL', 30, 30, 'Theo doi huyet ap va chuc nang than.', '2026-08-20 02:50:00', NULL),
(UUID_TO_BIN('16300000-0000-0000-0000-000000000012'), UUID_TO_BIN('16200000-0000-0000-0000-000000000006'), UUID_TO_BIN('16000000-0000-0000-0000-000000000018'), 'Atorvastatin 20 mg', 'Atorvastatin', '20 mg', 'vien', '1 vien', 'Moi toi', 'ORAL', 30, 30, 'Uong buoi toi.', '2026-08-20 02:50:00', NULL);

-- ===========================
-- Warning logs and amendments
-- ===========================

INSERT INTO prescription_warning_logs (
    id, prescription_id, drug_interaction_id, first_medicine_id,
    second_medicine_id, severity, warning_message, action, override_reason,
    handled_by, handled_at, created_at
) VALUES
(UUID_TO_BIN('16400000-0000-0000-0000-000000000001'), UUID_TO_BIN('16200000-0000-0000-0000-000000000005'), UUID_TO_BIN('16100000-0000-0000-0000-000000000009'), UUID_TO_BIN('16000000-0000-0000-0000-000000000002'), UUID_TO_BIN('16000000-0000-0000-0000-000000000029'), 'MAJOR', 'Ibuprofen lam tang nguy co xuat huyet khi dung warfarin. Recommendation: Tranh NSAID; can nhac paracetamol va theo doi INR.', 'OVERRIDDEN', 'Bac si da danh gia loi ich vuot nguy co trong thoi gian dieu tri rat ngan.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:47:00', '2026-08-20 02:47:00'),
(UUID_TO_BIN('16400000-0000-0000-0000-000000000002'), UUID_TO_BIN('16200000-0000-0000-0000-000000000003'), UUID_TO_BIN('16100000-0000-0000-0000-000000000007'), UUID_TO_BIN('16000000-0000-0000-0000-000000000016'), UUID_TO_BIN('16000000-0000-0000-0000-000000000017'), 'MINOR', 'Tang nguy co ha duong huyet khi dung metformin va gliclazide. Recommendation: Theo doi duong huyet va huong dan nhan biet ha duong huyet.', 'OVERRIDDEN', 'Phac do phoi hop duoc bac si chi dinh va da huong dan benh nhan theo doi ha duong huyet.', UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'), '2026-08-20 02:45:00', '2026-08-20 02:45:00');

INSERT INTO prescription_amendments (
    id, prescription_id, change_reason, before_data, after_data,
    amended_by, amended_at
) VALUES
(
    UUID_TO_BIN('16500000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('16200000-0000-0000-0000-000000000003'),
    'Bo sung gliclazide theo duong huyet luc doi cao.',
    JSON_OBJECT('schemaVersion', 1, 'prescriptionCode', 'RX000003', 'status', 'PENDING_DISPENSE', 'note', 'Don mau cho cap phat va dieu chinh.', 'items', JSON_ARRAY(JSON_OBJECT('medicineName', 'Metformin 500 mg', 'dosage', '1 vien', 'quantity', 60))),
    JSON_OBJECT('schemaVersion', 1, 'prescriptionCode', 'RX000003', 'status', 'PENDING_DISPENSE', 'note', 'Don mau cho cap phat va dieu chinh.', 'items', JSON_ARRAY(JSON_OBJECT('medicineName', 'Metformin 500 mg', 'dosage', '1 vien', 'quantity', 60), JSON_OBJECT('medicineName', 'Gliclazide MR 30 mg', 'dosage', '1 vien', 'quantity', 30))),
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2'),
    '2026-08-20 02:45:00'
);

-- The next automatically generated code is RX000007.
INSERT INTO prescription_code_sequences (code_prefix, last_value)
VALUES ('RX', 6);
