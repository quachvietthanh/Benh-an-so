package com.benhsoan.port.inbound.prescription;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;
import com.benhsoan.port.dto.result.PrescriptionAllergyWarningLogResult;

public interface GetPrescriptionAllergyWarningLogsUseCase {

    Page<PrescriptionAllergyWarningLogResult> search(SearchPrescriptionAllergyWarningLogsQuery query);
}
