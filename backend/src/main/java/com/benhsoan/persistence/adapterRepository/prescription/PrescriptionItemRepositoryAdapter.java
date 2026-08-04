package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.persistence.entity.prescription.PrescriptionItemEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionItemRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionItemPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionItemRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionItemRepositoryAdapter
        implements PrescriptionItemRepository {

    private final JpaPrescriptionItemRepository jpaRepository;

    private final PrescriptionItemPersistenceMapper mapper;

    @Override
    public Optional<PrescriptionItem> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public PrescriptionItem save(PrescriptionItem prescriptionItem) {
        PrescriptionItemEntity entity = mapper.toEntity(prescriptionItem);
        PrescriptionItemEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<PrescriptionItem> findByPrescriptionId(UUID prescriptionId) {
        return jpaRepository
                .findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByPrescriptionId(UUID prescriptionId) {
        if (prescriptionId == null) {
            return;
        }

        jpaRepository.deleteAllByPrescriptionId(prescriptionId);
    }
}
