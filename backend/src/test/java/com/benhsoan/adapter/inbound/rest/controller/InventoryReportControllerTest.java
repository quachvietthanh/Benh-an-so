package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryReportRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockExportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockSummaryResult;
import com.benhsoan.port.inbound.inventory.ExportInventoryInOutStockReportUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryInOutStockReportUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InventoryReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({InventoryReportRestMapper.class, GlobalExceptionHandler.class})
class InventoryReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInventoryInOutStockReportUseCase getInventoryInOutStockReportUseCase;

    @MockitoBean
    private ExportInventoryInOutStockReportUseCase exportInventoryInOutStockReportUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private ClockPort clockPort;

    @Test
    void getReport_returns200AndValidPayload() throws Exception {
        UUID medicineId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Instant now = Instant.parse("2026-09-21T10:00:00Z");

        InventoryInOutStockItemResult item = new InventoryInOutStockItemResult(
                medicineId,
                "MED-001",
                "Paracetamol 500mg",
                "Viên",
                50,
                100,
                30,
                10,
                -5,
                125
        );

        InventoryInOutStockSummaryResult summary = new InventoryInOutStockSummaryResult(
                1, 50, 100, 30, 10, -5, 125
        );

        InventoryInOutStockReportResult reportResult = new InventoryInOutStockReportResult(
                from,
                to,
                now,
                true,
                List.of(item),
                summary
        );

        when(getInventoryInOutStockReportUseCase.getReport(any(GetInventoryInOutStockReportQuery.class)))
                .thenReturn(reportResult);

        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .param("keyword", "Para"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-08-01"))
                .andExpect(jsonPath("$.to").value("2026-08-31"))
                .andExpect(jsonPath("$.hasTransactions").value(true))
                .andExpect(jsonPath("$.items[0].medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$.items[0].medicineCode").value("MED-001"))
                .andExpect(jsonPath("$.items[0].openingStock").value(50))
                .andExpect(jsonPath("$.items[0].importQuantity").value(100))
                .andExpect(jsonPath("$.items[0].dispensedQuantity").value(30))
                .andExpect(jsonPath("$.items[0].returnedQuantity").value(10))
                .andExpect(jsonPath("$.items[0].adjustedQuantity").value(-5))
                .andExpect(jsonPath("$.items[0].closingStock").value(125))
                .andExpect(jsonPath("$.summary.totalOpeningStock").value(50))
                .andExpect(jsonPath("$.summary.totalClosingStock").value(125));

        verify(getInventoryInOutStockReportUseCase).getReport(any(GetInventoryInOutStockReportQuery.class));
    }

    @Test
    void getReport_failsValidationWhenFromAfterTo() throws Exception {
        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-31")
                        .param("to", "2026-08-01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getInventoryInOutStockReportUseCase);
    }

    @Test
    void getReport_failsValidationWhenRangeExceeds366Days() throws Exception {
        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2025-01-01")
                        .param("to", "2026-01-10"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getInventoryInOutStockReportUseCase);
    }

    @Test
    void getReport_failsValidationWhenDateInvalidFormat() throws Exception {
        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "invalid-date")
                        .param("to", "2026-08-31"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getInventoryInOutStockReportUseCase);
    }

    @Test
    void export_returnsCsvAttachment() throws Exception {
        byte[] csvBytes = "\uFEFFHeader\n1,MED-001,50".getBytes(StandardCharsets.UTF_8);
        InventoryInOutStockExportResult exportResult = new InventoryInOutStockExportResult(
                "bao-cao-xuat-nhap-ton-2026-08-01-den-2026-08-31.csv",
                "text/csv; charset=UTF-8",
                csvBytes
        );

        when(exportInventoryInOutStockReportUseCase.export(any(GetInventoryInOutStockReportQuery.class)))
                .thenReturn(exportResult);

        mockMvc.perform(get("/inventory/reports/in-out-stock/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"bao-cao-xuat-nhap-ton-2026-08-01-den-2026-08-31.csv\""))
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(content().bytes(csvBytes));

        verify(exportInventoryInOutStockReportUseCase).export(any(GetInventoryInOutStockReportQuery.class));
    }

    @Test
    void getReport_returns404WhenMedicineNotFound() throws Exception {
        UUID medicineId = UUID.randomUUID();
        when(getInventoryInOutStockReportUseCase.getReport(any(GetInventoryInOutStockReportQuery.class)))
                .thenThrow(new com.benhsoan.domain.medicine.exception.MedicineNotFoundException(medicineId));

        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .param("medicineId", medicineId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDICINE_NOT_FOUND"));
    }
}
