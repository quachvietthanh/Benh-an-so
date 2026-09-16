package com.benhsoan.application.ucservice.patient;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.dto.result.PatientResult;

@Component
public class PatientResultMapper {

    public PatientResult toResult(Patient patient) {
        return toResult(patient, null);
    }

    public PatientResult toResult(Patient patient, String mergedIntoPatientCode) {

        return new PatientResult(
                patient.getId(),
                patient.getPatientCode(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getIdentityNumber(),
                patient.getInsuranceNumber(),
                patient.getBloodType(),
                patient.getEmergencyContact(),
                patient.getEmergencyRelationship(),
                patient.getEmergencyPhone(),
                patient.getGuardianName(),
                patient.getGuardianRelationship(),
                patient.getGuardianPhone(),
                patient.getGuardianIdentityNumber(),
                patient.getGuardianUserId(),
                patient.getConsentSignerName(),
                patient.isMinor(),
                patient.requiresAdultTransition(),
                patient.isActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.isConsentAgreed(),
                patient.getConsentAgreedAt(),
                patient.getConsentVersion(),
                patient.isConsentWithdrawn(),
                patient.getConsentWithdrawnAt(),
                patient.getConsentWithdrawnReason(),
                patient.isNonMedicalUseRestricted(),
                patient.getStatus(),
                patient.isMerged(),
                patient.getMergedIntoPatientId(),
                mergedIntoPatientCode,
                patient.getMergedAt(),
                patient.getMergedBy(),
                patient.getMergeReason()
        );
    }

    public Page<PatientResult> toResult(Page<Patient> patients) {
        return patients.map(this::toResult);
    }
}