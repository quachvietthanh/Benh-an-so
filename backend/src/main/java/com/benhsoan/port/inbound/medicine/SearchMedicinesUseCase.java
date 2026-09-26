package com.benhsoan.port.inbound.medicine;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.medicine.SearchMedicinesQuery;
import com.benhsoan.port.dto.result.MedicineResult;

public interface SearchMedicinesUseCase {

    Page<MedicineResult> search(SearchMedicinesQuery query);
}
