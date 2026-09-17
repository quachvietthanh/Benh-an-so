package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorRevenueResult(
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
