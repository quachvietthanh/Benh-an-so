package com.benhsoan.port.inbound.medicine;

import java.util.UUID;

import com.benhsoan.port.dto.result.MedicineResult;

public interface DeactivateMedicineUseCase {

    MedicineResult deactivate(UUID medicineId);
}
