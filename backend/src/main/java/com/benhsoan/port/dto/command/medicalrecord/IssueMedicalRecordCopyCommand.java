package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

public record IssueMedicalRecordCopyCommand(
        UUID medicalRecordId,
        MedicalRecordCopyRecipientType recipientType,
        String recipientName,
        String recipientIdentityNumber,
        String requestReason,
        String authorizationDocumentNumber
) {
}
