package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueBreakdownReportResult(
        LocalDate from,
        LocalDate to,
        BigDecimal totalNetRevenue,
        BigDecimal totalExamRevenue,
        BigDecimal totalClinicalServiceRevenue,
        BigDecimal totalMedicationRevenue,
        BigDecimal totalAdjustmentRevenue,
        String currency,
        List<ServiceGroupRevenueResult> serviceGroups,
        List<DoctorRevenueResult> doctors
) {
}
