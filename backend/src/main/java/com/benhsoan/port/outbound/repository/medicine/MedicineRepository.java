package com.benhsoan.port.outbound.repository.medicine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.medicine.Medicine;

public interface MedicineRepository {

    Optional<Medicine> findById(UUID id);

    List<Medicine> findAllByIds(Collection<UUID> ids);

    Optional<Medicine> findByMedicineCode(String medicineCode);

    boolean existsByMedicineCode(String medicineCode);

    boolean existsByMedicineNameAndActiveIngredient(
            String medicineName,
            String activeIngredient,
            UUID excludedId
    );

    Optional<Medicine> findTopByOrderByMedicineCodeDesc();

    List<Medicine> findAllActive();

    Page<Medicine> search(MedicineSearchCriteria criteria, Pageable pageable);

    Medicine save(Medicine medicine);
}
