package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ReportingRestMapper;
import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.inbound.reporting.ExportOperationalReportUseCase;
import com.benhsoan.port.inbound.reporting.GetOperationalSummaryUseCase;
import com.benhsoan.port.inbound.reporting.GetTopMedicinesReportUseCase;
import com.benhsoan.port.inbound.reporting.GetOperationalTimelineUseCase;
import com.benhsoan.port.dto.result.TopMedicineItemResult;
import com.benhsoan.port.dto.result.TopMedicinesReportResult;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ReportsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ReportingRestMapper.class)
class ReportsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetOperationalSummaryUseCase getOperationalSummaryUseCase;
    @MockitoBean private GetOperationalTimelineUseCase getOperationalTimelineUseCase;
    @MockitoBean private GetTopMedicinesReportUseCase getTopMedicinesReportUseCase;
    @MockitoBean private ExportOperationalReportUseCase exportOperationalReportUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void returnsSummary() throws Exception {
        when(getOperationalSummaryUseCase.getSummary(any(), any())).thenReturn(new OperationalSummaryResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                12L,
                new BigDecimal("5400000"),
                "VND"
        ));

        mockMvc.perform(get("/reports/summary")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitCount").value(12))
                .andExpect(jsonPath("$.revenue").value(5400000))
                .andExpect(jsonPath("$.currency").value("VND"));
    }

    @Test
    void returnsEmptySummaryWhenNoDataExists() throws Exception {
        when(getOperationalSummaryUseCase.getSummary(any(), any())).thenReturn(new OperationalSummaryResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                0L,
                BigDecimal.ZERO,
                "VND"
        ));

        mockMvc.perform(get("/reports/summary")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitCount").value(0))
                .andExpect(jsonPath("$.revenue").value(0))
                .andExpect(jsonPath("$.currency").value("VND"));
    }

    @Test
    void rejectsMissingFromParameter() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("to", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from is required."));

        verifyNoInteractions(getOperationalSummaryUseCase);
    }

    @Test
    void rejectsBlankFromParameter() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("from", " ")
                        .param("to", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from is required."));

        verifyNoInteractions(getOperationalSummaryUseCase);
    }

    @Test
    void rejectsInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("from", "01-08-2026")
                        .param("to", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be in yyyy-MM-dd format."));

        verifyNoInteractions(getOperationalSummaryUseCase);
    }

    @Test
    void rejectsWhenFromIsAfterTo() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("from", "2026-08-04")
                        .param("to", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before or equal to to."));

        verifyNoInteractions(getOperationalSummaryUseCase);
    }

    @Test
    void returnsTimeline() throws Exception {
        when(getOperationalTimelineUseCase.getTimeline(any(), any())).thenReturn(new OperationalTimelineResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                List.of(
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 1), 2L, new BigDecimal("100000")),
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 2), 0L, BigDecimal.ZERO),
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 3), 1L, new BigDecimal("-20000"))
                )
        ));

        mockMvc.perform(get("/reports/visits-timeline")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].date").value("2026-08-02"))
                .andExpect(jsonPath("$.items[1].visitCount").value(0))
                .andExpect(jsonPath("$.items[2].revenue").value(-20000));
    }

    @Test
    void returnsTimelineWithZeroRowsWhenNoDataExists() throws Exception {
        when(getOperationalTimelineUseCase.getTimeline(any(), any())).thenReturn(new OperationalTimelineResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                List.of(
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 1), 0L, BigDecimal.ZERO),
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 2), 0L, BigDecimal.ZERO),
                        new OperationalTimelineItemResult(LocalDate.of(2026, 8, 3), 0L, BigDecimal.ZERO)
                )
        ));

        mockMvc.perform(get("/reports/visits-timeline")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].visitCount").value(0))
                .andExpect(jsonPath("$.items[1].revenue").value(0))
                .andExpect(jsonPath("$.items[2].visitCount").value(0));
    }

    @Test
    void returnsTopMedicines() throws Exception {
        when(getTopMedicinesReportUseCase.getTopMedicines(any(), any())).thenReturn(new TopMedicinesReportResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                Instant.parse("2026-08-03T08:00:00Z"),
                List.of(
                        new TopMedicineItemResult(
                                1,
                                java.util.UUID.fromString("16000000-0000-0000-0000-000000000001"),
                                "MED-PARA-500",
                                "Paracetamol 500 mg",
                                9L
                        )
                )
        ));

        mockMvc.perform(get("/reports/top-medicines")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").value("2026-08-03T08:00:00Z"))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].medicineCode").value("MED-PARA-500"))
                .andExpect(jsonPath("$.items[0].medicineName").value("Paracetamol 500 mg"))
                .andExpect(jsonPath("$.items[0].totalDispensedQuantity").value(9));
    }

    @Test
    void returnsEmptyTopMedicinesWhenNoDataExists() throws Exception {
        when(getTopMedicinesReportUseCase.getTopMedicines(any(), any())).thenReturn(new TopMedicinesReportResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                Instant.parse("2026-08-03T08:00:00Z"),
                List.of()
        ));

        mockMvc.perform(get("/reports/top-medicines")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-01"))
                .andExpect(jsonPath("$.to").value("2026-08-03"))
                .andExpect(jsonPath("$.generatedAt").value("2026-08-03T08:00:00Z"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void rejectsInvalidDateFormatForTopMedicines() throws Exception {
        mockMvc.perform(get("/reports/top-medicines")
                        .param("from", "01-08-2026")
                        .param("to", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be in yyyy-MM-dd format."));

        verifyNoInteractions(getTopMedicinesReportUseCase);
    }

    @Test
    void rejectsWhenFromIsAfterToForTopMedicines() throws Exception {
        mockMvc.perform(get("/reports/top-medicines")
                        .param("from", "2026-08-04")
                        .param("to", "2026-08-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before or equal to to."));

        verifyNoInteractions(getTopMedicinesReportUseCase);
    }

    @Test
    void rejectsWhenTopMedicinesDateRangeExceeds366Days() throws Exception {
        mockMvc.perform(get("/reports/top-medicines")
                        .param("from", "2025-01-01")
                        .param("to", "2026-01-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date range must not exceed 366 days."));

        verifyNoInteractions(getTopMedicinesReportUseCase);
    }

    @Test
    void exportsCsv() throws Exception {
        when(exportOperationalReportUseCase.export(any(), any())).thenReturn(new OperationalReportExportResult(
                "operational-report-2026-08-01-to-2026-08-03.csv",
                "text/csv; charset=UTF-8",
                "\uFEFFOPERATIONAL REPORT\nFrom,2026-08-01\nTo,2026-08-03\nVisit Count,2\nRevenue (VND),100000\n\nDate,Visit Count,Revenue (VND)\n2026-08-01,2,100000\n"
                        .getBytes(StandardCharsets.UTF_8)
        ));

        mockMvc.perform(get("/reports/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"operational-report-2026-08-01-to-2026-08-03.csv\""));
    }
}
