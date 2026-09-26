package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionItem;
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
