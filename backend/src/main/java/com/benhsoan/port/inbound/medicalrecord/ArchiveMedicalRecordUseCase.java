package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface ArchiveMedicalRecordUseCase {

    MedicalRecordResult archive(UUID medicalRecordId);
}
