package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.dto.result.MedicalRecordResult;

/**
 * Inbound port for fetching medical record content (NCL-04-CN-004).
 * Every read access MUST trigger a medical record access audit log (QTN-02).
 */
public interface GetMedicalRecordUseCase {

    MedicalRecordResult getById(UUID medicalRecordId);

    MedicalRecordResult getByVisitId(UUID visitId);

    MedicalRecordDetailResult getDetailByVisitId(UUID visitId);

    List<MedicalRecordDetailResult> getHistoryByPatientId(UUID patientId);
}
