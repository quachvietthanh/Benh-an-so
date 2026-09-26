package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface LockMedicalRecordUseCase {
    MedicalRecordResult lock(UUID medicalRecordId);
}
