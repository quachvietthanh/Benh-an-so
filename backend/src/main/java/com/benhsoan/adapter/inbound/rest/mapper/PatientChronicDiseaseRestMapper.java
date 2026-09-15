package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.patient.AddPatientChronicDiseaseRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientChronicDiseaseResponse;
import com.benhsoan.port.dto.command.patient.AddPatientChronicDiseaseCommand;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;

@Component
public class PatientChronicDiseaseRestMapper {

    public AddPatientChronicDiseaseCommand toCommand(UUID patientId, AddPatientChronicDiseaseRequest request) {
        if (request == null) {
            return null;
        }
        return AddPatientChronicDiseaseCommand.builder()
                .patientId(patientId)
                .diagnosisCatalogId(request.diagnosisCatalogId())
                .yearDetected(request.yearDetected())
                .notes(request.notes())
                .visitId(request.visitId())
                .build();
    }

    public PatientChronicDiseaseResponse toResponse(PatientChronicDiseaseResult result) {
        if (result == null) {
            return null;
        }
        return new PatientChronicDiseaseResponse(
                result.id(),
                result.patientId(),
                result.diagnosisCatalogId(),
                result.yearDetected(),
                result.notes(),
                result.active(),
                result.createdBy(),
                result.createdAt(),
                result.updatedBy(),
                result.updatedAt()
        );
    }
}
