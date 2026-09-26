package com.benhsoan.domain.patient.enums;

public enum ConsentHistoryStatus {
    AGREED("Đồng ý xử lý dữ liệu"),
    PARTIALLY_WITHDRAWN("Thu hẹp phạm vi đồng ý"),
    WITHDRAWN("Rút lại toàn bộ sự đồng ý");

    private final String description;

    ConsentHistoryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
