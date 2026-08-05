package com.benhsoan.port.outbound.repository.medicine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicine.Medicine;
public interface MedicineRepository {

    Optional<Medicine> findById(UUID id);

    List<Medicine> findAllById(Collection<UUID> ids);

    Optional<Medicine> findByMedicineCode(String medicineCode);

    boolean existsByMedicineCode(String medicineCode);

    Optional<Medicine> findTopByOrderByMedicineCodeDesc();

    List<Medicine> findAllActive();
}
