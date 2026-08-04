package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

import lombok.Builder;

@Builder
public record CreatePrescriptionItemCommand(

        UUID medicineId,

        String dosage,

        String frequency,

        AdministrationRoute route,

        Integer durationDays,

        int quantity,

        String instructions

) {
}
