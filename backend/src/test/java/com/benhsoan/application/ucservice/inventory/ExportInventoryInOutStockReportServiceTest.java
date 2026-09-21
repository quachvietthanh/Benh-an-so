package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockExportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockSummaryResult;
import com.benhsoan.port.inbound.inventory.GetInventoryInOutStockReportUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ExportInventoryInOutStockReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    private final GetInventoryInOutStockReportUseCase getInventoryInOutStockReportUseCase =
            mock(GetInventoryInOutStockReportUseCase.class);
    private final InventoryReportAuthorizer authorizer = mock(InventoryReportAuthorizer.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private ExportInventoryInOutStockReportService exportService;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(USER_ID);

        exportService = new ExportInventoryInOutStockReportService(
                getInventoryInOutStockReportUseCase,
                authorizer,
                auditLogRepository,
                currentUserPort,
                clockPort
        );
    }

    @Test
    void successfullyExportsInOutStockReportAsCsv() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(from, to, null, null);

        InventoryInOutStockItemResult item = new InventoryInOutStockItemResult(
                UUID.randomUUID(),
                "MED-001",
                "Paracetamol, 500mg",
                "Viên",
                50,
                100,
                30,
                5,
                -2,
                123
        );

        InventoryInOutStockSummaryResult summary = new InventoryInOutStockSummaryResult(
                1, 50, 100, 30, 5, -2, 123
        );

        InventoryInOutStockReportResult reportResult = new InventoryInOutStockReportResult(
                from,
                to,
                NOW,
                true,
                List.of(item),
                summary
        );

        when(getInventoryInOutStockReportUseCase.getReport(query)).thenReturn(reportResult);

        InventoryInOutStockExportResult exportResult = exportService.export(query);

        assertNotNull(exportResult);
        assertEquals("bao-cao-xuat-nhap-ton-2026-08-01-den-2026-08-31.csv", exportResult.fileName());
        assertEquals("text/csv; charset=UTF-8", exportResult.contentType());

        String csv = new String(exportResult.content(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("BÁO CÁO XUẤT NHẬP TỒN KHO THUỐC"));
        assertTrue(csv.contains("Từ ngày,2026-08-01"));
        assertTrue(csv.contains("Đến ngày,2026-08-31"));
        assertTrue(csv.contains("\"Paracetamol, 500mg\"")); // escaped
        assertTrue(csv.contains("Tổng cộng,,,,50,100,30,5,-2,123"));

        verify(auditLogRepository).save(argThat(audit ->
                audit.getActionType() == ActionType.EXPORT
                        && audit.getResourceType() == ResourceType.OPERATIONAL_REPORT
                        && audit.getUserId().equals(USER_ID)
        ));
        verify(authorizer).requireReportAccess();
    }

    @Test
    void throwsWhenAuthorizerDeniesAccess() {
        doThrow(new AccessDeniedException("Forbidden")).when(authorizer).requireReportAccess();

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                null,
                null
        );

        assertThrows(AccessDeniedException.class, () -> exportService.export(query));
        verify(authorizer).requireReportAccess();
    }
}
