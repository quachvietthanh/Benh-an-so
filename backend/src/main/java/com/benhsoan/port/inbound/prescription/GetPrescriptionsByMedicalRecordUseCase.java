package com.benhsoan.port.inbound.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionResult;

public interface GetPrescriptionsByMedicalRecordUseCase {

    List<PrescriptionResult> getByMedicalRecordId(UUID medicalRecordId);
}
