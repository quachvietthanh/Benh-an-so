package com.benhsoan.adapter.inbound.rest.response.portal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.benhsoan.port.dto.result.portal.PortalLookupResult;

public record PortalLookupResponse(
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
        List<DiagnosisItemResponse> diagnoses,
        String conclusion,
        String doctorInstructions,
        List<ClinicalTestResultItemResponse> clinicalTestResults,
        List<PrescriptionItemResponse> prescriptions
) {

    public static PortalLookupResponse from(PortalLookupResult result) {
        return new PortalLookupResponse(
                result.appointmentCode(),
                result.appointmentStartTime(),
                result.appointmentReason(),
                result.patientName(),
                result.patientDateOfBirth(),
                result.patientGender(),
                result.patientPhoneMasked(),
                result.visitCode(),
                result.visitAt(),
                result.doctorName(),
                result.diagnoses() == null ? List.of() : result.diagnoses().stream()
                        .map(d -> new DiagnosisItemResponse(d.code(), d.name(), d.type()))
                        .toList(),
                result.conclusion(),
                result.doctorInstructions(),
                result.clinicalTestResults() == null ? List.of() : result.clinicalTestResults().stream()
                        .map(c -> new ClinicalTestResultItemResponse(
                                c.serviceCode(), c.serviceName(), c.resultType(), c.value(),
                                c.unit(), c.referenceRange(), c.conclusion()))
                        .toList(),
                result.prescriptions() == null ? List.of() : result.prescriptions().stream()
                        .map(p -> new PrescriptionItemResponse(
                                p.medicineName(), p.activeIngredient(), p.strength(), p.unit(),
                                p.dosage(), p.frequency(), p.route(), p.durationDays(),
                                p.quantity(), p.instructions()))
                        .toList()
        );
    }

    public record DiagnosisItemResponse(String code, String name, String type) {}

    public record ClinicalTestResultItemResponse(
            String serviceCode,
            String serviceName,
            String resultType,
            String value,
            String unit,
            String referenceRange,
            String conclusion
    ) {}

    public record PrescriptionItemResponse(
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            Integer frequency,
            String route,
            Integer durationDays,
            int quantity,
            String instructions
    ) {}
}
