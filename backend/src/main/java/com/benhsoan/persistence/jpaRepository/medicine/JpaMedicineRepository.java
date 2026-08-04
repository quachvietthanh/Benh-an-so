package com.benhsoan.persistence.jpaRepository.medicine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.medicine.MedicineEntity;

public interface JpaMedicineRepository
        extends JpaRepository<MedicineEntity, UUID> {

    Optional<MedicineEntity> findByMedicineCode(String medicineCode);

    boolean existsByMedicineCode(String medicineCode);

    Optional<MedicineEntity> findTopByOrderByMedicineCodeDesc();

    List<MedicineEntity> findAllByActiveTrueOrderByMedicineNameAsc();
}
