package com.benhsoan.port.inbound.portal;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultSummaryResult;

public interface GetPatientPortalClinicalResultsUseCase {

    List<PatientPortalClinicalResultSummaryResult> getClinicalResults(UUID visitId);
}
