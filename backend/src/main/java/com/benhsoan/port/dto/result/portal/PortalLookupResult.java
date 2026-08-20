package com.benhsoan.port.dto.result.portal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PortalLookupResult(
        String appointmentCode,
        Instant appointmentStartTime,
        String appointmentReason,
        String patientName,
        LocalDate patientDateOfBirth,
        String patientGender,
        String patientPhoneMasked,
        String visitCode,
        Instant visitAt,
        String doctorName,
        List<DiagnosisItem> diagnoses,
        String conclusion,
        String doctorInstructions,
        List<ClinicalTestResultItem> clinicalTestResults,
        List<PrescriptionItemView> prescriptions
) {

    public record DiagnosisItem(String code, String name, String type) {}

    public record ClinicalTestResultItem(
            String serviceCode,
            String serviceName,
            String resultType,
            String value,
            String unit,
            String referenceRange,
            String conclusion
    ) {}

    public record PrescriptionItemView(
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            String frequency,
            String route,
            Integer durationDays,
            int quantity,
            String instructions
    ) {}
}
