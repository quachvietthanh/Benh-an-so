package com.benhsoan.port.outbound.repository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.clinical.MedicalAttachment;
public interface MedicalAttachmentRepository {
    Optional<MedicalAttachment> findById(UUID id);
    MedicalAttachment save(MedicalAttachment attachment);
    boolean existsByClinicalResultId(UUID clinicalResultId);
    List<MedicalAttachment> findByClinicalResultId(UUID clinicalResultId);
    List<MedicalAttachment> findByClinicalResultIdIn(Collection<UUID> clinicalResultIds);
    List<MedicalAttachment> saveAll(Collection<MedicalAttachment> attachments);
}
