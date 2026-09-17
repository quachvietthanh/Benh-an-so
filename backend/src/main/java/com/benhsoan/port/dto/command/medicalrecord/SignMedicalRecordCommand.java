package com.benhsoan.port.dto.command.medicalrecord;

public record SignMedicalRecordCommand(
        String signatureData,
        Boolean acknowledgePendingOrders
) {
    public SignMedicalRecordCommand(String signatureData) {
        this(signatureData, false);
    }
}
