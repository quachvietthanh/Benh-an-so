package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;

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
        LocalDate revisitDate,
        MedicalRecordStatus status,
        String signatureData,
        Instant signedAt,
        UUID signedBy,
        Instant lockedAt,
        UUID lockedBy,
        String primaryIcdCode,
        String primaryIcdName,
        List<String> secondaryIcdCodes,
        List<MedicalRecordDiagnosisResponse> diagnoses,
        AppliedMedicalRecordTemplateResponse appliedTemplate
) {
    public MedicalRecordDetailResponse(PatientInfo patient, VisitInfo visit, UUID medicalRecordId, String chiefComplaint,
            String symptoms, String medicalHistory, String physicalExamination, String clinicalProgress,
            String treatmentPlan, String doctorInstructions, String conclusion, MedicalRecordStatus status,
            String signatureData, Instant signedAt, UUID signedBy, Instant lockedAt, UUID lockedBy,
            String primaryIcdCode, String primaryIcdName, List<String> secondaryIcdCodes,
            List<MedicalRecordDiagnosisResponse> diagnoses, AppliedMedicalRecordTemplateResponse appliedTemplate) {
        this(patient, visit, medicalRecordId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null, status, signatureData, signedAt,
                signedBy, lockedAt, lockedBy, primaryIcdCode, primaryIcdName, secondaryIcdCodes, diagnoses, appliedTemplate);
    }

    public MedicalRecordDetailResponse(PatientInfo patient, VisitInfo visit, UUID medicalRecordId, String chiefComplaint,
            String symptoms, String medicalHistory, String physicalExamination, String clinicalProgress,
            String treatmentPlan, String doctorInstructions, String conclusion, MedicalRecordStatus status,
            String signatureData, Instant signedAt, UUID signedBy, Instant lockedAt, UUID lockedBy,
            String primaryIcdCode, String primaryIcdName, List<String> secondaryIcdCodes,
            List<MedicalRecordDiagnosisResponse> diagnoses) {
        this(patient, visit, medicalRecordId, chiefComplaint, symptoms, medicalHistory, physicalExamination,
                clinicalProgress, treatmentPlan, doctorInstructions, conclusion, null, status, signatureData, signedAt,
                signedBy, lockedAt, lockedBy, primaryIcdCode, primaryIcdName, secondaryIcdCodes, diagnoses, null);
    }

    public record PatientInfo(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String identityNumber,
            String insuranceNumber,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone
    ) {
        public PatientInfo(
                UUID id,
                String patientCode,
                String fullName,
                LocalDate dateOfBirth,
                Gender gender,
                String phone,
                String identityNumber,
                String insuranceNumber
        ) {
            this(id, patientCode, fullName, dateOfBirth, gender, phone, identityNumber, insuranceNumber, null, null, null);
        }
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
