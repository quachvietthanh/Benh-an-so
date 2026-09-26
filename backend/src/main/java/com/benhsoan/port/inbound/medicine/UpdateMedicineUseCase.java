package com.benhsoan.port.inbound.medicine;

import com.benhsoan.port.dto.command.medicine.UpdateMedicineCommand;
import com.benhsoan.port.dto.result.MedicineResult;

public interface UpdateMedicineUseCase {

    MedicineResult update(UpdateMedicineCommand command);
}
