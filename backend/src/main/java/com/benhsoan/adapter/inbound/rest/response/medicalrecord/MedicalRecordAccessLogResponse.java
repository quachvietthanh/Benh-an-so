package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;

public record MedicalRecordAccessLogResponse(
        UUID id, UUID patientId, UUID visitId, UUID medicalRecordId, UUID accessedBy,
        MedicalRecordAccessAction action, String detail, Instant accessedAt
) {
}
