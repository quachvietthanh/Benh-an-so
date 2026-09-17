package com.benhsoan.port.inbound.clinical;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.clinical.GetPendingClinicalOrdersQuery;
import com.benhsoan.port.dto.result.PendingClinicalOrderResult;

public interface GetPendingClinicalOrdersUseCase {

    Page<PendingClinicalOrderResult> getPendingOrders(GetPendingClinicalOrdersQuery query);
}
