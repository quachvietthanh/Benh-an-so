package com.benhsoan.domain.inventory.procurement.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class ProcurementPlanDuplicateMedicineException extends DomainException {

    public ProcurementPlanDuplicateMedicineException(UUID medicineId) {
        super(DomainErrorCode.PROCUREMENT_PLAN_DUPLICATE_MEDICINE,
                "Thuốc có mã ID " + medicineId + " đã xuất hiện nhiều lần trong phiếu dự trù.");
    }
}
