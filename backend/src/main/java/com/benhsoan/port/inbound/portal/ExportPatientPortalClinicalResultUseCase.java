package com.benhsoan.port.inbound.portal;

import java.util.UUID;

import com.benhsoan.port.dto.result.portal.ClinicalResultPrintResult;

public interface ExportPatientPortalClinicalResultUseCase {

    ClinicalResultPrintResult exportByResult(UUID clinicalResultId);

    ClinicalResultPrintResult exportByVisit(UUID visitId);
}
