package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.medicalrecord.DiagnosisCatalogResponse;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

@Component
public class DiagnosisCatalogRestMapper {

    public DiagnosisCatalogResponse toResponse(DiagnosisCatalogResult result) {
        return new DiagnosisCatalogResponse(
                result.id(), result.code(), result.name(),
                result.description(), result.active(),
                result.createdAt(), result.updatedAt()
        );
    }

    public List<DiagnosisCatalogResponse> toResponse(List<DiagnosisCatalogResult> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
