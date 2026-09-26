package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.MedicationReturn;
import com.benhsoan.persistence.entity.prescription.MedicationReturnEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaMedicationReturnRepository;
import com.benhsoan.persistence.mapper.prescription.MedicationReturnPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.MedicationReturnRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicationReturnRepositoryAdapter implements MedicationReturnRepository {

    private final JpaMedicationReturnRepository jpaRepository;
    private final MedicationReturnPersistenceMapper mapper;

    @Override
    public MedicationReturn save(MedicationReturn medicationReturn) {
        MedicationReturnEntity entity = mapper.toEntity(medicationReturn);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<MedicationReturn> findByPrescriptionId(UUID prescriptionId) {
        return jpaRepository.findByPrescriptionIdOrderByReturnedAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
