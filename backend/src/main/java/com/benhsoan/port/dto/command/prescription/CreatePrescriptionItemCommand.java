package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

import lombok.Builder;

@Builder
public record CreatePrescriptionItemCommand(

        UUID medicineId,

        String dosage,

        Integer frequency,

        AdministrationRoute route,

        Integer durationDays,

        int quantity,

        String instructions

) {
}
