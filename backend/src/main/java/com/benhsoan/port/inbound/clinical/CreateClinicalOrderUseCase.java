package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;

public interface CreateClinicalOrderUseCase {

    ClinicalOrderResult createOrder(UUID examinationId, CreateClinicalOrderCommand command);
}
