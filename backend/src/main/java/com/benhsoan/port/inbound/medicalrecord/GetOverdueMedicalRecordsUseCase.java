package com.benhsoan.port.inbound.medicalrecord;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.medicalrecord.GetOverdueMedicalRecordsQuery;
import com.benhsoan.port.dto.result.OverdueMedicalRecordResult;

public interface GetOverdueMedicalRecordsUseCase {

    Page<OverdueMedicalRecordResult> getOverdueRecords(GetOverdueMedicalRecordsQuery query);
}
