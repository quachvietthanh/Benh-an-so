package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.patient.MergePatientsRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.RegisterPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.SearchPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.DuplicatePatientGroupResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.MergePatientsResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.port.dto.command.patient.MergePatientsCommand;
import com.benhsoan.port.dto.command.patient.RegisterPatientCommand;
import com.benhsoan.port.dto.command.patient.SearchPatientCommand;
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.patient.DuplicatePatientGroupResult;
import com.benhsoan.port.dto.result.patient.MergePatientsResult;
import com.benhsoan.port.dto.result.PatientResult;

@Component
public class PatientRestMapper {

    private final AnonymizationModeState anonymizationModeState;

    public PatientRestMapper(
            AnonymizationModeState anonymizationModeState) {
        this.anonymizationModeState = anonymizationModeState;
    }

    public RegisterPatientCommand toCommand(RegisterPatientRequest request) {

        return RegisterPatientCommand.builder()
                .fullName(request.fullName())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .phone(request.phone())
                .email(request.email())
                .address(request.address())
                .identityNumber(request.identityNumber())
                .insuranceNumber(request.insuranceNumber())
                .bloodType(request.bloodType())
                .emergencyContact(request.emergencyContact())
                .emergencyRelationship(request.emergencyRelationship())
                .emergencyPhone(request.emergencyPhone())
                .consentAgreed(request.consentAgreed())
                .consentVersion(request.consentVersion())
                .guardianName(request.guardianName())
                .guardianRelationship(request.guardianRelationship())
                .guardianPhone(request.guardianPhone())
                .guardianIdentityNumber(request.guardianIdentityNumber())
                .consentSignerName(request.consentSignerName())
                .build();
    }

    public UpdatePatientCommand toCommand(UpdatePatientRequest request) {

        return UpdatePatientCommand.builder()
                .fullName(request.fullName())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .phone(request.phone())
                .email(request.email())
                .address(request.address())
                .identityNumber(request.identityNumber())
                .insuranceNumber(request.insuranceNumber())
                .bloodType(request.bloodType())
                .emergencyContact(request.emergencyContact())
                .emergencyRelationship(request.emergencyRelationship())
                .emergencyPhone(request.emergencyPhone())
                .active(request.active())
                .consentAgreed(request.consentAgreed())
                .consentWithdrawn(request.consentWithdrawn())
                .consentWithdrawnReason(request.consentWithdrawnReason())
                .consentVersion(request.consentVersion())
                .guardianName(request.guardianName())
                .guardianRelationship(request.guardianRelationship())
                .guardianPhone(request.guardianPhone())
                .guardianIdentityNumber(request.guardianIdentityNumber())
                .consentSignerName(request.consentSignerName())
                .transitionToAdult(request.transitionToAdult())
                .build();
    }

    public UpdatePatientCommand toConsentCommand(com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientConsentRequest request) {

        return UpdatePatientCommand.builder()
                .consentAgreed(request.consentAgreed())
                .consentWithdrawn(request.consentWithdrawn())
                .consentWithdrawnReason(request.consentWithdrawnReason())
                .consentVersion(request.consentVersion())
                .build();
    }

    public PatientResponse toResponse(PatientResult result) {

        String fullName = anonymizationModeState.isEnabled() ? PatientAnonymizer.maskFullName(result.patientCode()) : result.fullName();
        String phone = anonymizationModeState.isEnabled() ? PatientAnonymizer.maskPhone(result.phone()) : result.phone();
        String address = anonymizationModeState.isEnabled() ? PatientAnonymizer.maskAddress(result.address()) : result.address();
        String emergencyContact = anonymizationModeState.isEnabled() && result.emergencyContact() != null
                ? PatientAnonymizer.maskFullName(null)
                : result.emergencyContact();
        String emergencyPhone = anonymizationModeState.isEnabled()
                ? PatientAnonymizer.maskPhone(result.emergencyPhone())
                : result.emergencyPhone();
        String guardianName = anonymizationModeState.isEnabled() && result.guardianName() != null
                ? PatientAnonymizer.maskGuardianName(result.patientCode())
                : result.guardianName();
        String guardianPhone = anonymizationModeState.isEnabled()
                ? PatientAnonymizer.maskPhone(result.guardianPhone())
                : result.guardianPhone();
        String consentSignerName = anonymizationModeState.isEnabled() && result.consentSignerName() != null
                ? (result.isMinor() ? PatientAnonymizer.maskGuardianName(result.patientCode()) : PatientAnonymizer.maskFullName(result.patientCode()))
                : result.consentSignerName();

        return new PatientResponse(
                result.id(),
                result.patientCode(),
                fullName,
                result.dateOfBirth(),
                result.gender(),
                phone,
                result.email(),
                address,
                result.identityNumber(),
                result.insuranceNumber(),
                result.bloodType(),
                emergencyContact,
                result.emergencyRelationship(),
                emergencyPhone,
                guardianName,
                result.guardianRelationship(),
                guardianPhone,
                result.guardianIdentityNumber(),
                result.guardianUserId(),
                consentSignerName,
                result.isMinor(),
                result.requiresAdultTransitionPrompt(),
                result.active(),
                result.createdAt(),
                result.updatedAt(),
                result.consentAgreed(),
                result.consentAgreedAt(),
                result.consentVersion(),
                result.consentWithdrawn(),
                result.consentWithdrawnAt(),
                result.consentWithdrawnReason(),
                result.nonMedicalUseRestricted(),
                result.status(),
                result.isMerged(),
                result.mergedIntoPatientId(),
                result.mergedIntoPatientCode(),
                result.mergedAt(),
                result.mergedBy(),
                result.mergeReason()
        );
    }

