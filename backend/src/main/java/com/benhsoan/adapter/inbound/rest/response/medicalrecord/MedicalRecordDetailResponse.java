package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;

/**
 * REST display model for reviewing medical record content (NCL-04-CN-004).
 */
public record MedicalRecordDetailResponse(
        PatientInfo patient,
        VisitInfo visit,
        UUID medicalRecordId,
        String chiefComplaint,
        String symptoms,
        String medicalHistory,
        String physicalExamination,
        String clinicalProgress,
        String treatmentPlan,
        String doctorInstructions,
        String conclusion,
        MedicalRecordStatus status,
        Instant lockedAt,
        UUID lockedBy,
        String primaryIcdCode,
        String primaryIcdName,
        List<String> secondaryIcdCodes,
        List<MedicalRecordDiagnosisResponse> diagnoses
) {

    public record PatientInfo(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String identityNumber,
            String insuranceNumber
    ) {
    }

    public record VisitInfo(
            UUID id,
            String visitCode,
            VisitType visitType,
            VisitStatus status,
            Instant visitAt,
            Instant startedAt,
            Instant completedAt,
            String reason,
            String note,
            UUID doctorId,
            String doctorName
    ) {
    }
}
