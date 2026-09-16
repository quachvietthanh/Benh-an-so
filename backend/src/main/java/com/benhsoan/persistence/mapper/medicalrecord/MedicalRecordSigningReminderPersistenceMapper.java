package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordSigningReminderEntity;

@Component
public class MedicalRecordSigningReminderPersistenceMapper {

    public MedicalRecordSigningReminder toDomain(MedicalRecordSigningReminderEntity entity) {
        if (entity == null) {
            return null;
        }

        return MedicalRecordSigningReminder.restore(
                entity.getId(),
                entity.getMedicalRecordId(),
                entity.getDoctorId(),
                entity.getRemindedBy(),
                entity.getRemindedAt(),
                entity.getOverdueHours(),
                entity.getChannel(),
                entity.getNotes(),
                entity.getStatus()
        );
    }

    public MedicalRecordSigningReminderEntity toEntity(MedicalRecordSigningReminder domain) {
        if (domain == null) {
            return null;
        }

        return MedicalRecordSigningReminderEntity.builder()
                .id(domain.getId())
                .medicalRecordId(domain.getMedicalRecordId())
                .doctorId(domain.getDoctorId())
                .remindedBy(domain.getRemindedBy())
                .remindedAt(domain.getRemindedAt())
                .overdueHours(domain.getOverdueHours())
                .channel(domain.getChannel())
                .notes(domain.getNotes())
                .status(domain.getStatus())
                .build();
    }
}
