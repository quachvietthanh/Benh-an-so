package com.benhsoan.application.ucservice.inventory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockExportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockSummaryResult;
import com.benhsoan.port.inbound.inventory.ExportInventoryInOutStockReportUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryInOutStockReportUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExportInventoryInOutStockReportService implements ExportInventoryInOutStockReportUseCase {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(CLINIC_ZONE);

    private final GetInventoryInOutStockReportUseCase getInventoryInOutStockReportUseCase;
    private final InventoryReportAuthorizer authorizer;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public InventoryInOutStockExportResult export(GetInventoryInOutStockReportQuery query) {
        authorizer.requireReportAccess();
        InventoryInOutStockReportResult report = getInventoryInOutStockReportUseCase.getReport(query);

        Instant now = clockPort.now();
        String fileName = "bao-cao-xuat-nhap-ton-" + query.from() + "-den-" + query.to() + ".csv";
        String csvContent = buildCsv(report);

        UUID currentUserId = currentUserPort.getCurrentUserId();
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.EXPORT,
                ResourceType.OPERATIONAL_REPORT,
                null,
                "Exported inventory in-out stock report from " + query.from() + " to " + query.to(),
                null,
                now
        ));

        return new InventoryInOutStockExportResult(
                fileName,
                CSV_CONTENT_TYPE,
                csvContent.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String buildCsv(InventoryInOutStockReportResult report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // UTF-8 BOM for Microsoft Excel compatibility
        csv.append("BÁO CÁO XUẤT NHẬP TỒN KHO THUỐC\n");
        csv.append("Từ ngày,").append(report.from()).append('\n');
        csv.append("Đến ngày,").append(report.to()).append('\n');
        csv.append("Thời điểm xuất,").append(TIMESTAMP_FORMATTER.format(report.generatedAt())).append('\n');
        csv.append("Có giao dịch phát sinh,").append(report.hasTransactions() ? "Có" : "Không có giao dịch").append("\n\n");

        csv.append("STT,Mã thuốc,Tên thuốc,Đơn vị tính,Tồn đầu kỳ,Nhập trong kỳ,Xuất cấp phát,Trả lại,Điều chỉnh,Tồn cuối kỳ\n");

        int index = 1;
        for (InventoryInOutStockItemResult item : report.items()) {
            csv.append(index++).append(',')
                    .append(escapeCsv(item.medicineCode())).append(',')
                    .append(escapeCsv(item.medicineName())).append(',')
                    .append(escapeCsv(item.unit())).append(',')
                    .append(item.openingStock()).append(',')
                    .append(item.importQuantity()).append(',')
                    .append(item.dispensedQuantity()).append(',')
                    .append(item.returnedQuantity()).append(',')
                    .append(item.adjustedQuantity()).append(',')
                    .append(item.closingStock())
                    .append('\n');
        }

        InventoryInOutStockSummaryResult summary = report.summary();
        csv.append("Tổng cộng,,,,")
                .append(summary.totalOpeningStock()).append(',')
                .append(summary.totalImportQuantity()).append(',')
                .append(summary.totalDispensedQuantity()).append(',')
                .append(summary.totalReturnedQuantity()).append(',')
                .append(summary.totalAdjustedQuantity()).append(',')
                .append(summary.totalClosingStock())
                .append('\n');

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