    public Page<PatientResponse> toResponse(Page<PatientResult> results) {
        return results.map(this::toResponse);
    }

    public SearchPatientCommand toCommand( SearchPatientRequest request, Pageable pageable) {
        return SearchPatientCommand.builder()
            .patientCode(request.patientCode())
            .fullName(request.fullName())
            .phone(request.phone())
            .identityNumber(request.identityNumber())
            .insuranceNumber(request.insuranceNumber())
            .dateOfBirth(request.dateOfBirth())
            .gender(request.gender())
            .active(request.active())
            .pageable(pageable)
            .build();
    }

    public MergePatientsCommand toMergeCommand(MergePatientsRequest request) {
        return new MergePatientsCommand(
                request.sourcePatientId(),
                request.targetPatientId(),
                request.reason()
        );
    }

    public MergePatientsResponse toMergeResponse(MergePatientsResult result) {
        return new MergePatientsResponse(
                result.sourcePatientId(),
                result.sourcePatientCode(),
                result.targetPatientId(),
                result.targetPatientCode(),
                result.transferredVisitsCount(),
                result.mergedBy(),
                result.reason(),
                result.mergedAt()
        );
    }

    public DuplicatePatientGroupResponse toDuplicateGroupResponse(DuplicatePatientGroupResult result) {
        return new DuplicatePatientGroupResponse(
                result.fullName(),
                result.dateOfBirth(),
                result.phone(),
                result.candidates().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.patient.PatientImportPreviewResponse toResponse(
            com.benhsoan.port.dto.result.patient.PatientImportPreviewResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.patient.PatientImportPreviewResponse(
                result.fileName(),
                result.totalRows(),
                result.validCount(),
                result.errorCount(),
                result.duplicateCount(),
                result.errors().stream().map(this::toResponse).toList(),
                result.suspectedDuplicates().stream().map(this::toResponse).toList()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.patient.PatientImportResultResponse toResponse(
            com.benhsoan.port.dto.result.patient.PatientImportResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.patient.PatientImportResultResponse(
                result.importLogId(),
                result.fileName(),
                result.totalRows(),
                result.successCount(),
                result.errorCount(),
                result.duplicateCount(),
                result.createdPatientCodes(),
                result.errors().stream().map(this::toResponse).toList()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.patient.PatientImportLogResponse toResponse(
            com.benhsoan.port.dto.result.patient.PatientImportLogResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.patient.PatientImportLogResponse(
                result.id(),
                result.fileName(),
                result.fileSize(),
                result.totalRows(),
                result.successRows(),
                result.errorRows(),
                result.duplicateRows(),
                result.status(),
                result.importedBy(),
                result.importedByName(),
                result.createdAt(),
                result.errors().stream().map(this::toResponse).toList()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.patient.PatientImportRowErrorResponse toResponse(
            com.benhsoan.port.dto.result.patient.PatientImportRowErrorResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.patient.PatientImportRowErrorResponse(
                result.rowNumber(),
                result.errorField(),
                result.errorMessage(),
                result.rawData()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.patient.SuspectedDuplicateResponse toResponse(
            com.benhsoan.port.dto.result.patient.SuspectedDuplicateResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.patient.SuspectedDuplicateResponse(
                result.rowNumber(),
                result.fullName(),
                result.dateOfBirth(),
                result.phone(),
                result.identityNumber(),
                result.matchedExistingPatientId(),
                result.matchedExistingPatientCode(),
                result.matchedExistingFullName(),
                result.duplicateReason()
        );
    }

}