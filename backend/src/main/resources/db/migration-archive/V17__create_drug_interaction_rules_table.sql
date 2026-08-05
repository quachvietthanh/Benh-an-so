-- =====================================================
-- V17__create_drug_interaction_rules_table.sql
-- Drug interaction rules keyed by active ingredients
-- MySQL 8.x
-- =====================================================

-- ===========================
-- Drug interaction rules
-- ===========================

CREATE TABLE drug_interaction_rules (
    id BINARY(16) NOT NULL,
    active_ingredient_a VARCHAR(255) NOT NULL,
    active_ingredient_b VARCHAR(255) NOT NULL,
    severity_level VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    clinical_recommendation TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_drug_interaction_rules PRIMARY KEY (id),
    -- The unique constraint below also creates the composite index on
    -- (active_ingredient_a, active_ingredient_b), which makes the symmetric
    -- bidirectional (A-B / B-A) lookup fast.
    CONSTRAINT uk_drug_interaction_rules_ingredient_pair
        UNIQUE (active_ingredient_a, active_ingredient_b),
    CONSTRAINT chk_drug_interaction_rules_different_ingredients CHECK (
        active_ingredient_a <> active_ingredient_b
    ),
    CONSTRAINT chk_drug_interaction_rules_severity CHECK (
        severity_level IN ('MILD', 'MODERATE', 'SEVERE', 'CONTRAINDICATED')
    )
);

CREATE INDEX idx_drug_interaction_rules_ingredient_b
    ON drug_interaction_rules(active_ingredient_b);

CREATE INDEX idx_drug_interaction_rules_active_severity
    ON drug_interaction_rules(is_active, severity_level);

-- ===========================
-- Seed common interaction rules
-- ===========================

INSERT INTO drug_interaction_rules (id, active_ingredient_a, active_ingredient_b, severity_level, description, clinical_recommendation, is_active, created_at, updated_at) VALUES
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000001'), 'Aspirin', 'Warfarin', 'SEVERE', 'Aspirin làm tăng nguy cơ chảy máu khi phối hợp với Warfarin, đặc biệt là xuất huyết tiêu hóa.', 'Cân nhắc ngừng Aspirin; nếu bắt buộc dùng chung, theo dõi INR sát và chỉ định bảo vệ dạ dày.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000002'), 'Ibuprofen', 'Warfarin', 'SEVERE', 'NSAID làm tăng nguy cơ xuất huyết tiêu hóa và có thể làm tăng tác dụng chống đông của Warfarin.', 'Tránh phối hợp; nếu cần giảm đau, cân nhắc Paracetamol và theo dõi INR cùng triệu chứng chảy máu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000003'), 'Clarithromycin', 'Simvastatin', 'CONTRAINDICATED', 'Clarithromycin ức chế CYP3A4 làm tăng nồng độ Simvastatin, nguy cơ tiêu cơ vân và tổn thương thận cấp.', 'Chống chỉ định phối hợp; thay bằng kháng sinh khác hoặc tạm ngừng Simvastatin trong thời gian điều trị.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000004'), 'Gemfibrozil', 'Simvastatin', 'CONTRAINDICATED', 'Gemfibrozil làm tăng nồng độ Simvastatin, nguy cơ tiêu cơ vân nghiêm trọng.', 'Chống chỉ định phối hợp; cân nhắc nhóm fibrate khác hoặc ngừng một trong hai thuốc.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000005'), 'Amiodarone', 'Simvastatin', 'SEVERE', 'Amiodarone ức chế CYP3A4 làm tăng nồng độ Simvastatin, tăng nguy cơ bệnh cơ.', 'Giới hạn liều Simvastatin tối đa 20 mg/ngày hoặc đổi sang statin khác ít tương tác.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000006'), 'Isosorbide Mononitrate', 'Sildenafil', 'CONTRAINDICATED', 'Phối hợp Sildenafil với nitrate gây tụt huyết áp nặng, nguy cơ nhồi máu cơ tim.', 'Chống chỉ định; không dùng Sildenafil trong vòng 24 - 48 giờ sau khi dùng nitrate.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000007'), 'Methotrexate', 'Trimethoprim', 'CONTRAINDICATED', 'Trimethoprim làm tăng độc tính ức chế tủy xương của Methotrexate.', 'Chống chỉ định phối hợp; chọn kháng sinh thay thế và theo dõi công thức máu.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000008'), 'Digoxin', 'Furosemide', 'MODERATE', 'Furosemide gây hạ kali máu làm tăng độc tính của Digoxin.', 'Theo dõi kali máu và nồng độ Digoxin; bổ sung kali khi cần thiết.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000009'), 'Ciprofloxacin', 'Tizanidine', 'CONTRAINDICATED', 'Ciprofloxacin ức chế CYP1A2 làm tăng nồng độ Tizanidine, gây tụt huyết áp và an thần nặng.', 'Chống chỉ định phối hợp; chọn kháng sinh khác.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000a'), 'Ciprofloxacin', 'Theophylline', 'SEVERE', 'Ciprofloxacin làm tăng nồng độ Theophylline, nguy cơ độc tính thần kinh và tim mạch.', 'Giảm liều Theophylline và theo dõi nồng độ thuốc trong máu khi dùng chung.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000b'), 'Ibuprofen', 'Lithium', 'SEVERE', 'NSAID làm giảm thải trừ Lithium, tăng nguy cơ ngộ độc Lithium.', 'Tránh phối hợp; nếu dùng chung, theo dõi nồng độ Lithium chặt chẽ.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000c'), 'Lisinopril', 'Potassium Chloride', 'MODERATE', 'Phối hợp thuốc ức chế men chuyển với bổ sung kali làm tăng nguy cơ tăng kali máu.', 'Theo dõi kali máu định kỳ; hạn chế bổ sung kali trừ khi thật sự cần.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000d'), 'Paracetamol', 'Warfarin', 'MODERATE', 'Sử dụng Paracetamol liều cao kéo dài có thể làm tăng INR và nguy cơ chảy máu.', 'Dùng Paracetamol liều thấp, ngắn ngày và theo dõi INR định kỳ.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000e'), 'Fluoxetine', 'Tramadol', 'SEVERE', 'Phối hợp SSRI với Tramadol làm tăng nguy cơ hội chứng serotonin và co giật.', 'Tránh phối hợp; cân nhắc nhóm giảm đau khác.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-00000000000f'), 'Clopidogrel', 'Omeprazole', 'MODERATE', 'Omeprazole ức chế CYP2C19 làm giảm hiệu quả chống ngưng tập tiểu cầu của Clopidogrel.', 'Ưu tiên Pantoprazole hoặc thuốc ức chế bơm proton khác ít tương tác hơn.', TRUE, NOW(), NULL),
(UUID_TO_BIN('d1a00000-0000-0000-0000-000000000010'), 'Cetirizine', 'Diphenhydramine', 'MILD', 'Phối hợp hai kháng histamine làm tăng tác dụng an thần.', 'Theo dõi buồn ngủ; tránh lái xe hoặc vận hành máy móc.', TRUE, NOW(), NULL);
