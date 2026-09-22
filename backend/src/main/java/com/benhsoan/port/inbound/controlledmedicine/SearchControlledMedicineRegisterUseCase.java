package com.benhsoan.port.inbound.controlledmedicine;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.controlledmedicine.SearchControlledMedicineRegisterQuery;
import com.benhsoan.port.dto.result.ControlledMedicineRegisterResult;

public interface SearchControlledMedicineRegisterUseCase {

    Page<ControlledMedicineRegisterResult> search(SearchControlledMedicineRegisterQuery query);
}
