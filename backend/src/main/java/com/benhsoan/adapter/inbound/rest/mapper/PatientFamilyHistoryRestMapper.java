package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.patient.AddPatientFamilyHistoryRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientFamilyHistoryResponse;
import com.benhsoan.port.dto.command.patient.AddPatientFamilyHistoryCommand;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;

@Component
public class PatientFamilyHistoryRestMapper {

    public AddPatientFamilyHistoryCommand toCommand(UUID patientId, AddPatientFamilyHistoryRequest request) {
        if (request == null) {
            return null;
        }
        return AddPatientFamilyHistoryCommand.builder()
                .patientId(patientId)
                .relationship(request.relationship())
                .diagnosisCatalogId(request.diagnosisCatalogId())
                .notes(request.notes())
                .visitId(request.visitId())
                .build();
    }

    public PatientFamilyHistoryResponse toResponse(PatientFamilyHistoryResult result) {
        if (result == null) {
            return null;
        }
        return new PatientFamilyHistoryResponse(
                result.id(),
                result.patientId(),
                result.relationship(),
                result.diagnosisCatalogId(),
                result.notes(),
                result.active(),
                result.createdBy(),
                result.createdAt(),
                result.updatedBy(),
                result.updatedAt()
        );
    }
}
