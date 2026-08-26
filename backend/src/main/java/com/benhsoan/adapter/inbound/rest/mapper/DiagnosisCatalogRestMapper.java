package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateDiagnosisCatalogRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateDiagnosisCatalogRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.DiagnosisCatalogResponse;
import com.benhsoan.port.dto.command.medicalrecord.CreateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

@Component
public class DiagnosisCatalogRestMapper {

    public CreateDiagnosisCatalogCommand toCommand(CreateDiagnosisCatalogRequest request) {
        return new CreateDiagnosisCatalogCommand(
                request.code(), request.name(), request.diseaseGroup(), request.description()
        );
    }

    public UpdateDiagnosisCatalogCommand toCommand(UUID diagnosisCatalogId, UpdateDiagnosisCatalogRequest request) {
        return new UpdateDiagnosisCatalogCommand(
                diagnosisCatalogId, request.name(), request.diseaseGroup(), request.description()
        );
    }

    public DiagnosisCatalogResponse toResponse(DiagnosisCatalogResult result) {
        return new DiagnosisCatalogResponse(
                result.id(), result.code(), result.name(),
                result.diseaseGroup(),
                result.description(), result.active(),
                result.createdAt(), result.updatedAt()
        );
    }

    public List<DiagnosisCatalogResponse> toResponse(List<DiagnosisCatalogResult> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
