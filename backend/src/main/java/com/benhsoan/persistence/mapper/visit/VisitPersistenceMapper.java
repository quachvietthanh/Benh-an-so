package com.benhsoan.persistence.mapper.visit;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.persistence.entity.visit.VisitEntity;

@Component
public class VisitPersistenceMapper {

    public Visit toDomain(VisitEntity e) {
        return e == null ? null : Visit.restore(e.getId(), e.getVisitCode(), e.getPatientId(), e.getDoctorId(), e.getAppointmentId(), e.getQueueId(), e.getVisitType(), e.getStatus(), e.getVisitAt(), e.getStartedAt(), e.getCompletedAt(), e.getReason(), e.getNote(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public VisitEntity toEntity(Visit d) {
        return d == null ? null : VisitEntity.builder().id(d.getId()).visitCode(d.getVisitCode()).patientId(d.getPatientId()).doctorId(d.getDoctorId()).appointmentId(d.getAppointmentId()).queueId(d.getQueueId()).visitType(d.getVisitType()).status(d.getStatus()).visitAt(d.getVisitAt()).startedAt(d.getStartedAt()).completedAt(d.getCompletedAt()).reason(d.getReason()).note(d.getNote()).createdBy(d.getCreatedBy()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
