package com.benhsoan.domain.inventory.procurement.exception;

import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class ProcurementPlanInvalidStatusException extends DomainException {

    public ProcurementPlanInvalidStatusException(String message) {
        super(DomainErrorCode.PROCUREMENT_PLAN_INVALID_STATUS, message);
    }

    public ProcurementPlanInvalidStatusException(ProcurementPlanStatus currentStatus, String action) {
        super(DomainErrorCode.PROCUREMENT_PLAN_INVALID_STATUS,
                "Không thể thực hiện hành động '" + action + "' khi phiếu dự trù đang ở trạng thái: " + currentStatus);
    }
}
