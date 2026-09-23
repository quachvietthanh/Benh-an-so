package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentWaitlist;
import com.benhsoan.persistence.entity.appointment.AppointmentWaitlistEntity;

@Component
public class AppointmentWaitlistPersistenceMapper {

    public AppointmentWaitlist toDomain(AppointmentWaitlistEntity entity) {
        if (entity == null) {
            return null;
        }

        return AppointmentWaitlist.reconstitute(
                entity.getId(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getDesiredDate(),
                entity.getTimePreference(),
                entity.getStatus(),
                entity.getNote(),
                entity.getCancelReason(),
                entity.getBookedAppointmentId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AppointmentWaitlistEntity toEntity(AppointmentWaitlist domain) {
        if (domain == null) {
            return null;
        }

        return AppointmentWaitlistEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .doctorId(domain.getDoctorId())
                .desiredDate(domain.getDesiredDate())
                .timePreference(domain.getTimePreference())
                .status(domain.getStatus())
                .note(domain.getNote())
                .cancelReason(domain.getCancelReason())
                .bookedAppointmentId(domain.getBookedAppointmentId())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
