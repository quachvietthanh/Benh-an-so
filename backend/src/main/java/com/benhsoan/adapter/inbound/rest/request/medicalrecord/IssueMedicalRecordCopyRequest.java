package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.MedicalRecordCopyRecipientType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IssueMedicalRecordCopyRequest(
        @NotNull MedicalRecordCopyRecipientType recipientType,
        @NotBlank String recipientName,
        @NotBlank String recipientIdentityNumber,
        String requestReason,
        String authorizationDocumentNumber
) {
}
