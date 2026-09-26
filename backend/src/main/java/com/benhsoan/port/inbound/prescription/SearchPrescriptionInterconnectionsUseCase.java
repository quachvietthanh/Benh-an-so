package com.benhsoan.port.inbound.prescription;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.prescription.SearchPrescriptionInterconnectionsQuery;
import com.benhsoan.port.dto.result.PrescriptionInterconnectionListItemResult;

public interface SearchPrescriptionInterconnectionsUseCase {

    Page<PrescriptionInterconnectionListItemResult> search(SearchPrescriptionInterconnectionsQuery query);
}
