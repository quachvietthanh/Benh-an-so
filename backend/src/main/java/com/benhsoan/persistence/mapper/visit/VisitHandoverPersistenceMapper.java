package com.benhsoan.persistence.mapper.visit;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.persistence.entity.visit.VisitHandoverEntity;

@Component
public class VisitHandoverPersistenceMapper {

    public VisitHandover toDomain(VisitHandoverEntity e) {
        if (e == null) {
            return null;
        }
        return VisitHandover.restore(
                e.getId(),
                e.getVisitId(),
                e.getFromDoctorId(),
                e.getToDoctorId(),
                e.getReason(),
                e.getHandedOverAt(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }

    public VisitHandoverEntity toEntity(VisitHandover d) {
        if (d == null) {
            return null;
        }
        return VisitHandoverEntity.builder()
                .id(d.getId())
                .visitId(d.getVisitId())
                .fromDoctorId(d.getFromDoctorId())
                .toDoctorId(d.getToDoctorId())
                .reason(d.getReason())
                .handedOverAt(d.getHandedOverAt())
                .createdBy(d.getCreatedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
