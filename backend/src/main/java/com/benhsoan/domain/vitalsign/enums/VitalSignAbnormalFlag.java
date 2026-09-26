package com.benhsoan.domain.vitalsign.enums;

public enum VitalSignAbnormalFlag {
    HYPERTENSION("Huyết áp cao"),
    HYPOTENSION("Huyết áp thấp"),
    TACHYCARDIA("Nhịp tim nhanh"),
    BRADYCARDIA("Nhịp tim chậm"),
    FEVER("Sốt"),
    HYPOTHERMIA("Hạ thân nhiệt"),
    TACHYPNEA("Nhịp thở nhanh"),
    BRADYPNEA("Nhịp thở chậm"),
    HYPOXEMIA("Giảm nồng độ oxy máu"),
    UNDERWEIGHT("Thiếu cân"),
    OVERWEIGHT("Thừa cân / Béo phì");

    private final String description;

    VitalSignAbnormalFlag(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
