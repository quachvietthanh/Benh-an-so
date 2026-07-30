package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.persistence.jpaRepository.clinical.JpaMedicalAttachmentRepository;
import com.benhsoan.persistence.mapper.clinical.MedicalAttachmentPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.MedicalAttachmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalAttachmentRepositoryAdapter implements MedicalAttachmentRepository {
    private final JpaMedicalAttachmentRepository jpaRepository;
    private final MedicalAttachmentPersistenceMapper mapper;
    public Optional<MedicalAttachment> findById(UUID id) { return jpaRepository.findById(id).map(mapper::toDomain); }
    public MedicalAttachment save(MedicalAttachment attachment) { return mapper.toDomain(jpaRepository.save(mapper.toEntity(attachment))); }
    public void deleteById(UUID id) { if (id != null) jpaRepository.deleteById(id); }
    public List<MedicalAttachment> findByClinicalResultId(UUID resultId) { return jpaRepository.findByClinicalResultIdOrderByUploadedAtDesc(resultId).stream().map(mapper::toDomain).toList(); }
    public List<MedicalAttachment> findByClinicalResultIdIn(Collection<UUID> resultIds) {
        return resultIds == null || resultIds.isEmpty() ? List.of() : jpaRepository.findByClinicalResultIdIn(resultIds).stream().map(mapper::toDomain).toList();
    }
    public List<MedicalAttachment> saveAll(Collection<MedicalAttachment> attachments) {
        return attachments == null || attachments.isEmpty() ? List.of() : jpaRepository.saveAll(attachments.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();
    }
}
