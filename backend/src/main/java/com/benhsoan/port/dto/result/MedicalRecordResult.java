package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

public record MedicalRecordResult(
        UUID id, UUID visitId, String chiefComplaint, String symptoms, String medicalHistory,
        String physicalExamination, String clinicalProgress, String treatmentPlan,
        String doctorInstructions, String conclusion, MedicalRecordStatus status,
        Instant lockedAt, UUID lockedBy, UUID createdBy, Instant createdAt,
        UUID updatedBy, Instant updatedAt
) {
}
