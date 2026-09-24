package com.benhsoan.domain.inventory.procurement.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class ProcurementPlanNotFoundException extends DomainException {

    public ProcurementPlanNotFoundException(UUID id) {
        super(DomainErrorCode.PROCUREMENT_PLAN_NOT_FOUND, "Không tìm thấy phiếu dự trù mua thuốc: " + id);
    }

    public ProcurementPlanNotFoundException(String planCode) {
        super(DomainErrorCode.PROCUREMENT_PLAN_NOT_FOUND, "Không tìm thấy phiếu dự trù mua thuốc có mã: " + planCode);
    }
}
