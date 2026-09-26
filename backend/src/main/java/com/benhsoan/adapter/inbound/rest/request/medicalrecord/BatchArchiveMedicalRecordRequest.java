package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;
import java.util.UUID;

public record BatchArchiveMedicalRecordRequest(
        List<UUID> medicalRecordIds,
        Boolean archiveAllEligible
) {
}
