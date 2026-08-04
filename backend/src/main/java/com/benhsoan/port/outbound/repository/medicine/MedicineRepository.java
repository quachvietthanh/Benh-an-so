package com.benhsoan.port.outbound.repository.medicine;

import java.util.List;
import java.util.Optional;

import com.benhsoan.domain.medicine.Medicine;
public interface MedicineRepository {

    Optional<Medicine> findByMedicineCode(String medicineCode);

    boolean existsByMedicineCode(String medicineCode);

    Optional<Medicine> findTopByOrderByMedicineCodeDesc();

    List<Medicine> findAllActive();
}
