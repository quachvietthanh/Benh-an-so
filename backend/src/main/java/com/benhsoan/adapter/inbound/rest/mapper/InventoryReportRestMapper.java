package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryInOutStockItemResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryInOutStockReportResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryInOutStockSummaryResponse;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockSummaryResult;

@Component
public class InventoryReportRestMapper {

    public InventoryInOutStockReportResponse toResponse(InventoryInOutStockReportResult result) {
        if (result == null) {
            return null;
        }

        return new InventoryInOutStockReportResponse(
                result.from(),
                result.to(),
                result.generatedAt(),
                result.hasTransactions(),
                result.items().stream().map(this::toItemResponse).toList(),
                toSummaryResponse(result.summary())
        );
    }

    public InventoryInOutStockItemResponse toItemResponse(InventoryInOutStockItemResult item) {
        if (item == null) {
            return null;
        }

        return new InventoryInOutStockItemResponse(
                item.medicineId(),
                item.medicineCode(),
                item.medicineName(),
                item.unit(),
                item.openingStock(),
                item.importQuantity(),
                item.dispensedQuantity(),
                item.returnedQuantity(),
                item.adjustedQuantity(),
                item.closingStock()
        );
    }

    public InventoryInOutStockSummaryResponse toSummaryResponse(InventoryInOutStockSummaryResult summary) {
        if (summary == null) {
            return null;
        }

        return new InventoryInOutStockSummaryResponse(
                summary.totalMedicines(),
                summary.totalOpeningStock(),
                summary.totalImportQuantity(),
                summary.totalDispensedQuantity(),
                summary.totalReturnedQuantity(),
                summary.totalAdjustedQuantity(),
                summary.totalClosingStock()
        );
    }
}
