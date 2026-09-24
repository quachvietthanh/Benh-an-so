package com.benhsoan.port.outbound.repository.controlledmedicine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.controlledmedicine.ControlledMedicineRegister;

/**
 * Read/write port for the special controlled medicine register
 * (NCL-06-CN-014). This port is append-only and read-only: it exposes only
 * {@code save}/{@code saveAll} (append) and {@code search}. There is
 * deliberately no update or delete method, so historical register records are
 * immutable from the application's perspective (TC-04).
 */
public interface ControlledMedicineRegisterRepository {

    ControlledMedicineRegister save(ControlledMedicineRegister record);

    List<ControlledMedicineRegister> saveAll(List<ControlledMedicineRegister> records);

    Page<ControlledMedicineRegister> search(
            ControlledMedicineRegisterSearchCriteria criteria,
            Pageable pageable
    );

    record ControlledMedicineRegisterSearchCriteria(
            UUID patientId,
            UUID medicineId,
            Instant fromInclusive,
            Instant toExclusive
    ) {
    }
}
