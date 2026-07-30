package com.benhsoan.port.inbound.medicalrecord;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;

public interface GetMedicalRecordAccessLogsUseCase {
    Page<MedicalRecordAccessLogResult> getAccessLogs(GetMedicalRecordAccessLogsQuery query);
}
