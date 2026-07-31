package com.benhsoan.port.inbound.clinical;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.clinical.GetClinicalResultsByVisitQuery;
import com.benhsoan.port.dto.result.ClinicalResultResult;

public interface GetClinicalResultsByVisitUseCase {

    Page<ClinicalResultResult> getResultsByVisit(GetClinicalResultsByVisitQuery query);
}
