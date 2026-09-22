package com.benhsoan.domain.controlledmedicine;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Append-only record in the special controlled medicine register
 * ("sổ theo dõi riêng", NCL-06-CN-014 / QTN-39).
 *
 * <p>A record is created implicitly when a pharmacist dispenses a controlled
 * medicine and captures the prescribing doctor, dispensing pharmacist, patient,
 * medicine and quantity required by TC-03. It is immutable: there is no update
 * or delete path (TC-04).</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControlledMedicineRegister {

    private UUID id;
    private UUID prescriptionId;
    private UUID prescriptionItemId;
    private UUID medicineId;
    private String medicineName;
    private UUID patientId;
    private UUID prescribedBy;
    private UUID dispensedBy;
    private int quantity;
    private Instant dispensedAt;
    private Instant createdAt;

    private ControlledMedicineRegister(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID medicineId,
            String medicineName,
            UUID patientId,
            UUID prescribedBy,
            UUID dispensedBy,
            int quantity,
            Instant dispensedAt,
            Instant createdAt
    ) {
        this.id = requireNonNull(id, "Controlled medicine register id is required.");
        this.prescriptionId = requireNonNull(prescriptionId, "Prescription id is required.");
        this.prescriptionItemId = requireNonNull(prescriptionItemId, "Prescription item id is required.");
        this.medicineId = requireNonNull(medicineId, "Medicine id is required.");
        this.medicineName = requireText(medicineName, "Medicine name snapshot is required.");
        this.patientId = requireNonNull(patientId, "Patient id is required.");
        this.prescribedBy = requireNonNull(prescribedBy, "Prescribing doctor id is required.");
        this.dispensedBy = requireNonNull(dispensedBy, "Dispensing pharmacist id is required.");
        this.quantity = requirePositive(quantity, "Dispensed quantity must be greater than zero.");
        this.dispensedAt = requireNonNull(dispensedAt, "Dispensed at is required.");
        this.createdAt = requireNonNull(createdAt, "Created at is required.");
    }

    public static ControlledMedicineRegister create(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID medicineId,
            String medicineName,
            UUID patientId,
            UUID prescribedBy,
            UUID dispensedBy,
            int quantity,
            Instant dispensedAt
    ) {
        return new ControlledMedicineRegister(
                id,
                prescriptionId,
                prescriptionItemId,
                medicineId,
                medicineName,
                patientId,
                prescribedBy,
                dispensedBy,
                quantity,
                dispensedAt,
                dispensedAt
        );
    }

    public static ControlledMedicineRegister restore(
            UUID id,
            UUID prescriptionId,
            UUID prescriptionItemId,
            UUID medicineId,
            String medicineName,
            UUID patientId,
            UUID prescribedBy,
            UUID dispensedBy,
            int quantity,
            Instant dispensedAt,
            Instant createdAt
    ) {
        return new ControlledMedicineRegister(
                id,
                prescriptionId,
                prescriptionItemId,
                medicineId,
                medicineName,
                patientId,
                prescribedBy,
                dispensedBy,
                quantity,
                dispensedAt,
                createdAt
        );
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new ValidationException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
