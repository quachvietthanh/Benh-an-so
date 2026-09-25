package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.List;
import java.util.UUID;

public record BatchArchiveMedicalRecordResponse(
        int totalRequested,
        int archivedCount,
        int skippedCount,
        List<UUID> archivedMedicalRecordIds,
        List<UUID> skippedMedicalRecordIds
) {
}
