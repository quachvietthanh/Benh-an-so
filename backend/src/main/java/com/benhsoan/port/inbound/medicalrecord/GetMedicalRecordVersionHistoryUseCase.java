package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordVersionHistoryResult;

/**
 * Inbound port for retrieving the version/amendment history of a medical record
 * (NCL-11-CN-003).
 */
public interface GetMedicalRecordVersionHistoryUseCase {

    MedicalRecordVersionHistoryResult getVersionHistory(UUID medicalRecordId);
}
