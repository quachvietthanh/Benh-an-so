package com.benhsoan.port.inbound.prescription;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.prescription.SearchPrescriptionsQuery;
import com.benhsoan.port.dto.result.PrescriptionResult;

public interface SearchPrescriptionsUseCase {

    Page<PrescriptionResult> search(SearchPrescriptionsQuery query);
}
