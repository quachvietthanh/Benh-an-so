package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorRevenueResponse(
        UUID doctorId,
        String doctorCode,
        String doctorName,
        BigDecimal examRevenue,
        BigDecimal clinicalServiceRevenue,
        BigDecimal medicationRevenue,
        BigDecimal adjustmentRevenue,
        BigDecimal totalRevenue,
        BigDecimal percentage
) {
}
