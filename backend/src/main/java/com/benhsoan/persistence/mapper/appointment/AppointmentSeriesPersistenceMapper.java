package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentSeries;
import com.benhsoan.persistence.entity.appointment.AppointmentSeriesEntity;

@Component
public class AppointmentSeriesPersistenceMapper {

    public AppointmentSeries toDomain(AppointmentSeriesEntity entity) {
        if (entity == null) {
            return null;
        }

        return AppointmentSeries.restore(
                entity.getId(),
                entity.getSeriesCode(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getMedicalRecordId(),
                entity.getTotalSessions(),
                entity.getIntervalDays(),
                entity.getTitle(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AppointmentSeriesEntity toEntity(AppointmentSeries domain) {
        if (domain == null) {
            return null;
        }

        return AppointmentSeriesEntity.builder()
                .id(domain.getId())
                .seriesCode(domain.getSeriesCode())
                .patientId(domain.getPatientId())
                .doctorId(domain.getDoctorId())
                .medicalRecordId(domain.getMedicalRecordId())
                .totalSessions(domain.getTotalSessions())
                .intervalDays(domain.getIntervalDays())
                .title(domain.getTitle())
                .notes(domain.getNotes())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
