package com.benhsoan.domain.prescription.exception;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;

public class PrescriptionInsufficientStockException extends PrescriptionException {

    private final UUID prescriptionId;
    private final List<StockShortageDetail> details;

    public PrescriptionInsufficientStockException(
            UUID prescriptionId,
            List<StockShortageDetail> details
    ) {
        super(HttpStatus.CONFLICT, "Insufficient stock for one or more medicines.");
        this.prescriptionId = Objects.requireNonNull(prescriptionId, "Prescription id is required.");
        this.details = List.copyOf(Objects.requireNonNull(details, "Stock shortage details are required."));
    }

    public UUID getPrescriptionId() {
        return prescriptionId;
    }

    public List<StockShortageDetail> getDetails() {
        return details;
    }

    public record StockShortageDetail(
            UUID prescriptionItemId,
            UUID medicineId,
            String medicineCode,
            String medicineName,
            int requiredQuantity,
            int availableQuantity,
            int shortageQuantity
    ) {
        public StockShortageDetail {
            if (prescriptionItemId == null) {
                throw new IllegalArgumentException("Prescription item id is required.");
            }
            if (medicineId == null) {
                throw new IllegalArgumentException("Medicine id is required.");
            }
            if (medicineCode == null || medicineCode.isBlank()) {
                throw new IllegalArgumentException("Medicine code is required.");
            }
            if (medicineName == null || medicineName.isBlank()) {
                throw new IllegalArgumentException("Medicine name is required.");
            }
            if (requiredQuantity <= 0) {
                throw new IllegalArgumentException("Required quantity must be greater than 0.");
            }
            if (availableQuantity < 0) {
                throw new IllegalArgumentException("Available quantity must be non-negative.");
            }
            if (shortageQuantity <= 0) {
                throw new IllegalArgumentException("Shortage quantity must be greater than 0.");
            }
        }
    }
}
