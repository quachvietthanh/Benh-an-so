package com.benhsoan.port.outbound.repository.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;

public interface MedicalRecordTemplateRepository {

    MedicalRecordTemplate save(MedicalRecordTemplate template);

    Optional<MedicalRecordTemplate> findById(UUID id);

    List<MedicalRecordTemplate> findBySpecialtyIdAndActive(UUID specialtyId, boolean active);

    List<MedicalRecordTemplate> search(UUID specialtyId, Boolean active);

    boolean existsBySpecialtyIdAndNameKey(UUID specialtyId, String nameKey);

    boolean existsBySpecialtyIdAndNameKeyAndIdNot(UUID specialtyId, String nameKey, UUID templateId);

    MedicalRecordTemplate setDefault(UUID templateId, UUID updatedBy, Instant updatedAt);

    MedicalRecordTemplate deactivate(UUID templateId, UUID replacementTemplateId, UUID updatedBy, Instant updatedAt);
}
