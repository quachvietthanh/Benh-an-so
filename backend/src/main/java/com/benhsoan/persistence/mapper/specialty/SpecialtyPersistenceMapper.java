package com.benhsoan.persistence.mapper.specialty;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.persistence.entity.specialty.SpecialtyEntity;

@Component
public class SpecialtyPersistenceMapper {

    public Specialty toDomain(SpecialtyEntity entity) {
        return entity == null ? null : Specialty.restore(entity.getId(), entity.getCode(), entity.getName(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
