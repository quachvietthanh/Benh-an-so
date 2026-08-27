package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateSection;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultReplacementRequiredException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInvalidReplacementException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordTemplateRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordTemplateSectionRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordTemplateVersionRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordTemplatePersistenceMapper;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordTemplateSectionPersistenceMapper;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordTemplateVersionPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordTemplateRepositoryAdapter implements MedicalRecordTemplateRepository {

    private final JpaMedicalRecordTemplateRepository templateJpaRepository;
    private final JpaMedicalRecordTemplateVersionRepository versionJpaRepository;
    private final JpaMedicalRecordTemplateSectionRepository sectionJpaRepository;
    private final MedicalRecordTemplatePersistenceMapper templateMapper;
    private final MedicalRecordTemplateVersionPersistenceMapper versionMapper;
    private final MedicalRecordTemplateSectionPersistenceMapper sectionMapper;

    @Override
    @Transactional
    public MedicalRecordTemplate save(MedicalRecordTemplate template) {
        if (template.isDefaultTemplate()) {
            templateJpaRepository.findActiveBySpecialtyIdForUpdate(template.getSpecialtyId());
            UUID changedBy = template.getUpdatedBy() == null ? template.getCreatedBy() : template.getUpdatedBy();
            Instant changedAt = template.getUpdatedAt() == null ? template.getCreatedAt() : template.getUpdatedAt();
            templateJpaRepository.clearDefaultBySpecialtyId(template.getSpecialtyId(), changedBy, changedAt);
        }
        saveAggregate(template);
        return findRequired(template.getId());
    }

    @Override
    public Optional<MedicalRecordTemplate> findById(UUID id) {
        return templateJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<MedicalRecordTemplate> findByIdForUpdate(UUID id) {
        return templateJpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Optional<MedicalRecordTemplateVersion> findVersionById(UUID id) {
        return versionJpaRepository.findById(id).map(version -> versionMapper.toDomain(version,
                sectionJpaRepository.findByTemplateVersionIdOrderByDisplayOrderAsc(version.getId()).stream()
                        .map(sectionMapper::toDomain).toList()));
    }

    @Override
    public List<MedicalRecordTemplate> findBySpecialtyIdAndActive(UUID specialtyId, boolean active) {
        return templateJpaRepository.findBySpecialtyIdAndActiveOrderByNameAsc(specialtyId, active).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MedicalRecordTemplate> search(UUID specialtyId, Boolean active) {
        return templateJpaRepository.search(specialtyId, active).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsBySpecialtyIdAndNameKey(UUID specialtyId, String nameKey) {
        return templateJpaRepository.existsBySpecialtyIdAndNameKey(specialtyId, nameKey);
    }

    @Override
    public boolean existsBySpecialtyIdAndNameKeyAndIdNot(UUID specialtyId, String nameKey, UUID templateId) {
        return templateJpaRepository.existsBySpecialtyIdAndNameKeyAndIdNot(specialtyId, nameKey, templateId);
    }

    @Override
    @Transactional
    public MedicalRecordTemplate setDefault(UUID templateId, UUID updatedBy, Instant updatedAt) {
        MedicalRecordTemplate template = findRequiredForUpdate(templateId);
        templateJpaRepository.findActiveBySpecialtyIdForUpdate(template.getSpecialtyId());
        template.setDefault(updatedBy, updatedAt);
        templateJpaRepository.clearDefaultBySpecialtyId(template.getSpecialtyId(), updatedBy, updatedAt);
        templateJpaRepository.saveAndFlush(templateMapper.toEntity(template));
        return findRequired(templateId);
    }

    @Override
    @Transactional
    public MedicalRecordTemplate deactivate(UUID templateId, UUID replacementTemplateId, UUID updatedBy,
            Instant updatedAt) {
        MedicalRecordTemplate template = findRequiredForUpdate(templateId);
        List<MedicalRecordTemplateEntity> activeTemplates = templateJpaRepository
                .findActiveBySpecialtyIdForUpdate(template.getSpecialtyId());

        MedicalRecordTemplate replacement = null;
        if (template.isDefaultTemplate() && replacementTemplateId != null) {
            replacement = activeTemplates.stream().filter(candidate -> candidate.getId().equals(replacementTemplateId))
                    .findFirst().map(this::toDomain).orElseThrow(MedicalRecordTemplateInvalidReplacementException::new);
        }

        template.deactivate(activeTemplates.size(), replacementTemplateId, updatedBy, updatedAt);
        if (template.isDefaultTemplate() || replacement != null) {
            templateJpaRepository.clearDefaultBySpecialtyId(template.getSpecialtyId(), updatedBy, updatedAt);
        }
        templateJpaRepository.saveAndFlush(templateMapper.toEntity(template));
        if (replacement != null) {
            replacement.setDefault(updatedBy, updatedAt);
            templateJpaRepository.saveAndFlush(templateMapper.toEntity(replacement));
        }
        return findRequired(templateId);
    }

    private void saveAggregate(MedicalRecordTemplate template) {
        templateJpaRepository.saveAndFlush(templateMapper.toEntity(template));
        MedicalRecordTemplateVersion currentVersion = template.getCurrentVersion();
        versionJpaRepository.saveAndFlush(versionMapper.toEntity(currentVersion));
        sectionJpaRepository.saveAllAndFlush(currentVersion.getSections().stream().map(sectionMapper::toEntity).toList());
    }

    private MedicalRecordTemplate findRequired(UUID id) {
        return findById(id).orElseThrow(() -> new MedicalRecordTemplateNotFoundException(id));
    }

    private MedicalRecordTemplate findRequiredForUpdate(UUID id) {
        MedicalRecordTemplateEntity entity = templateJpaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(id));
        return toDomain(entity);
    }

    private MedicalRecordTemplate toDomain(MedicalRecordTemplateEntity entity) {
        List<MedicalRecordTemplateVersion> versions = versionJpaRepository
                .findByTemplateIdOrderByVersionNoAsc(entity.getId()).stream()
                .map(version -> versionMapper.toDomain(version, sectionJpaRepository
                        .findByTemplateVersionIdOrderByDisplayOrderAsc(version.getId()).stream()
                        .map(sectionMapper::toDomain).toList()))
                .toList();
        return MedicalRecordTemplate.restore(entity.getId(), entity.getSpecialtyId(), entity.getName(), entity.isActive(),
                entity.isDefaultTemplate(), entity.getCurrentVersionNo(), entity.getCreatedBy(), entity.getCreatedAt(),
                entity.getUpdatedBy(), entity.getUpdatedAt(), versions);
    }
}
