package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionTemplateResult;

public interface GetPrescriptionTemplateUseCase {

    PrescriptionTemplateResult getById(UUID templateId);
}
