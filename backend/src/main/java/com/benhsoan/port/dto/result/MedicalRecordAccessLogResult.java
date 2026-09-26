package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;

public record MedicalRecordAccessLogResult(
        UUID id, UUID patientId, UUID visitId, UUID medicalRecordId, UUID accessedBy,
        MedicalRecordAccessAction action, String detail, Instant accessedAt
) {
}
