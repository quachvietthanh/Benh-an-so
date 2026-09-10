package com.benhsoan.domain.medicalrecord.enums;

import java.util.Set;

public enum MedicalRecordAccessAction {
    VIEW, VIEW_HISTORY, CREATE, UPDATE, TEMPLATE_APPLY, SIGN, LOCK, AMEND, EXPORT, ARCHIVE;

    /**
     * Read-type actions that represent a user opening/viewing medical record data.
     * These are the events counted as "lượt truy cập bệnh án" in NCL-15-CN-004
     * (consistent with QTN-25 "mở bệnh án" and the NCL-15-CN-002 anomaly scanner).
     */
    public static final Set<MedicalRecordAccessAction> ACCESS_ACTIONS = Set.of(VIEW, VIEW_HISTORY);
}
