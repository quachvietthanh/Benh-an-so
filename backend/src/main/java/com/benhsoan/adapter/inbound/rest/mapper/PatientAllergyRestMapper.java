package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.patient.AddPatientAllergyRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientAllergyRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientAllergyChangeLogResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientAllergyResponse;
import com.benhsoan.port.dto.command.patient.AddPatientAllergyCommand;
import com.benhsoan.port.dto.command.patient.UpdatePatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyChangeLogResult;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;

@Component
public class PatientAllergyRestMapper {

    public AddPatientAllergyCommand toCommand(UUID patientId, AddPatientAllergyRequest request) {
        if (request == null) {
            return null;
        }

        return AddPatientAllergyCommand.builder()
                .patientId(patientId)
                .allergenType(request.allergenType())
                .allergenName(request.allergenName())
                .severity(request.severity())
                .reaction(request.reaction())
                .notes(request.notes())
                .visitId(request.visitId())
                .build();
    }

    public UpdatePatientAllergyCommand toCommand(UUID patientId, UUID allergyId, UpdatePatientAllergyRequest request) {
        if (request == null) {
            return null;
        }

        return UpdatePatientAllergyCommand.builder()
                .allergyId(allergyId)
                .patientId(patientId)
                .allergenName(request.allergenName())
                .severity(request.severity())
                .reaction(request.reaction())
                .notes(request.notes())
                .changeReason(request.changeReason())
                .build();
    }

    public PatientAllergyResponse toResponse(PatientAllergyResult result) {
        if (result == null) {
            return null;
        }

        return new PatientAllergyResponse(
                result.id(),
                result.patientId(),
                result.allergenType(),
                result.allergenName(),
                result.severity(),
                result.reaction(),
                result.notes(),
                result.active(),
                result.createdBy(),
                result.createdAt(),
                result.updatedBy(),
                result.updatedAt()
        );
    }

    public PatientAllergyChangeLogResponse toResponse(PatientAllergyChangeLogResult result) {
        if (result == null) {
            return null;
        }

        return new PatientAllergyChangeLogResponse(
                result.id(),
                result.allergyId(),
                result.patientId(),
                result.action(),
                result.beforeData(),
                result.afterData(),
                result.changeReason(),
                result.changedBy(),
                result.changedAt()
        );
    }
}
