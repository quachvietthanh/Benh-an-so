package com.benhsoan.port.outbound.repository.crudRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface MedicalAttachmentRepository extends BaseRepository<MedicalAttachment, UUID> {
    List<MedicalAttachment> findByClinicalResultId(UUID clinicalResultId);
    List<MedicalAttachment> findByClinicalResultIdIn(Collection<UUID> clinicalResultIds);
    List<MedicalAttachment> saveAll(Collection<MedicalAttachment> attachments);
}
