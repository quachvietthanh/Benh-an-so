package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExaminationDiagnosisResult(
        UUID id,
        UUID visitId,
        UUID doctorId,
        String primaryIcdCode,
        String primaryIcdName,
        List<SecondaryDiagnosis> secondaryDiagnoses,
        String clinicalNotes,
        Instant diagnosedAt,
        List<ClinicalOrderResult> clinicalOrders
) {
    public record SecondaryDiagnosis(UUID id, String code, String name) {}

    public record ClinicalOrderResult(
            UUID id,
            String orderCode,
            String serviceCode,
            String serviceName,
            String status,
            Instant orderedAt
    ) {}
}
