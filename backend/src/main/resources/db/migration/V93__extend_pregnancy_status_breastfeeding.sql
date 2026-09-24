-- =====================================================
-- V90__extend_pregnancy_status_breastfeeding.sql
-- NCL-05-CN-006: Mở rộng trạng thái thai kỳ và loại chống chỉ định
-- Bổ sung giá trị BREASTFEEDING (Phụ nữ đang cho con bú)
-- (Đánh lại version để tránh trùng V78 với NCL-06-CN-014)
-- =====================================================

ALTER TABLE patients DROP CONSTRAINT chk_patients_pregnancy_status;
ALTER TABLE patients ADD CONSTRAINT chk_patients_pregnancy_status
    CHECK (pregnancy_status IS NULL OR pregnancy_status IN ('PREGNANT', 'NOT_PREGNANT', 'BREASTFEEDING'));

ALTER TABLE contraindication_rules DROP CONSTRAINT chk_contraindication_rules_type;
ALTER TABLE contraindication_rules ADD CONSTRAINT chk_contraindication_rules_type
    CHECK (contraindication_type IN ('AGE', 'PREGNANCY', 'DISEASE', 'BREASTFEEDING'));

ALTER TABLE prescription_contraindication_warning_logs DROP CONSTRAINT chk_presc_contra_warning_type;
ALTER TABLE prescription_contraindication_warning_logs ADD CONSTRAINT chk_presc_contra_warning_type
    CHECK (contraindication_type IN ('AGE', 'PREGNANCY', 'DISEASE', 'BREASTFEEDING'));
