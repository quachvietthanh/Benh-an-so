package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.persistence.entity.medicalrecord.DiagnosisCatalogEntity;

@Component
public class DiagnosisCatalogPersistenceMapper {

    public DiagnosisCatalog toDomain(DiagnosisCatalogEntity e) {
        return e == null ? null : DiagnosisCatalog.restore(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public DiagnosisCatalogEntity toEntity(DiagnosisCatalog d) {
        return d == null ? null : DiagnosisCatalogEntity.builder().id(d.getId()).code(d.getCode()).name(d.getName()).description(d.getDescription()).active(d.isActive()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
