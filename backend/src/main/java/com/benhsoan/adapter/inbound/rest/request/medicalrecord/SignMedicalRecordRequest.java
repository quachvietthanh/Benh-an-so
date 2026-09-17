package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

public record SignMedicalRecordRequest(
        String signatureData,
        Boolean acknowledgePendingOrders
) {
    public SignMedicalRecordRequest(String signatureData) {
        this(signatureData, false);
    }
}
