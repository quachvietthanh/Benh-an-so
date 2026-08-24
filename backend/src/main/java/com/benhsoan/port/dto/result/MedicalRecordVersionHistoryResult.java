package com.benhsoan.port.dto.result;

import java.util.List;

/**
 * Version history of a medical record. When {@code originalOnly} is true the
 * record has no amendments and {@code amendments} is empty (NCL-11-CN-003 TC-02).
 */
public record MedicalRecordVersionHistoryResult(
        boolean originalOnly,
        MedicalRecordVersion originalVersion,
        List<MedicalRecordVersion> amendments
) {
    public MedicalRecordVersionHistoryResult {
        amendments = List.copyOf(amendments);
    }
}
