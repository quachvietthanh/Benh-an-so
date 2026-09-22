package com.benhsoan.application.ucservice.inventory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.InventoryStockReportExportResult;
import com.benhsoan.port.dto.result.InventoryStockReportItemResult;
import com.benhsoan.port.dto.result.InventoryStockReportResult;
import com.benhsoan.port.inbound.inventory.ExportInventoryStockReportUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryStockReportUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportInventoryStockReportService implements ExportInventoryStockReportUseCase {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final GetInventoryStockReportUseCase getInventoryStockReportUseCase;

    @Override
    public InventoryStockReportExportResult export(LocalDate from, LocalDate to) {
        InventoryStockReportResult report = getInventoryStockReportUseCase.getStockReport(from, to);

        String fileName = "stock-in-out-report-" + from + "-to-" + to + ".csv";
        byte[] content = buildCsv(report).getBytes(StandardCharsets.UTF_8);

        return new InventoryStockReportExportResult(fileName, CSV_CONTENT_TYPE, content);
    }

    private String buildCsv(InventoryStockReportResult report) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // UTF-8 BOM so Excel opens the CSV correctly.
        csv.append("Medicine ID,Medicine Code,Medicine Name,Unit,")
                .append("Opening Quantity,Received Quantity,Dispensed Quantity,Returned Quantity,")
                .append("Adjusted Quantity,Closing Quantity\n");

        for (InventoryStockReportItemResult item : report.items()) {
            csv.append(csvCell(item.medicineId() == null ? "" : item.medicineId().toString())).append(',')
                    .append(csvCell(item.medicineCode())).append(',')
                    .append(csvCell(item.medicineName())).append(',')
                    .append(csvCell(item.unit())).append(',')
                    .append(item.openingQuantity()).append(',')
                    .append(item.receivedQuantity()).append(',')
                    .append(item.dispensedQuantity()).append(',')
                    .append(item.returnedQuantity()).append(',')
                    .append(item.adjustedQuantity()).append(',')
                    .append(item.closingQuantity()).append('\n');
        }

        return csv.toString();
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String neutralized = neutralizeFormulaInjection(value);
        if (neutralized.contains(",") || neutralized.contains("\"") || neutralized.contains("\n")) {
            return "\"" + neutralized.replace("\"", "\"\"") + "\"";
        }
        return neutralized;
    }

    /**
     * Neutralize spreadsheet formula injection (CSV injection, OWASP) for
     * medicine catalog text before it is written into a CSV cell.
     */
    private String neutralizeFormulaInjection(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') {
            return "'" + value;
        }
        return value;
    }
}
