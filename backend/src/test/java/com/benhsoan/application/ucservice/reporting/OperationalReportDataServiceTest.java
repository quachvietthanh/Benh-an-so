package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.outbound.repository.reporting.DailyRevenueSummary;
import com.benhsoan.port.outbound.repository.reporting.DailyVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.DoctorVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.TopMedicineSummary;
import com.benhsoan.domain.reporting.enums.ReportType;

class OperationalReportDataServiceTest {

    @Test
    void checksSourceDataAccordingToReportType() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.hasCompletedVisits(any(), any())).thenReturn(true);
        when(repository.hasInvoices(any(), any())).thenReturn(false);
        OperationalReportDataService service = new OperationalReportDataService(repository);
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);

        assertTrue(service.hasReportData(ReportType.VISIT_REPORT, from, to));
        assertFalse(service.hasReportData(ReportType.REVENUE_REPORT, from, to));
        assertTrue(service.hasReportData(ReportType.OPERATIONAL_REPORT, from, to));
    }

    @Test
    void treatsInvoicesAsOperationalSourceDataWhenNoCompletedVisitExists() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.hasCompletedVisits(any(), any())).thenReturn(false);
        when(repository.hasInvoices(any(), any())).thenReturn(true);
        OperationalReportDataService service = new OperationalReportDataService(repository);

        assertTrue(service.hasReportData(
                ReportType.OPERATIONAL_REPORT, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)));
    }

    @Test
    void buildsSummaryAndTimelineFromSharedReportingDataSource() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.countCompletedVisits(any(), any())).thenReturn(3L);
        when(repository.sumNetRevenue(any(), any())).thenReturn(new BigDecimal("80000"));
        when(repository.findDailyCompletedVisits(any(), any())).thenReturn(List.of(
                new DailyVisitSummary(LocalDate.of(2026, 8, 1), 2L),
                new DailyVisitSummary(LocalDate.of(2026, 8, 3), 1L)
        ));
        when(repository.findDailyNetRevenue(any(), any())).thenReturn(List.of(
                new DailyRevenueSummary(LocalDate.of(2026, 8, 1), new BigDecimal("100000")),
                new DailyRevenueSummary(LocalDate.of(2026, 8, 3), new BigDecimal("-20000"))
        ));

        OperationalReportDataService service = new OperationalReportDataService(repository);
        OperationalReportData reportData = service.getReportData(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(3L, reportData.summary().visitCount());
        assertEquals(new BigDecimal("80000"), reportData.summary().revenue());
        assertEquals("VND", reportData.summary().currency());
        assertEquals(3, reportData.timeline().items().size());
        assertEquals(0L, reportData.timeline().items().get(1).visitCount());
        assertEquals(BigDecimal.ZERO, reportData.timeline().items().get(1).revenue());
        assertEquals(new BigDecimal("-20000"), reportData.timeline().items().get(2).revenue());
    }

    @Test
    void returnsTopMedicinesGroupedByMedicineIdUsingDispensedAtRange() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.findTopDispensedMedicines(any(), any())).thenReturn(List.of(
                new TopMedicineSummary(
                        java.util.UUID.fromString("16000000-0000-0000-0000-000000000003"),
                        "MED-IBU-400",
                        "Ibuprofen 400 mg",
                        15L
                ),
                new TopMedicineSummary(
                        java.util.UUID.fromString("16000000-0000-0000-0000-000000000001"),
                        "MED-PARA-500",
                        "Paracetamol 500 mg",
                        9L
                )
        ));

        OperationalReportDataService service = new OperationalReportDataService(repository);
        var result = service.getTopMedicines(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

        assertEquals(LocalDate.of(2026, 8, 1), result.from());
        assertEquals(LocalDate.of(2026, 8, 3), result.to());
        assertNull(result.generatedAt());
        assertEquals(2, result.items().size());
        assertEquals(1, result.items().get(0).rank());
        assertEquals("MED-IBU-400", result.items().get(0).medicineCode());
        assertEquals(15L, result.items().get(0).totalDispensedQuantity());
        assertEquals(2, result.items().get(1).rank());
        assertEquals("MED-PARA-500", result.items().get(1).medicineCode());
    }

    @Test
    void returnsDoctorVisitsGroupedByDoctorWithSequentialRank() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.findDoctorVisitSummaries(any(), any())).thenReturn(List.of(
                new DoctorVisitSummary(
                        java.util.UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"),
                        "doctor2",
                        "Dr. Tran Quang Huy",
                        18L
                ),
                new DoctorVisitSummary(
                        java.util.UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"),
                        "doctor1",
                        "Dr. Nguyen Minh Anh",
                        9L
                )
        ));

        OperationalReportDataService service = new OperationalReportDataService(repository);
        var result = service.getDoctorVisits(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14));

        assertEquals(LocalDate.of(2026, 8, 1), result.from());
        assertEquals(LocalDate.of(2026, 8, 14), result.to());
        assertNull(result.generatedAt());
        assertEquals(2, result.items().size());
        assertEquals(1, result.items().get(0).rank());
        assertEquals("doctor2", result.items().get(0).doctorCode());
        assertEquals(18L, result.items().get(0).totalVisits());
        assertEquals(2, result.items().get(1).rank());
        assertEquals("doctor1", result.items().get(1).doctorCode());
        assertEquals(9L, result.items().get(1).totalVisits());
    }

    @Test
    void returnsEmptyDoctorVisitsWhenNoCompletedVisitsExist() {
        OperationalReportQueryRepository repository = mock(OperationalReportQueryRepository.class);
        when(repository.findDoctorVisitSummaries(any(), any())).thenReturn(List.of());

        OperationalReportDataService service = new OperationalReportDataService(repository);
        var result = service.getDoctorVisits(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14));

        assertEquals(0, result.items().size());
    }
}
