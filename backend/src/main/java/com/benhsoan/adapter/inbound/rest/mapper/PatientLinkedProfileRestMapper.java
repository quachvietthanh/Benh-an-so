package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.patient.LinkedPatientProfileResponse;
import com.benhsoan.port.dto.result.patient.LinkedPatientProfileResult;

@Component
public class PatientLinkedProfileRestMapper {

    public LinkedPatientProfileResponse toResponse(LinkedPatientProfileResult result) {
        return new LinkedPatientProfileResponse(
                result.patientId(),
                result.patientCode(),
                result.fullName(),
                result.dateOfBirth(),
                result.age(),
                result.isMinor(),
                result.relationship(),
                result.self(),
                result.requiresGuardianLinkReview()
        );
    }
}
