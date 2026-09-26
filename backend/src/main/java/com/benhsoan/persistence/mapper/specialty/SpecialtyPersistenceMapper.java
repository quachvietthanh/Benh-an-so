package com.benhsoan.persistence.mapper.specialty;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.persistence.entity.specialty.SpecialtyEntity;

@Component
public class SpecialtyPersistenceMapper {

    public Specialty toDomain(SpecialtyEntity entity) {
        if (entity == null) {
            return null;
        }
        return Specialty.restore(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SpecialtyEntity toEntity(Specialty domain) {
        if (domain == null) {
            return null;
        }
        return SpecialtyEntity.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .name(domain.getName())
                .nameKey(domain.getNameKey())
                .description(domain.getDescription())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
