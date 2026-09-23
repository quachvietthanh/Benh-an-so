package com.benhsoan.port.inbound.prescription;

import java.util.List;

import com.benhsoan.port.dto.result.PrescriptionTemplateResult;

public interface GetPrescriptionTemplatesUseCase {

    List<PrescriptionTemplateResult> getByDiagnosisCode(String diagnosisCode);
}
