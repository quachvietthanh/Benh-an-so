package com.benhsoan.persistence.mapper.followup;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.followup.FollowUpReminder;
import com.benhsoan.persistence.entity.followup.FollowUpReminderEntity;

@Component
public class FollowUpReminderPersistenceMapper {

    public FollowUpReminder toDomain(FollowUpReminderEntity entity) {
        if (entity == null) {
            return null;
        }
        return FollowUpReminder.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getVisitId(),
                entity.getAppointmentId(),
                entity.getFollowUpDate(),
                entity.getRemindAt(),
                entity.getReminderType(),
                entity.getStatus(),
                entity.getNotes(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    public FollowUpReminderEntity toEntity(FollowUpReminder domain) {
        if (domain == null) {
            return null;
        }
        return FollowUpReminderEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .visitId(domain.getVisitId())
                .appointmentId(domain.getAppointmentId())
                .followUpDate(domain.getFollowUpDate())
                .remindAt(domain.getRemindAt())
                .reminderType(domain.getReminderType())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
