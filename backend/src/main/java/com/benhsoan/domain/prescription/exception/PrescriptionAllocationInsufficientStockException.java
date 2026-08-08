package com.benhsoan.domain.prescription.exception;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.PrescriptionItem;

public class PrescriptionAllocationInsufficientStockException extends PrescriptionInsufficientStockException {

    public PrescriptionAllocationInsufficientStockException(
            UUID prescriptionId,
            PrescriptionItem item,
            String medicineCode,
            int availableQuantity
    ) {
        super(
                prescriptionId,
                List.of(new StockShortageDetail(
                        item.getId(),
                        item.getMedicineId(),
                        medicineCode,
                        item.getMedicineName(),
                        item.getQuantity(),
                        Math.max(availableQuantity, 0),
                        Math.max(item.getQuantity() - Math.max(availableQuantity, 0), 1)
                ))
        );
    }
}
