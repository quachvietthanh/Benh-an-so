package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Size;

public record ExportMedicalRecordExchangeRequest(
        @Size(max = 100, message = "Maximum 100 medical records can be exported in a single batch.")
        List<UUID> medicalRecordIds,

        @Size(max = 100, message = "Maximum 100 visits can be exported in a single batch.")
        List<UUID> visitIds,

        String format
) {
}
