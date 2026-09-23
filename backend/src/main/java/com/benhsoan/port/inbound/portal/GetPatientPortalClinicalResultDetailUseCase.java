package com.benhsoan.port.inbound.portal;

import java.util.UUID;

import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultDetailResult;

public interface GetPatientPortalClinicalResultDetailUseCase {

    PatientPortalClinicalResultDetailResult getClinicalResultDetail(UUID clinicalResultId);
}
