package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

public record PrescriptionPrintDocument(
        String clinicName,
        String clinicAddress,
        String clinicPhone,
        String prescriptionCode,
        UUID patientId,
        String patientCode,
        String patientName,
        UUID doctorId,
        String doctorName,
        Instant prescribedAt,
        List<Item> items
) {
    public PrescriptionPrintDocument {
        items = List.copyOf(items);
    }

    public record Item(
            String medicineName,
            String strength,
            String unit,
            String dosage,
            Integer frequency,
            Integer durationDays,
            AdministrationRoute route,
            int quantity,
            String instructions
    ) {
    }
}
