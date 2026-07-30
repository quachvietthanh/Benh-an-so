package com.benhsoan.domain.clinical.enums;

public enum ClinicalResultType {
    NUMBER, TEXT, FILE, MIXED;

    public static ClinicalResultType from(ClinicalResultDataType resultDataType) {
        return ClinicalResultType.valueOf(resultDataType.name());
    }

    public boolean requiresAttachment() {
        return this == FILE;
    }
}
