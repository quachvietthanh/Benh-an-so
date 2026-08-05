package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.persistence.entity.prescription.PrescriptionEntity;
import com.benhsoan.persistence.entity.prescription.PrescriptionItemEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionItemRepository;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionItemPersistenceMapper;
import com.benhsoan.persistence.mapper.prescription.PrescriptionPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionRepositoryAdapter
        implements PrescriptionRepository {

    private final JpaPrescriptionRepository jpaRepository;

    private final JpaPrescriptionItemRepository itemJpaRepository;

    private final PrescriptionPersistenceMapper mapper;

    private final PrescriptionItemPersistenceMapper itemMapper;

    @Override
    @Transactional
    public Prescription save(Prescription prescription) {
        PrescriptionEntity savedEntity = jpaRepository.save(
                mapper.toEntity(prescription)
        );

        itemJpaRepository.deleteAllByPrescriptionId(savedEntity.getId());

        List<PrescriptionItemEntity> itemEntities = prescription.getItems()
                .stream()
                .map(itemMapper::toEntity)
                .toList();

        List<PrescriptionItemEntity> savedItemEntities = itemJpaRepository.saveAll(itemEntities);

        return mapper.toDomain(savedEntity, savedItemEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> findByPrescriptionCode(
            String prescriptionCode
    ) {
        return jpaRepository.findByPrescriptionCode(prescriptionCode)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByPrescriptionCode(String prescriptionCode) {
        return jpaRepository.existsByPrescriptionCode(prescriptionCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> findTopByOrderByPrescriptionCodeDesc() {
        return jpaRepository.findTopByOrderByPrescriptionCodeDesc()
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> findByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository
                .findByMedicalRecordIdOrderByPrescribedAtDesc(medicalRecordId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Optional<Prescription> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id)
                .map(this::toDomain);
    }

    private Prescription toDomain(PrescriptionEntity entity) {
        List<PrescriptionItemEntity> itemEntities = itemJpaRepository
                .findByPrescriptionIdOrderByCreatedAtAsc(entity.getId());

        return mapper.toDomain(entity, itemEntities);
    }
}
