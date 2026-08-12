package com.benhsoan.port.inbound.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.PayableEncounterResult;

public interface GetPayableEncountersUseCase {

    Page<PayableEncounterResult> get(Pageable pageable);
}
