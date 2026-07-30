package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.AmendMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordAmendmentResult;

public interface AmendMedicalRecordUseCase {
    MedicalRecordAmendmentResult amend(UUID medicalRecordId, AmendMedicalRecordCommand command);
}
