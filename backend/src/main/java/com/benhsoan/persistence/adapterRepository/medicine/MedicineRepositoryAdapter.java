package com.benhsoan.persistence.adapterRepository.medicine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.persistence.jpaRepository.medicine.JpaMedicineRepository;
import com.benhsoan.persistence.mapper.medicine.MedicinePersistenceMapper;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicineRepositoryAdapter
        implements MedicineRepository {

    private final JpaMedicineRepository jpaRepository;

    private final MedicinePersistenceMapper mapper;

    @Override
    public Optional<Medicine> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Medicine> findAllById(Collection<UUID> ids) {
        return jpaRepository.findAllById(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Medicine> findByMedicineCode(String medicineCode) {
        return jpaRepository.findByMedicineCode(medicineCode)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByMedicineCode(String medicineCode) {
        return jpaRepository.existsByMedicineCode(medicineCode);
    }

    @Override
    public Optional<Medicine> findTopByOrderByMedicineCodeDesc() {
        return jpaRepository.findTopByOrderByMedicineCodeDesc()
                .map(mapper::toDomain);
    }

    @Override
    public List<Medicine> findAllActive() {
        return jpaRepository.findAllByActiveTrueOrderByMedicineNameAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
