package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.ClinicalServiceManagementResult;

public interface GetClinicalServicesUseCase {

    Page<ClinicalServiceManagementResult> search(String keyword, Boolean active, Pageable pageable);

    ClinicalServiceManagementResult getById(UUID clinicalServiceId);
}
