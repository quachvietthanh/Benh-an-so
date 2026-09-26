package com.benhsoan.port.inbound.clinical;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.clinical.GetClinicalOrdersByVisitQuery;
import com.benhsoan.port.dto.result.ClinicalOrderResult;

public interface GetClinicalOrdersByVisitUseCase {

    Page<ClinicalOrderResult> getOrdersByVisit(GetClinicalOrdersByVisitQuery query);
}
