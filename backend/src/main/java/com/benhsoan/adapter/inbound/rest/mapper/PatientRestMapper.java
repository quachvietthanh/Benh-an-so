package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.patient.RegisterPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.SearchPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.port.dto.command.patient.RegisterPatientCommand;
import com.benhsoan.port.dto.command.patient.SearchPatientCommand;
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;

@Component
public class PatientRestMapper {

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
                .emergencyPhone(request.emergencyPhone())
                .consentAgreed(request.consentAgreed())
                .consentVersion(request.consentVersion())
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
                .emergencyPhone(request.emergencyPhone())
                .active(request.active())
                .consentAgreed(request.consentAgreed())
                .consentWithdrawn(request.consentWithdrawn())
                .consentWithdrawnReason(request.consentWithdrawnReason())
                .consentVersion(request.consentVersion())
                .build();
    }

    public PatientResponse toResponse(PatientResult result) {

        return new PatientResponse(
                result.id(),
                result.patientCode(),
                result.fullName(),
                result.dateOfBirth(),
                result.gender(),
                result.phone(),
                result.email(),
                result.address(),
                result.identityNumber(),
                result.insuranceNumber(),
                result.bloodType(),
                result.emergencyContact(),
                result.emergencyPhone(),
                result.active(),
                result.createdAt(),
                result.updatedAt(),
                result.consentAgreed(),
                result.consentAgreedAt(),
                result.consentVersion(),
                result.consentWithdrawn(),
                result.consentWithdrawnAt(),
                result.consentWithdrawnReason(),
                result.nonMedicalUseRestricted()
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

}