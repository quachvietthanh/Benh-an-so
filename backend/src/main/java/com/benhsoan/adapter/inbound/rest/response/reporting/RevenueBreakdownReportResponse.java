package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RevenueBreakdownReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalNetRevenue,
        BigDecimal totalExamRevenue,
        BigDecimal totalClinicalServiceRevenue,
        BigDecimal totalMedicationRevenue,
        BigDecimal totalAdjustmentRevenue,
        String currency,
        List<ServiceGroupRevenueResponse> serviceGroups,
        List<DoctorRevenueResponse> doctors
) {
}
