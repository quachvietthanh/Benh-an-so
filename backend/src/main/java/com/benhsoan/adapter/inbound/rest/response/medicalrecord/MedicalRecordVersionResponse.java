package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;

public record MedicalRecordVersionResponse(
        int versionNumber,
        String modifiedBy,
        Instant modifiedAt,
        String reason,
        String content,
        MedicalRecordClinicalSnapshotResponse snapshot
) {
}
