package com.benhsoan.domain.inventory.procurement.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class ProcurementPlanEmptyItemsException extends DomainException {

    public ProcurementPlanEmptyItemsException() {
        super(DomainErrorCode.PROCUREMENT_PLAN_EMPTY_ITEMS, "Phiếu dự trù mua thuốc phải có ít nhất một loại thuốc.");
    }
}
