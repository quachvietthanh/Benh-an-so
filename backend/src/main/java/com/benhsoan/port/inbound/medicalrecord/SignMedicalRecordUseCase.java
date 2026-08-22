package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;

public interface SignMedicalRecordUseCase {

    MedicalRecordResult sign(UUID medicalRecordId, SignMedicalRecordCommand command);
}
