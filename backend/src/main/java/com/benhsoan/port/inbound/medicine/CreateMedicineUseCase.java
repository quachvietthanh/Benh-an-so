package com.benhsoan.port.inbound.medicine;

import com.benhsoan.port.dto.command.medicine.CreateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;

public interface CreateMedicineUseCase {

    MedicineResult create(CreateMedicineCommand command);
}
