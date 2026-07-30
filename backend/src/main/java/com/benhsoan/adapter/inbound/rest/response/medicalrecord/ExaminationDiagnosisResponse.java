package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExaminationDiagnosisResponse(
        UUID id,
        UUID visitId,
        UUID doctorId,
        String primaryIcdCode,
        String primaryIcdName,
        List<SecondaryDiagnosisResponse> secondaryDiagnoses,
        String clinicalNotes,
        Instant diagnosedAt,
        List<ClinicalOrderSummaryResponse> clinicalOrders
) {
    public record SecondaryDiagnosisResponse(UUID id, String code, String name) {}

    public record ClinicalOrderSummaryResponse(
            UUID id,
            String orderCode,
            String serviceCode,
            String serviceName,
            String status,
            Instant orderedAt
    ) {}
}
