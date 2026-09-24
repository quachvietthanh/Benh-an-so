package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionTemplate;
import com.benhsoan.persistence.entity.prescription.PrescriptionTemplateEntity;
import com.benhsoan.persistence.entity.prescription.PrescriptionTemplateItemEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionTemplateItemRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionTemplateRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionTemplatePersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionTemplateRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionTemplateRepositoryAdapter
        implements PrescriptionTemplateRepository {

    private final JpaPrescriptionTemplateRepository jpaRepository;
    private final JpaPrescriptionTemplateItemRepository itemJpaRepository;
    private final PrescriptionTemplatePersistenceMapper mapper;

    @Override
    @Transactional
    public PrescriptionTemplate save(PrescriptionTemplate template) {
        PrescriptionTemplateEntity savedEntity = jpaRepository.save(mapper.toEntity(template));

        itemJpaRepository.deleteAllByTemplateId(savedEntity.getId());
        List<PrescriptionTemplateItemEntity> savedItems = itemJpaRepository.saveAll(
                template.getItems().stream().map(mapper::toEntityItem).toList()
        );

        return mapper.toDomain(savedEntity, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrescriptionTemplate> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionTemplate> findByDiagnosisCatalogIdAndCreatedBy(
            UUID diagnosisCatalogId,
            UUID createdBy
    ) {
        return jpaRepository
                .findByDiagnosisCatalogIdAndCreatedByOrderByCreatedAtDesc(diagnosisCatalogId, createdBy)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionTemplate> findByDiagnosisCatalogId(UUID diagnosisCatalogId) {
        return jpaRepository
                .findByDiagnosisCatalogIdOrderByCreatedAtDesc(diagnosisCatalogId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private PrescriptionTemplate toDomain(PrescriptionTemplateEntity entity) {
        List<PrescriptionTemplateItemEntity> items = itemJpaRepository
                .findByTemplateIdOrderBySortOrderAsc(entity.getId());
        return mapper.toDomain(entity, items);
    }
}
