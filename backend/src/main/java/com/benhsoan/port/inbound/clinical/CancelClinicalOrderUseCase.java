package com.benhsoan.port.inbound.clinical;

import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderCommand;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderItemCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;

public interface CancelClinicalOrderUseCase {

    ClinicalOrderResult cancelOrder(CancelClinicalOrderCommand command);

    ClinicalOrderResult cancelOrderItem(CancelClinicalOrderItemCommand command);
}
