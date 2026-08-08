package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.persistence.entity.prescription.PrescriptionDispenseItemEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionDispenseItemRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionDispenseItemPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionDispenseItemRepositoryAdapter implements PrescriptionDispenseItemRepository {

    private final JpaPrescriptionDispenseItemRepository jpaRepository;
    private final PrescriptionDispenseItemPersistenceMapper mapper;

    @Override
    public PrescriptionDispenseItem save(PrescriptionDispenseItem dispenseItem) {
        PrescriptionDispenseItemEntity entity = mapper.toEntity(dispenseItem);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<PrescriptionDispenseItem> saveAll(List<PrescriptionDispenseItem> dispenseItems) {
        return jpaRepository.saveAll(
                        dispenseItems.stream()
                                .map(mapper::toEntity)
                                .toList()
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PrescriptionDispenseItem> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PrescriptionDispenseItem> findByPrescriptionId(UUID prescriptionId) {
        return jpaRepository.findByPrescriptionIdOrderByDispensedAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PrescriptionDispenseItem> findByPrescriptionItemId(UUID prescriptionItemId) {
        return jpaRepository.findByPrescriptionItemIdOrderByDispensedAtAsc(prescriptionItemId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
