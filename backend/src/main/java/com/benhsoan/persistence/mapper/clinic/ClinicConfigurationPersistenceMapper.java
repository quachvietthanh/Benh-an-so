package com.benhsoan.persistence.mapper.clinic;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.persistence.entity.clinic.ClinicConfigurationEntity;

@Component
public class ClinicConfigurationPersistenceMapper {

    public ClinicConfiguration toDomain(ClinicConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }

        return ClinicConfiguration.restore(
                entity.getId(),
                entity.getClinicName(),
                entity.getAddress(),
                entity.getPhone(),
                entity.getOpeningTime(),
                entity.getClosingTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ClinicConfigurationEntity toEntity(ClinicConfiguration domain) {
        if (domain == null) {
            return null;
        }

        return ClinicConfigurationEntity.builder()
                .id(domain.getId())
                .clinicName(domain.getClinicName())
                .address(domain.getAddress())
                .phone(domain.getPhone())
                .openingTime(domain.getOpeningTime())
                .closingTime(domain.getClosingTime())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
