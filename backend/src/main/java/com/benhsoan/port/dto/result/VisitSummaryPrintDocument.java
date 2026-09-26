package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record VisitSummaryPrintDocument(
        String clinicName,
        String clinicAddress,
        String clinicPhone,
        String patientCode,
        String patientName,
        String patientDateOfBirth,
        String patientGender,
        String patientPhone,
        String visitCode,
        Instant visitAt,
        String doctorName,
        List<Diagnosis> diagnoses,
        List<ClinicalOrder> clinicalOrders,
        String doctorInstructions,
        String treatmentPlan,
        LocalDate revisitDate,
        String signedByName,
        Instant signedAt,
        String printedByName,
        Instant printedAt
) {

    public record Diagnosis(
            String code,
            String name,
            boolean isPrimary
    ) {
    }

    public record ClinicalOrder(
            String orderCode,
            String serviceCode,
            String serviceName,
            String instruction,
            String status
    ) {
    }
}
