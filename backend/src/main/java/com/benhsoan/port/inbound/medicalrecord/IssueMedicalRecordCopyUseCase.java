package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.IssueMedicalRecordCopyCommand;
import com.benhsoan.port.dto.result.MedicalRecordCopyResult;

public interface IssueMedicalRecordCopyUseCase {

    MedicalRecordCopyResult issue(IssueMedicalRecordCopyCommand command);
}
