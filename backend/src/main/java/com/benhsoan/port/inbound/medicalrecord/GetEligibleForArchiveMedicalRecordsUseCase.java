package com.benhsoan.port.inbound.medicalrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.ArchiveEligibleMedicalRecordResult;

public interface GetEligibleForArchiveMedicalRecordsUseCase {

    Page<ArchiveEligibleMedicalRecordResult> getEligibleRecords(Pageable pageable);
}
