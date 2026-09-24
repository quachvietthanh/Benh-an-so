package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.patient.RequestPatientDataErasureRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientConsentRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.DataErasureResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientConsentHistoryResponse;
import com.benhsoan.port.dto.command.patient.RequestPatientDataErasureCommand;
import com.benhsoan.port.dto.command.patient.UpdatePatientConsentCommand;
import com.benhsoan.port.dto.result.patient.DataErasureResult;
import com.benhsoan.port.dto.result.patient.PatientConsentHistoryResult;

@Component
public class PatientConsentRestMapper {

    public UpdatePatientConsentCommand toCommand(UpdatePatientConsentRequest request) {
        if (request == null) {
            return null;
        }

        return UpdatePatientConsentCommand.builder()
                .consentAgreed(request.consentAgreed())
                .consentWithdrawn(request.consentWithdrawn())
                .consentWithdrawnReason(request.consentWithdrawnReason())
                .consentVersion(request.consentVersion())
                .scopes(request.scopes())
                .requestDataErasure(request.requestDataErasure())
                .build();
    }

    public RequestPatientDataErasureCommand toErasureCommand(RequestPatientDataErasureRequest request) {
        String reason = request != null ? request.reason() : null;
        return RequestPatientDataErasureCommand.builder()
                .reason(reason)
                .build();
    }

    public PatientConsentHistoryResponse toHistoryResponse(PatientConsentHistoryResult result) {
        if (result == null) {
            return null;
        }

        return PatientConsentHistoryResponse.builder()
                .id(result.id())
                .patientId(result.patientId())
                .versionNumber(result.versionNumber())
                .versionCode(result.versionCode())
                .status(result.status() != null ? result.status().name() : null)
                .statusDescription(result.status() != null ? result.status().getDescription() : null)
                .scopes(result.scopes())
                .consentAgreed(result.consentAgreed())
                .consentAgreedAt(result.consentAgreedAt())
                .consentWithdrawn(result.consentWithdrawn())
                .consentWithdrawnAt(result.consentWithdrawnAt())
                .consentWithdrawnReason(result.consentWithdrawnReason())
                .nonMedicalUseRestricted(result.nonMedicalUseRestricted())
                .signerName(result.signerName())
                .createdBy(result.createdBy())
                .createdAt(result.createdAt())
                .build();
    }

    public DataErasureResponse toErasureResponse(DataErasureResult result) {
        if (result == null) {
            return null;
        }

        return DataErasureResponse.builder()
                .patientId(result.patientId())
                .consentWithdrawn(result.consentWithdrawn())
                .nonMedicalUseRestricted(result.nonMedicalUseRestricted())
                .medicalRecordsRetained(result.medicalRecordsRetained())
                .retentionYears(result.retentionYears())
                .message(result.message())
                .build();
    }
}
