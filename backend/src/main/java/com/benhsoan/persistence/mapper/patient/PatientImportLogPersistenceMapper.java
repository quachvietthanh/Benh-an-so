package com.benhsoan.persistence.mapper.patient;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.PatientImportLog;
import com.benhsoan.domain.patient.PatientImportRowError;
import com.benhsoan.persistence.entity.patient.PatientImportLogErrorEntity;
import com.benhsoan.persistence.entity.patient.PatientImportLogEntity;

@Component
public class PatientImportLogPersistenceMapper {

    public PatientImportLog toDomain(PatientImportLogEntity entity) {
        if (entity == null) {
            return null;
        }

        List<PatientImportRowError> domainErrors = new ArrayList<>();
        if (entity.getErrors() != null) {
            for (PatientImportLogErrorEntity errEntity : entity.getErrors()) {
                domainErrors.add(PatientImportRowError.reconstitute(
                        errEntity.getId(),
                        entity.getId(),
                        errEntity.getRowNumber(),
                        errEntity.getErrorField(),
                        errEntity.getErrorMessage(),
                        errEntity.getRawData()
                ));
            }
        }

        return PatientImportLog.reconstitute(
                entity.getId(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getTotalRows(),
                entity.getSuccessRows(),
                entity.getErrorRows(),
                entity.getDuplicateRows(),
                entity.getStatus(),
                entity.getImportedBy(),
                entity.getCreatedAt(),
                domainErrors
        );
    }

    public PatientImportLogEntity toEntity(PatientImportLog domain) {
        if (domain == null) {
            return null;
        }

        PatientImportLogEntity entity = PatientImportLogEntity.builder()
                .id(domain.getId())
                .fileName(domain.getFileName())
                .fileSize(domain.getFileSize())
                .totalRows(domain.getTotalRows())
                .successRows(domain.getSuccessRows())
                .errorRows(domain.getErrorRows())
                .duplicateRows(domain.getDuplicateRows())
                .status(domain.getStatus())
                .importedBy(domain.getImportedBy())
                .createdAt(domain.getCreatedAt())
                .errors(new ArrayList<>())
                .build();

        if (domain.getErrors() != null) {
            for (PatientImportRowError err : domain.getErrors()) {
                PatientImportLogErrorEntity errEntity = PatientImportLogErrorEntity.builder()
                        .id(err.getId())
                        .importLog(entity)
                        .rowNumber(err.getRowNumber())
                        .errorField(err.getErrorField())
                        .errorMessage(err.getErrorMessage())
                        .rawData(err.getRawData())
                        .build();
                entity.getErrors().add(errEntity);
            }
        }

        return entity;
    }
}
