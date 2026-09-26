package com.benhsoan.port.inbound.billing;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.billing.PayableEncounterQuery;
import com.benhsoan.port.dto.result.PayableEncounterResult;

public interface GetPayableEncountersUseCase {

    Page<PayableEncounterResult> get(PayableEncounterQuery query);
}
