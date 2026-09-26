package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.List;

public record MedicalRecordVersionHistoryResponse(
        boolean originalOnly,
        MedicalRecordVersionResponse originalVersion,
        List<MedicalRecordVersionResponse> amendments
) {
}
