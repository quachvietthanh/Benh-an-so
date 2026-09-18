package com.benhsoan.adapter.inbound.rest.response.visit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;

public record VisitSummaryResponse(
        UUID visitId,
        String visitCode,
        Instant visitAt,
        ClinicInfo clinic,
        PatientInfo patient,
        DoctorInfo doctor,
        MedicalRecordInfo medicalRecord,
        List<DiagnosisItem> diagnoses,
        List<ClinicalOrderItemInfo> clinicalOrders,
        String doctorInstructions,
        String treatmentPlan,
        LocalDate revisitDate,
        List<PrintHistoryItem> printHistory
) {

    public record ClinicInfo(
            String name,
            String address,
            String phone
    ) {
    }

    public record PatientInfo(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String identityNumber
    ) {
    }

    public record DoctorInfo(
            UUID id,
            String fullName
    ) {
    }

    public record MedicalRecordInfo(
            UUID id,
            MedicalRecordStatus status,
            Instant signedAt,
            UUID signedBy,
            String signedByName
    ) {
    }

    public record DiagnosisItem(
            String code,
            String name,
            boolean isPrimary
    ) {
    }

    public record ClinicalOrderItemInfo(
            String orderCode,
            String serviceCode,
            String serviceName,
            String instruction,
            String status
    ) {
    }

    public record PrintHistoryItem(
            UUID printedBy,
            String printedByName,
            Instant printedAt,
            String detail
    ) {
    }
}
