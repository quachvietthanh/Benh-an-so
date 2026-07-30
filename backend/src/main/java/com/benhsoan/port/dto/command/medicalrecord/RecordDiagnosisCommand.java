package com.benhsoan.port.dto.command.medicalrecord;

import java.util.List;
import java.util.UUID;

public record RecordDiagnosisCommand(
        UUID diagnosisCatalogId,
        String primaryIcdCode,
        String primaryIcdName,
        List<SecondaryIcd> secondaryIcdCodes,
        String clinicalNotes
) {
    public record SecondaryIcd(String code, String name) {}
}
