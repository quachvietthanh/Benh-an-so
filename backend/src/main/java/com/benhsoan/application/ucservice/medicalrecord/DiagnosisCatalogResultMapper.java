package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

@Component
public class DiagnosisCatalogResultMapper {

    public DiagnosisCatalogResult toResult(DiagnosisCatalog catalog) {
        return new DiagnosisCatalogResult(
                catalog.getId(), catalog.getCode(), catalog.getName(), catalog.getDiseaseGroup(),
                catalog.getDescription(), catalog.isActive(), catalog.getCreatedAt(), catalog.getUpdatedAt()
        );
    }
}
