package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordSigningReminder;

public interface MedicalRecordSigningReminderRepository {

    MedicalRecordSigningReminder save(MedicalRecordSigningReminder reminder);

    List<MedicalRecordSigningReminder> findByMedicalRecordId(UUID medicalRecordId);

    long countByMedicalRecordId(UUID medicalRecordId);

    Optional<MedicalRecordSigningReminder> findLatestByMedicalRecordId(UUID medicalRecordId);
}
