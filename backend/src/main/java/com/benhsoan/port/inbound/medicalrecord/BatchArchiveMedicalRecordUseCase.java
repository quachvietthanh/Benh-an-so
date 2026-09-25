package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.BatchArchiveMedicalRecordResult;

public interface BatchArchiveMedicalRecordUseCase {

    BatchArchiveMedicalRecordResult batchArchive(List<UUID> medicalRecordIds, boolean archiveAllEligible);
}
